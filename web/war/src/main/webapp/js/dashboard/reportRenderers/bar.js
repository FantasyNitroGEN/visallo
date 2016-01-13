define([
    'flight/lib/component',
    'util/vertex/formatters',
    'util/requirejs/promise!util/service/ontologyPromise',
    './withRenderer'
], function(
    defineComponent,
    F,
    ontology,
    withRenderer) {
    'use strict';

    var MAX_ROWS = 10,
        BAR_OTHER_CATEGORY = 'BAR_OTHER_CATEGORY',
        d3,
        nameAsFloat = _.compose(floatFn, nameFn),
        nameAsDate = function(d) {
            return new Date(parseInt(d.name, 10))
        };

    return defineComponent(StackedBar, withRenderer);

    function countFn(d) {
        return d.value.count;
    }

    function nameFn(d) {
        return d.name;
    }

    function floatFn(d) {
        return parseFloat(d, 10);
    }

    function isOtherCategory(d) {
        return nameFn(d) === BAR_OTHER_CATEGORY;
    }

    function tickFormatInterval(interval) {
        return function(d) {
            if (_.isDate(d)) {
                var months = 1000 * 60 * 60 * 24 * 30,
                    years = months * 12;
                if (interval > years) {
                    return d.getFullYear();
                }
                return F.date.dateString(d);
            }

            return d;
        }
    }

    function tickFormat(d) {
        if (_.isDate(d)) {
            return F.date.dateString(d);
        } else if (_.isNumber(d)) {
            if (d >= 1000) {
                return F.number.prettyApproximate(d);
            }
            return d3.format('d')(d);
        }
        return d;
    }

    function StackedBar() {

        this.processData = function(data) {
            var self = this,
                root = data.root[0],
                report = this.attr.report;

            this.isHistogram = root.type === 'histogram';
            this.defsIdIncrement = 0;

            var buckets = _.sortBy(root.buckets, function(d) {
                    if ('count' in d.value) {
                        return self.isHistogram ? nameFn(d) : countFn(d);
                    }
                }),
                bucket = _.first(root.buckets);

            this.isNested = Boolean(bucket && bucket.value.nested && bucket.value.nested.length);
            this.isHorizontal = this.attr.item.configuration.reportRenderer === 'org-visallo-bar-horizontal';

            var reportAggregations = report.endpointParameters.aggregations.map(JSON.parse);
            this.field = reportAggregations[0].field;
            if (this.isNested) {
                this.nestedField = reportAggregations[0].nested[0].field;
                this.nestedCategories = _.unique(_.flatten(root.buckets.map(function(d) {
                    return _.pluck(d.value.nested, 'name');
                }))).sort().concat([BAR_OTHER_CATEGORY]);
            }

            if (this.isHistogram) {
                this.interval = root.interval;
                return _.sortBy(buckets, nameAsFloat);
            }

            return buckets.reverse();
        };

        this.render = function renderStackedBar(_d3, node, data, d3tip) {
            d3 = _d3;

            var $node = $(node),
                colorScale = this.colorScale,
                colors = this.colors;

            if (!data || !data.length) {
                $node.empty();
                return;
            }

            if (this.isNested) {
                colorScale = d3.scale.category20();
                colorScale.domain(this.nestedCategories);
                colors = colorScale.range();
            }

            var self = this,
                padding = 10,
                width = $node.width(),
                height = $node.height() - padding,
                heightXAxis = 20,
                xAxisVerticalOffset = (Math.trunc(height - heightXAxis) + 0.5),
                yAxisWidth = 30,
                report = this.attr.report,
                numBars = this.isHistogram ? data.length : this.isHorizontal ? height / heightXAxis : MAX_ROWS,
                dataSlice = data.slice(0, numBars),
                ontologyProperty = ontology.properties.byTitle[this.field],
                isDates = ontologyProperty && ontologyProperty.dataType === 'date',
                yScale = d3.scale.linear()
                            .domain([0, d3.extent(dataSlice, countFn)[1]])
                            .range([height - heightXAxis, 0]),
                xData = this.isHistogram && _.map(dataSlice, isDates ? nameAsDate : nameAsFloat),
                xScale = this.isHistogram ?
                    isDates ?
                        d3.time.scale()
                            .domain([xData[0], new Date(_.last(xData).getTime() + self.interval)])
                            .range([0, width - yAxisWidth - 2 * this.CHART_PADDING]) :
                        d3.scale.linear()
                            .domain([d3.min(xData), d3.max(xData) + self.interval])
                            .range([0, width - yAxisWidth - 2 * this.CHART_PADDING]) :
                        d3.scale.ordinal()
                            .domain(_.map(dataSlice, self.displayName))
                            .rangeRoundBands([0, width - yAxisWidth - 2 * this.CHART_PADDING], 0.05),
                xColorScale = d3.scale.linear()
                    .range([0.3, 1.0])
                    .domain(yScale.domain()),
                countScale = yScale,
                dataScale = xScale;

            if (this.isHorizontal) {
                var tmpScale = xScale;
                xScale = yScale;
                yScale = tmpScale;
                xScale.range([0, width - 2 * this.CHART_PADDING]);
                if (this.isHistogram) {
                    yScale.range([height - heightXAxis, 0]);
                } else {
                    yScale.rangeRoundBands([0, height - heightXAxis], 0.05);
                }
            }

            var numTicks;
            if (!_.isFunction(xScale.rangeRoundBands)) {
                numTicks = Math.round(width / 50);
            }
            if (isDates && self.isHistogram) {
                numTicks = Math.round(width / 110);
            }

            var xAxis = d3.svg.axis()
                            .scale(xScale)
                            .orient('bottom');
            if (isDates) {
                xAxis.tickFormat(tickFormatInterval(self.interval));
            } else {
                xAxis.tickFormat(tickFormat);
            }
            if (numTicks > 0) {
                xAxis.ticks(numTicks);
            }

            var yAxis = d3.svg.axis()
                            .scale(yScale)
                            .orient('left')
                            .tickFormat(tickFormat),
                svg = d3.select(node).selectAll('svg').data([1]).call(function() {
                        var band = self.isHistogram ? dataScale(self.interval) : dataScale.rangeBand(),
                            clip = { x: band / -2, y: 0, width: band, height: 30 };
                        this.enter()
                            .append('svg')
                            .call(function() {
                                this.append('defs')
                                    .append('clipPath').attr('id', 'xAxisLabelClip')
                                    .append('rect')
                                    .attr(clip)
                                this.append('g')
                                    .attr('class', 'padding')
                                    .attr('transform', 'translate(0, ' + padding + ')');
                            })
                        this.attr('width', width)
                            .attr('height', height + padding);
                        this.attr('viewBox', null);
                        this.select('clipPath rect').transition().duration(self.TRANSITION_DURATION).attr(clip);
                    }).select('g.padding'),
                tip = this.tooltip = this.tooltip || this.createTooltip(svg, d3tip,
                    function(d) {
                        return d3.format(self.attr.tipFormat || ',')(countFn(d));
                    },
                    function(d) {
                        var label = isOtherCategory(d) ? i18n('dashboard.report.other') : self.displayName(d);
                        return self.isHistogram ?
                            isDates ?
                            (F.date.dateString(nameAsDate(d)) + '-' + F.date.dateString(new Date(nameAsDate(d).getTime() + self.interval))) :
                            (F.number.pretty(nameAsFloat(d)) + '-' + F.number.pretty(nameAsFloat(d) + self.interval)) :
                            (String(label).length > 20 ? label.substring(0, 20) + '...' : label)
                    }),
                gChartArea = svg.selectAll('g.chart-area').data([1]).call(function() {
                        var xOffset = (self.isHorizontal ? 0 : yAxisWidth) + self.CHART_PADDING;
                        this.enter().append('g')
                            .classed('chart-area', true)
                            .attr('transform', 'translate(' + xOffset + ',0)');
                    }),
                gXAxis = gChartArea.selectAll('g.x.axis').data([1]).call(function() {
                        this.enter().append('g')
                            .attr('class', 'x axis')
                            .attr('transform', 'translate(0,' + xAxisVerticalOffset + ')');
                        this.transition().duration(self.TRANSITION_DURATION)
                            .attr('transform', 'translate(0,' + xAxisVerticalOffset + ')')
                            .call(xAxis)
                            .call(function() {
                                if (!self.isHorizontal && !self.isHistogram) {
                                    this.selectAll('text')
                                        .attr('clip-path', self.isHistogram ? undefined : 'url(#xAxisLabelClip)')
                                        .each(function() {
                                            var band = self.isHistogram ? dataScale(self.interval) - 1 : dataScale.rangeBand(),
                                                box = this.getBoundingClientRect();
                                            if (box.width > band) {
                                                d3.select(this)
                                                    .attr('dx', band / -2)
                                                    .style('text-anchor', 'start')
                                            } else {
                                                d3.select(this)
                                                    .attr('dx', null)
                                            }
                                        })
                                        .each(function() {
                                            d3.select(this)
                                                .attr('dy', '.55em')
                                        })
                                        .call(overflowEllipsis, dataScale.rangeBand())
                                }
                            })
                    }),
                gYAxis = !self.isHorizontal && gChartArea.selectAll('g.y.axis').data([1]).call(function() {
                        this.enter().append('g').attr('class', 'y axis')
                        this.transition().duration(self.TRANSITION_DURATION).call(yAxis)
                    }),
                gBars = gChartArea.selectAll('g.bars').data([1]).call(function() {
                        this.enter().append('g').classed('bars', true);
                    });

            var bars = gBars.selectAll('.bar').data(dataSlice, nameFn);

            bars.enter()
                .append('g').attr('class', 'bar')
                .attr('transform', barTransformFn)
                .call(function() {
                    this.selectAll('rect.all')
                        .data(function(d) {
                            return [d];
                        })
                        .call(function() {
                            // TODO: Fix for horizontal
                            this.enter()
                                .call(function() {
                                    this.append('rect')
                                        .attr('class', 'all')
                                        .classed('clickable', function(d) {
                                            return !!d.field;
                                        })
                                        .attr('fill', 'transparent')
                                        .on('click', function(d, segIndex, barIndex) {
                                            if (!d.field) return;

                                            var filter = {
                                                propertyId: d.field,
                                                values: [nameFn(d)]
                                            };

                                            if (self.isHistogram) {
                                                filter.values.push(String(nameAsFloat(d) + self.interval));
                                                filter.predicate = 'range';
                                            } else {
                                                filter.predicate = 'equal';
                                            }
                                            self.handleClick({ filters: [filter] });
                                        });

                                    if (self.isHorizontal) {
                                        var clipPathId = 'clip-path-' + (self.defsIdIncrement++),
                                            clipPathId2 = 'clip-path2-' + (self.defsIdIncrement++);
                                        this.append('defs')
                                            .call(function() {
                                                this.append('clipPath')
                                                    .attr('id', clipPathId)
                                                    .append('rect').attr('class', 'clipRect')
                                                this.append('clipPath')
                                                    .attr('id', clipPathId2)
                                                    .append('rect').attr('class', 'clipRect2')
                                            })

                                        this.append('g').attr('class', 'text-label').style('opacity', 0)
                                            .call(function() {
                                                this.append('text')
                                                    .attr('class', 'text-1')
                                                    .attr('x', 5)
                                                    .style({ fill: 'white', opacity: 1 })
                                                    .attr('clip-path', 'url(#' + clipPathId + ')')
                                                this.append('text')
                                                    .attr('class', 'text-2')
                                                    .attr('x', 5)
                                                    .style({ fill: self.colors[0], opacity: 1 })
                                                    .attr('clip-path', 'url(#' + clipPathId2 + ')')
                                            })
                                            .transition()
                                            .delay(self.TRANSITION_DURATION - self.TRANSITION_DURATION / 4)
                                            .duration(self.TRANSITION_DURATION / 4)
                                            .style('opacity', 1)
                                    } else {
                                        this.append('g').attr('class', 'text-label')
                                    }
                                })

                        })
                })
                .selectAll('rect.seg')
                .data(function(d) {
                    var y0 = 0,
                        count = d.value.count,
                        nestedCount = 0;
                    return _.chain(self.isNested ? d.value.nested : [d])
                        .sortBy(function(d) {
                            return d.value.count * -1;
                        })
                        .map(function(d) {
                            nestedCount += d.value.count;
                            return _.extend({}, d, {
                                y0: y0,
                                y1: y0 += d.value.count || 0
                            })
                        })
                        .tap(function(buckets) {
                            if (self.isNested) {
                                var diff = count - nestedCount;
                                if (diff > 0) {
                                    buckets.push({
                                        name: BAR_OTHER_CATEGORY,
                                        field: self.nestedField,
                                        value: {
                                            count: diff
                                        },
                                        y0: y0,
                                        y1: y0 += diff || 0
                                    })
                                }
                            }
                        })
                        .value()
                })
                .call(function() {
                    this.enter().insert('rect', function() {
                        return this.querySelector('.text-label');
                    }).attr('class', 'seg')
                        .call(function() {
                            if (self.handleClick) {
                                this.on('click', function(d, segIndex, barIndex) {
                                    var barData = bars.data()[barIndex],
                                        thisFilter = {
                                            propertyId: d.field
                                        };

                                    if (isOtherCategory(d)) {
                                        thisFilter.predicate = 'hasNot';
                                    } else if (self.isHistogram) {
                                        thisFilter.values = [nameFn(d), String(nameAsFloat(d) + self.interval)];
                                        thisFilter.predicate = 'range';
                                    } else {
                                        thisFilter.values = [d.name];
                                        thisFilter.predicate = 'equal';
                                    }
                                    self.handleClick({
                                        filters: [
                                            thisFilter
                                        ].concat(self.isNested ?
                                            [{
                                                propertyId: barData.field,
                                                values: [barData.name],
                                                predicate: 'equal'
                                            }] : []
                                        )
                                    });
                                });
                                this.classed('clickable', true);
                            }
                        })
                        .attr('width', self.isHorizontal ? 0 : widthFn)
                        .attr('y', self.isHorizontal ? yFn : function(d) { return xAxisVerticalOffset; })
                        .attr('x', 0)
                        .attr('height', self.isHorizontal ? heightFn : 0)
                        .style('fill', function(d) { return self.isNested ? colorScale(nameFn(d)) : colors[0]; })
                        .on('mouseover', function(d) { tip.show(d, this); })
                        .on('mouseout', tip.hide)
                        .on('mousedown', tip.hide)

                    this.style('fill', function(d) { return self.isNested ? colorScale(nameFn(d)) : colors[0]; })

                    this.exit().remove();
                })

            bars.each(function() {
                d3.select(this)
                    .call(function() {
                        this.select('.all')
                            .attr('width', self.isHorizontal ? width : widthFn)
                            .attr('y', 0)
                            .attr('height', self.isHorizontal ? heightFn : height)
                        this.select('defs .clipRect')
                            .attr('y', yFn)
                            .attr('width', function(d) {
                                return countScale(countFn(d));
                            })
                            .attr('height', heightFn)
                        this.select('defs .clipRect2')
                            .attr('x', function(d) {
                                return countScale(countFn(d));
                            })
                            .attr('y', yFn)
                            .attr('width', function(d) {
                                var r = countScale.range();
                                return r[1] - r[0] - countScale(countFn(d));
                            })
                            .attr('height', heightFn)
                        this.select('.text-1').call(updateText)
                        this.select('.text-2').call(updateText)

                        function updateText() {
                            this.text(function(d) {
                                    return self.displayName(d);
                                })
                                .attr('dy', function(d) {
                                    return 16 / 4 + heightFn(d) / 2;
                                })
                        }
                    })
            })

            bars.transition().duration(self.TRANSITION_DURATION)
                .attr('transform', barTransformFn)
                .selectAll('rect.seg')
                .style('opacity', function(d) {
                    if (self.isNested) return;
                    return xColorScale(countFn(d));
                })
                .attr('width', widthFn)
                .attr('y', yFn)
                .attr('x', xFn)
                .attr('height', heightFn);
            bars.exit().remove();

            function widthFn(d) {
                if (self.isHorizontal) {
                    return countScale(d.y1) - countScale(d.y0);
                }

                if (self.isHistogram) {
                    if (isDates) {
                        var initialDate = dataScale.domain()[0].getTime();
                        return Math.max(1, dataScale(new Date(initialDate + self.interval)) - dataScale(initialDate));
                    } else {
                        return Math.max(1, dataScale(self.interval))
                    }
                } else {
                    return dataScale.rangeBand(d);
                }
            }

            function heightFn(d) {
                if (self.isHorizontal) {
                    if (self.isHistogram) {
                        if (isDates) {
                            var initialDate = dataScale.domain()[0].getTime();
                            return Math.max(1, dataScale(new Date(initialDate + self.interval)) - dataScale(initialDate));
                        } else {
                            return Math.max(1, dataScale(self.interval))
                        }
                    } else {
                        return dataScale.rangeBand(d);
                    }
                }
                return countScale(d.y0) - countScale(d.y1);
            }

            function xFn(d) {
                if (self.isHorizontal) {
                    return countScale(d.y0)
                }
                return 0;
            }

            function yFn(d) {
                if (self.isHorizontal) {
                    return dataScale(d.y1)
                }
                return countScale(d.y1);

            }

            function barTransformFn(d) {
                var offset = dataScale(self.isHistogram ? nameAsFloat(d) : self.displayName(d));
                if (self.isHorizontal) {
                    return 'translate(0,' + offset + ')';
                }
                return 'translate(' + offset + ',0)';
            }

            function overflowEllipsis(text, width) {
                text.each(function() {
                    var textEl = d3.select(this);
                    var textLength = textEl.node().getComputedTextLength();
                    var text = textEl.text();

                    while (textLength > width && text.length > 0) {
                        text = text.slice(0, -1);
                        textEl.text(text + '...');
                        textLength = textEl.node().getComputedTextLength();
                    }
                });
            }
        };
    }
});

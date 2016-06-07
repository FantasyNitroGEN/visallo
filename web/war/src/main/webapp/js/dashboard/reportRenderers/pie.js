define([
    'flight/lib/component',
    'util/formatters',
    'util/requirejs/promise!util/service/ontologyPromise',
    'text!./pieCss.css',
    './withRenderer'
], function(
    defineComponent,
    F,
    ontology,
    pieCss,
    withRenderer) {
    'use strict';

    var PIE_OTHER_CATEGORY = 'PIE_OTHER_CATEGORY',
        nameAsFloat = _.compose(floatFn, nameFn),
        nameAsDate = function(d) {
            return new Date(parseInt(nameFn(d), 10))
        };

    return defineComponent(Pie, withRenderer);

    function isOtherCategory(d) {
        return nameFn(d) === PIE_OTHER_CATEGORY;
    }

    function clickableFn(d) {
        return !isOtherCategory(d);
    }

    function countFn(d) {
        return (d.data || d).value.count;
    }

    function percentageFn(d) {
        return (d.data || d).value.percentage;
    }

    function labelFn(d) {
        return (d.data || d).label;
    }

    function nameFn(d) {
        return (d.data || d).name;
    }

    function floatFn(d) {
        return parseFloat(d, 10);
    }

    function Pie() {

        this.processData = function(data) {
            var self = this,
                report = this.attr.report,
                root = data.root[0],
                buckets = root.buckets,
                totalValue = _.reduce(buckets, function(memo, d) { return memo + countFn(d) }, 0),
                partitionedData = _.chain(buckets)
                    .map(function(b) {
                        var copy = _.extend({}, b);
                        copy.value = _.extend({}, copy.value, { percentage: countFn(b) / totalValue });
                        return copy;
                    })
                    .partition(function(d) {
                        return percentageFn(d) > 0.05;
                    })
                    .value();

            this.isHistogram = root.type === 'histogram';
            var reportAggregations = report.endpointParameters.aggregations.map(JSON.parse);
            this.field = reportAggregations[0].field;
            var ontologyProperty = ontology.properties.byTitle[this.field];
            this.isDates = ontologyProperty && ontologyProperty.dataType === 'date';

            var sortedData = this.isHistogram ?
                _.sortBy(partitionedData[0], nameAsFloat) :
                _.sortBy(partitionedData[0], countFn).reverse();

            if (partitionedData[1].length) {
                sortedData.push(_.tap({ value: {}, name: '' }, function(otherSlice) {
                    var otherValue = _.reduce(partitionedData[1], function(memo, d) { return memo + countFn(d); }, 0);
                    otherSlice.field = data.root[0].field;
                    otherSlice.value.percentage = otherValue / totalValue;
                    otherSlice.value.count = otherValue;
                    otherSlice.name = PIE_OTHER_CATEGORY;
                    otherSlice.displayName = i18n('dashboard.report.other');
                }));
            }

            return sortedData;
        };

        this.render = function renderPieChart(d3, node, data, d3tip) {
            var $node = $(node);
            if (!data || !data.length) {
                $node.empty();
                return;
            }

            var self = this,
                displayName = function(d) {
                    if (self.isHistogram) {
                        if (labelFn(d)) {
                            return labelFn(d);
                        } else if (isOtherCategory(d)) {
                            return d.data.displayName;
                        } else if (self.isDates) {
                            return [
                                F.date.dateString(nameAsDate(d)),
                                F.date.dateString(new Date(nameAsDate(d).getTime() + self.aggregation.interval))
                            ].join('-')
                        } else {
                            return [
                                F.number.pretty(parseFloat(nameFn(d))),
                                F.number.pretty(parseFloat(nameFn(d)) + self.aggregation.interval)
                            ].join('-')
                        }
                    }
                    return d.data.displayName || self.aggregation.displayName(d.data);
                },
                width = $node.width(),
                height = $node.height(),
                report = this.attr.report,
                radius = (Math.min(width, height) - this.CHART_PADDING) / 2,
                pie = d3.layout.pie().sort(null).value(countFn)(data),
                svg = d3.select(node).selectAll('svg').data([1]).call(function() {
                    this.enter().append('svg');
                    this.attr('width', width).attr('height', height);
                    this.append('defs').append('style').attr('type', 'text/css').text(pieCss);
                }),
                gPie = svg.selectAll('g').data([1]).call(function() {
                        this.enter().append('g')
                            .attr('transform', 'translate(' + radius + ',' + radius + ')');
                    }),
                slices = gPie.selectAll('path').data(pie),
                gLegend = svg.select('.legend').size() ? svg.select('.legend') : svg.append('g').classed('legend', true),
                gLabels = gLegend.selectAll('.legend-item').data(pie, displayName.bind(this)),
                tip = this.tooltip = this.tooltip || this.createTooltip(svg, d3tip, function(d) {
                        return d3.format('%')(percentageFn(d)) +
                            ' (' +
                            d3.format(self.attr.tipFormat || ',')(countFn(d)) +
                            ')';
                    },
                    function(d) {
                        var label = displayName(d),
                            labelLimit = 25;
                        return label;
                    }
                );

            gPie.transition()
                .duration(self.TRANSITION_DURATION)
                .attr('transform', 'translate(' + radius + ',' + radius + ')');

            slices.enter().append('path')
                    .attr('fill', function(d, i) { return self.colors[isOtherCategory(d) ? self.OTHER_COLOR_INDEX : i]; })
                    .each(function(d) {
                        this._current = { startAngle: 0, endAngle: 0, padAngle: 0, radius: radius };
                    })
                    .on('mousedown', tip.hide)
                    .on('mouseout', function(d, i) {
                        tip.hide();
                        d3.select(this).attr('fill', self.colors[isOtherCategory(d) ? self.OTHER_COLOR_INDEX : i]);
                    })
                    .on('mouseover', function(d, i) {
                        tip.show(d, gPie[0][0]);
                        d3.select(this).attr('fill', self.highlightColors[isOtherCategory(d) ? self.OTHER_COLOR_INDEX : i]);

                    })
                    .on('mousemove', function() {
                        var $tip = $('#' + tip.attr('id'));
                        tip.style({left: event.pageX - ($tip.width() / 1.5) + 'px', top: event.pageY - ($tip.height() * 1.75) + 'px'});
                    });

            slices.transition().duration(self.TRANSITION_DURATION)
                    .attrTween('d', function(a) {
                        var ia = d3.interpolate(this._current, a),
                            ir = d3.interpolate(this._current.radius, radius);
                        this._current = _.extend(a, { radius: radius });
                        return function(t) {
                            return d3.svg.arc().outerRadius(ir(t))(ia(t));
                        };
                    });

            slices.exit().remove();

            var labelTransform = function(d, i) {
                return 'translate(0,' + (i * self.LEGEND_LABEL_HEIGHT) + ')';
            };

            gLabels.enter().append('g').classed('legend-item', true)
                .call(function() {
                   this.append('text')
                       .attr('x', self.LEGEND_COLOR_SWATCH_SIZE + self.LEGEND_SWATCH_TO_TEXT_MARGIN)
                       .style('text-anchor', 'start')
                       .text(displayName.bind(self));
                   this.append('rect')
                        .attr({ width: self.LEGEND_COLOR_SWATCH_SIZE, height: self.LEGEND_COLOR_SWATCH_SIZE})
                        .attr('y', -self.LEGEND_COLOR_SWATCH_SIZE);
                })
                .attr('transform', labelTransform)
                .style('opacity', 0)
                .on('mousedown', tip.hide)
                .on('mouseout', function(d, i, n) {
                    tip.hide();
                    d3.select(slices[0][i]).attr('fill', self.colors[isOtherCategory(d) ? self.OTHER_COLOR_INDEX : i]);
                })
                .on('mouseover', function(d, i, n) {
                    tip.show(d, gPie[0][0]);
                    d3.select(slices[0][i]).attr('fill', self.highlightColors[isOtherCategory(d) ? self.OTHER_COLOR_INDEX : i]);
                });

            gLabels.exit().transition().duration(self.TRANSITION_DURATION)
                .style('opacity', 0)
                .remove();

            gLabels.order().transition().duration(self.TRANSITION_DURATION)
                .style('opacity', 1)
                .attr('transform', labelTransform)
                .selectAll('rect')
                .style('fill', function(d, i, n) {
                    return self.colors[isOtherCategory(d) ? self.OTHER_COLOR_INDEX : n];
                });

            var gLabelXOffset = 2 * radius + self.CHART_PADDING,
                availableLegendSpace = width - self.CHART_PADDING - gLabelXOffset,
                availableLegendTextWidth = availableLegendSpace - self.LEGEND_COLOR_SWATCH_SIZE - self.LEGEND_SWATCH_TO_TEXT_MARGIN;
            gLegend.attr('transform', 'translate(' + gLabelXOffset + ',' + self.CHART_PADDING + ')');
            if (availableLegendTextWidth < 10 /* 10 is a totally arbitrary min width */) {
                gLegend.style('opacity', 0);
            } else {
                gLegend.style('opacity', 1);
                if (gLegend.node().getBBox().width > availableLegendSpace) {
                    gLabels.selectAll('text').each(function(d) {
                        var d3Self = d3.select(this),
                            text = d3.select(this).text();
                        while (this.getBBox().width > availableLegendTextWidth) {
                            text = text.substring(0, text.length - 1);
                            d3Self.text(text + '...');
                        }
                    });
                } else {
                    gLabels.selectAll('text').text(displayName.bind(self));
                }
            }

            if (this.handleClick) {
                slices.on('click', function(d) {
                    var filter = {
                        propertyId: d.data.field,
                        values: [nameFn(d)],
                        predicate: 'equal'
                    }
                    if (isOtherCategory(d)) {
                        console.info('Unable to search on other list', d.data.field);
                        return;
                    } else if (self.isHistogram) {
                        filter.values.push(String(nameAsFloat(d) + self.aggregation.interval));
                        filter.predicate = 'range';
                    }
                    self.handleClick({ filters: [filter] });
                });
                gLabels.on('click', function(object) {
                    self.handleClick(object.data);
                })
                gLabels.classed('clickable', clickableFn);
                slices.classed('clickable', clickableFn);
            }
        };

    }
});

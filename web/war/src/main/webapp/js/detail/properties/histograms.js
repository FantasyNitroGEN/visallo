define([
    'flight/lib/component',
    'util/vertex/formatters',
    'd3',
    'util/requirejs/promise!util/service/ontologyPromise'
], function(defineComponent, F, d3, ontology) {
    'use strict';

    var HISTOGRAM_STYLE = 'max', // max or sum
        GRAPH_TYPE = 'GRAPH_TYPE',
        EDGE_LABEL = 'edgeLabel',
        CONCEPT_TYPE = 'http://visallo.org#conceptType',
        MIN_VALUES_PRESENT_TO_DISPLAY = 1,
        BAR_HEIGHT = 25,
        PADDING = 5,
        ANIMATION_DURATION = 400,
        BINABLE_TYPES = 'double date currency integer number'.split(' '),
        SCALE_COLOR_BASED_ON_WIDTH = false,
        SCALE_COLOR_BASED_ON_WIDTH_RANGE = ['#00A1F8', '#0088cc'],
        SCALE_OPACITY_BASED_ON_WIDTH = true,
        SCALE_OPACITY_BASED_ON_WIDTH_RANGE = [50, 90],
        NO_HISTOGRAM_DATATYPES = [
            'geoLocation'
        ],
        MAX_BINS_FOR_NON_HISTOGRAM_TYPES = 5,
        OTHER_PLACEHOLDER = '${OTHER-CATEGORY}';

    return defineComponent(PropertyHistograms);

    function PropertyHistograms() {

        this.attributes({
            model: null,
            histogramSelector: '.histogram',
            histogramListSelector: '.histograms',
            histogramBarSelector: 'g.histogram-bar'
        })

        this.before('teardown', function() {
            this.$node.off('mouseenter mouseleave');
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.addClass('multiple').html('<ul class="histograms">');

            this.on('click', {
                histogramBarSelector: this.histogramClick
            });
            this.$node.on('mouseenter mouseleave', '.histogram-bar', this.histogramHover.bind(this));

            this.renderHistograms(this.attr.model, { duration: 0 });

            var previousSize = -1;
            this.debouncedRender = _.debounce(function(event, data) {
                self.renderHistograms(self.attr.model, { duration: 0 });
            }, 500);

            $(document).off('.elementHistograms');
            this.on(document, 'graphPaddingUpdated.elementHistograms', function(event, data) {
                if (data.padding.r > 0 && (previousSize < 0 || data.padding.r !== previousSize)) {
                    previousSize = data.padding.r;
                    this.debouncedRender();
                }
                if (data.padding.r === 0) {
                    $(document).off('.elementHistograms');
                }
            })

            this.on('updateModel', function(event, data) {
                this.attr.model = data.model;
                this.renderHistograms(data.model);
            })
        });

        this.renderHistograms = function(elements, options) {
            var self = this,
                opacityScale = d3.scale.linear().domain([0, 100]).range(SCALE_OPACITY_BASED_ON_WIDTH_RANGE),
                colorScale = d3.scale.linear().domain([0, 100]).range(SCALE_COLOR_BASED_ON_WIDTH_RANGE),
                animationDuration = (options && _.isNumber(options.duration)) ? options.duration : ANIMATION_DURATION;

            var propertySections = _.chain(elements)
                    .map(function(element) {
                        var minimalElement = {
                                elementType: element.type,
                                elementId: element.id
                            },
                            typeProperty = {
                                name: GRAPH_TYPE,
                                value: element.type
                            },
                            extras = [typeProperty];
                        if (element.type === 'edge') {
                            extras.push({ value: element.label, name: EDGE_LABEL});
                        }
                        return element.properties.concat(extras).map(function(p) {
                            return $.extend({}, p, minimalElement);
                        })
                    })
                    .flatten()
                    .filter(shouldDisplayProperty)
                    .groupBy('name')
                    .pairs()
                    .each(function(pair) {
                        if (pair[0] === CONCEPT_TYPE) {
                            pair[1] = _.reject(pair[1], function(element) {
                                return element.elementType === 'edge';
                            })
                        }
                    })
                    .filter(function(pair) {
                        var ontologyProperty = ontology.properties.byTitle[pair[0]];
                        if (ontologyProperty && ~BINABLE_TYPES.indexOf(ontologyProperty.dataType)) {
                            return true;
                        }
                        if (ontologyProperty && ontologyProperty.possibleValues) {
                            return true;
                        }

                        var valueCounts = _.groupBy(pair[1], 'value'),
                            values = _.map(valueCounts, function(value, key) {
                                return value.length;
                            }),
                            len = values.length;

                        if (len <= MAX_BINS_FOR_NON_HISTOGRAM_TYPES) {
                            return true;
                        }

                        var orderedCounts = _.unique(values)
                                .sort(function(a, b) {
                                    return a - b;
                                }),
                            collapseSmallest = function(orderedCounts, valueCounts, len) {
                                if (orderedCounts.length === 0 || len <= MAX_BINS_FOR_NON_HISTOGRAM_TYPES) {
                                    return;
                                }

                                var moveToOtherWithCount = orderedCounts.shift(),
                                    toMove = _.chain(valueCounts)
                                        .pairs()
                                        .filter(function(p) {
                                            return p[1].length === moveToOtherWithCount;
                                        })
                                        .value(),
                                    toMoveNames = _.map(toMove, function(p) {
                                            return p[0];
                                        });

                                pair[1] = _.reject(pair[1], function(p) {
                                    return ~toMoveNames.indexOf(p.value);
                                });

                                for (var i = 0; i < toMove.length; i++) {
                                    for (var j = 0; j < toMove[i][1].length; j++) {
                                        pair[1].push({
                                            value: OTHER_PLACEHOLDER,
                                            elementType: toMove[i][1][j].elementType,
                                            elementId: toMove[i][1][j].elementId
                                        });
                                    }
                                }

                                valueCounts = _.groupBy(pair[1], 'value');
                                values = _.map(valueCounts, function(value, key) {
                                    return value.length;
                                });
                                len = values.length;

                                collapseSmallest(orderedCounts, valueCounts, len);
                            };

                        collapseSmallest(orderedCounts, valueCounts, len);

                        return true;
                    })
                    .filter(function(pair) {
                        return pair[1].length >= MIN_VALUES_PRESENT_TO_DISPLAY;
                    })
                    .sortBy(function(pair) {
                        var ontologyProperty = ontology.properties.byTitle[pair[0]],
                            value = pair[0];

                        if (value === GRAPH_TYPE) {
                            return '0';
                        }
                        if (value === CONCEPT_TYPE) {
                            return '1';
                        }
                        if (value === EDGE_LABEL) {
                            return '2';
                        }
                        if (ontologyProperty && ontologyProperty.displayName) {
                            value = ontologyProperty.displayName;
                        }
                        return '3' + value.toLowerCase();
                    })
                    .sortBy(function(pair) {
                        return pair[1].length * -1;
                    })
                    .value(),
                container = this.select('histogramListSelector'),
                width = container.width();

            d3.select(container.get(0))
                    .selectAll('li.property-section')
                    .data(propertySections, function(d) {
                        return d[0];
                    })
                    .call(function() {
                        this.enter()
                            .append('li').attr('class', 'property-section')
                            .call(function() {
                                this.append('div').attr('class', 'nav-header')
                                this.append('svg').attr('width', '100%')
                            });
                        this.exit().remove();

                        this.order()
                            .call(function() {
                                this.select('.nav-header')
                                    .text(propertyDisplayName)
                                    .append('span')
                                    .text(function(d) {
                                        var ontologyProperty = ontology.properties.byTitle[d[0]],
                                            values = _.pluck(d[1], 'value');


                                        return _.map(F.vertex.rollup(d[0], values), function(value, label) {
                                            return label + '=' + value;
                                        }).join(', ');
                                    })
                                this.select('svg')
                                    .call(function() {
                                        this.transition()
                                            .duration(animationDuration)
                                            .attr('height', function(d) {
                                                var ontologyProperty = ontology.properties.byTitle[d[0]],
                                                    values = _.pluck(d[1], 'value');

                                                d.values = values;

                                                if (ontologyProperty &&
                                                    ~BINABLE_TYPES.indexOf(ontologyProperty.dataType) &&
                                                   !ontologyProperty.possibleValues) {

                                                    var bins = _.reject(
                                                        d3.layout.histogram().value(_.property('value'))(d[1]), function(bin) {
                                                        return bin.length === 0;
                                                    });

                                                    d.bins = bins;
                                                    return bins.length * BAR_HEIGHT;
                                                }

                                                if ('bins' in d) {
                                                    delete d.bins;
                                                }

                                                return _.unique(_.pluck(d[1], 'value')).length * BAR_HEIGHT;
                                            })
                                    })
                                    .selectAll('.histogram-bar')
                                    .data(function(d) {
                                        var xScale, yScale,
                                            bins,
                                            values = d.values;

                                        if ('bins' in d) {
                                            bins = d.bins;

                                            xScale = d3.scale.linear().range([0, 100]);
                                            yScale = d3.scale.ordinal();

                                            bins = bins.map(function(bin) {
                                                    bin.elements = _.clone(bin);
                                                    bin.xScale = xScale;
                                                    bin.yScale = yScale;
                                                    bin.name = d[0];
                                                    for (var i = 0; i < bin.length; i++) {
                                                        bin[i] = bin[i].value;
                                                    }
                                                    return bin;
                                                });
                                            var domainValues = _.reject(_.pluck(bins, '0'), function(v) {
                                                return _.isUndefined(v);
                                            });
                                            yScale.domain(domainValues);
                                            yScale.rangeRoundBands([0, bins.length * BAR_HEIGHT], 0.1, 0);
                                            xScale.domain([0, d3[HISTOGRAM_STYLE](bins, function(d) {
                                                return d.length
                                            })]);

                                            return bins;
                                        }

                                        xScale = d3.scale.linear()
                                            .range([0, 100]);

                                        var groupedByValue = _.groupBy(d[1], 'value');
                                        yScale = d3.scale.ordinal()
                                            .domain(
                                                _.chain(values)
                                                .unique()
                                                .sortBy(function(f) {
                                                    if (f === OTHER_PLACEHOLDER) {
                                                        return 999;
                                                    }

                                                    var bin = [f];
                                                    bin.name = d[0];
                                                    var value = propertyValueDisplay(bin);
                                                    return (100 - groupedByValue[f].length) +
                                                        String(value).toLowerCase();
                                                })
                                                .value()
                                            );

                                        bins = _.chain(groupedByValue)
                                                .pairs()
                                                .map(function(bin) {
                                                    bin.xScale = xScale;
                                                    bin.yScale = yScale;
                                                    bin.name = d[0];
                                                    bin.elements = bin[1];
                                                    bin[1] = bin[1].length;
                                                    return bin;
                                                })
                                                .value();

                                        yScale.rangeRoundBands([0, bins.length * BAR_HEIGHT], 0.1, 0)
                                        xScale.domain([0, d3[HISTOGRAM_STYLE](bins, function(d) {
                                            return d[1];
                                        })]);

                                        return bins;
                                    })
                                    .call(function() {
                                        this.enter()
                                            .append('g')
                                                .attr('class', 'histogram-bar')
                                                .call(function() {
                                                    this.append('defs')
                                                        .call(function() {
                                                            this.call(createMask);
                                                            this.call(createMask);
                                                            this.append('text')
                                                                .attr('class', 'text')
                                                                .attr('x', PADDING)
                                                                .attr('text-anchor', 'start');
                                                            this.append('text')
                                                                .attr('class', 'text-number');

                                                            function createMask() {
                                                                this.append('mask')
                                                                    .attr('maskUnits', 'userSpaceOnUse')
                                                                    .attr('y', 0)
                                                                    .append('rect')
                                                                        .attr('x', 0)
                                                                        .attr('y', 0)
                                                                        .attr('fill', 'white')
                                                            }
                                                        })
                                                    this.append('rect').attr('class', 'bar-background');
                                                    this.append('rect')
                                                        .attr('width', '100%')
                                                        .attr('class', 'click-target')
                                                        .attr('height', barHeight)
                                                    this.append('use').attr('class', 'on-bar-text');
                                                    this.append('use').attr('class', 'off-bar-text');
                                                    this.append('use').attr('class', 'on-number-bar-text');
                                                    this.append('use').attr('class', 'off-number-bar-text');
                                                })
                                        this.exit()
                                            .transition()
                                            .duration(animationDuration)
                                            .style('opacity', 0)
                                            .remove();

                                        this.order()
                                            .attr('data-element-ids', function(d) {
                                                return JSON.stringify(d.elementIds || []);
                                            })
                                            .transition()
                                            .duration(animationDuration)
                                            .attr('transform', function(d) {
                                                return 'translate(0,' + d.yScale(d[0]) + ')';
                                            })

                                        this.select('rect.bar-background')
                                            .style('fill-opacity', _.compose(toPercent, barOpacity, barWidth))
                                            .style('fill', _.compose(barColor, barWidth))
                                            .attr('height', barHeight)
                                            .attr('width', _.compose(toPercent, barWidth));

                                        this.select('defs')
                                            .call(function() {
                                                this.select('mask:first-child')
                                                    .attr('id', _.compose(append('_0'), maskId))
                                                    .attr('height', barHeight)
                                                    .attr('width', _.compose(toPercent, maskWidth(0)))
                                                    .attr('x', _.compose(toPercent, maskX(0)))
                                                    .select('rect')
                                                        .attr('width', '500%')
                                                        .attr('height', barHeight)
                                                this.select('mask:nth-child(2)')
                                                    .attr('id', _.compose(append('_1'), maskId))
                                                    .attr('height', barHeight)
                                                    .attr('width', _.compose(toPercent, maskWidth(1)))
                                                    .attr('x', _.compose(toPercent, maskX(1)))
                                                    .select('rect')
                                                        .attr('width', '500%')
                                                        .attr('height', barHeight)

                                                this.select('.text')
                                                    .attr('id', textId)
                                                    .text(propertyValueDisplay)
                                                    .each(setTextY(0.15));

                                                this.select('.text-number')
                                                    .attr('id', textNumberId)
                                                    .text(textNumberValue)
                                                    .attr('x', textNumberX)
                                                    .attr('dx', PADDING * -1)
                                                    .attr('text-anchor', 'end')
                                                    .each(positionTextNumber)
                                                    .each(setTextY(0.2));
                                            });

                                        this.select('use.on-bar-text')
                                            .call(markOther)
                                            .attr('xlink:href', _.compose(toRefId, textId))
                                            .attr('mask', _.compose(toUrlId, append('_0'), maskId));
                                        this.select('use.off-bar-text')
                                            .call(markOther)
                                            .attr('xlink:href', _.compose(toRefId, textId))
                                            .attr('mask', _.compose(toUrlId, append('_1'), maskId));
                                        this.select('use.on-number-bar-text')
                                            .attr('xlink:href', _.compose(toRefId, textNumberId))
                                        this.select('use.off-number-bar-text')
                                            .attr('xlink:href', _.compose(toRefId, textNumberId))
                                            .attr('mask', _.compose(toUrlId, append('_1'), maskId));

                                        d3.selectAll('defs .text-number')
                                            .each(positionTextNumber);

                                    })

                                function markOther(useTag) {
                                    useTag.classed('other', function(pair) {
                                        return pair[0] === OTHER_PLACEHOLDER;
                                    })
                                }

                                function append(toAppend) {
                                    return function(str) {
                                        return str + toAppend;
                                    }
                                }

                                function setTextY(k) {
                                    return function() {
                                        // Firefox exception here
                                        var height = 22;
                                        try {
                                            height = this.getBBox().height;
                                        } catch(e) { /*eslint no-empty:0*/ }
                                        this.setAttribute('y', (BAR_HEIGHT / 2 + height * k) + 'px');
                                    };
                                }
                                function maskId(d, i, barIndex) {
                                    return 'section_' + barIndex + '_bar_' + i + '_mask';
                                }
                                function textId(d, i, barIndex) {
                                    return 'section_' + barIndex + '_bar_' + i + '_text';
                                }
                                function textNumberId(d, i, barIndex) {
                                    return 'section_' + barIndex + '_bar_' + i + '_textnumber';
                                }
                                function textNumberValue(d, i, barIndex) {
                                    return barNumber(d);
                                }
                                function textNumberX(d, i, barIndex) {
                                    return toPercent(barWidth(d, i, barIndex));
                                }
                                function toRefId(id) {
                                    return '#' + id;
                                }
                                function toUrlId(id) {
                                    return 'url(#' + id + ')';
                                }
                                function maskX(i) {
                                    return function(d, ignored, barIndex) {
                                        if (i === 0) {
                                            return '0';
                                        }

                                        return barWidth(d, i, barIndex);
                                    }
                                }
                                function maskWidth(i) {
                                    return function(d, ignored, barIndex) {
                                        var width = barWidth(d, i, barIndex);
                                        if (i === 0) {
                                            return width;
                                        }

                                        return 100 - width;
                                    }
                                }
                                function barHeight(d) {
                                    return d.yScale.rangeBand();
                                }
                                function barNumber(d) {
                                    if ('dx' in d) {
                                        return d.y;
                                    }
                                    return d[1];
                                }
                                function barWidth(d) {
                                    return d.xScale(barNumber(d));
                                }
                                function barColor(percent) {
                                    if (SCALE_COLOR_BASED_ON_WIDTH) {
                                        return colorScale(percent);
                                    }
                                    return undefined;
                                }
                                function barOpacity(percent) {
                                    if (SCALE_OPACITY_BASED_ON_WIDTH) {
                                        return opacityScale(percent);
                                    }
                                    return 100;
                                }
                                function toPercent(number) {
                                    return number + '%';
                                }
                            });
                    })
        };

        this.histogramHover = function(event, object) {
            var ids = eventToElementIdMap(event),
                eventName = event.type === 'mouseenter' ? 'focus' : 'defocus';
            this.trigger(document, eventName + 'Elements', ids);
        };

        this.histogramClick = function(event) {
            var ids = eventToElementIdMap(event);
            if (ids) {
                this.trigger(document, 'selectObjects', ids);
                this.trigger(document, 'defocusElements');
            }
        };
    }

    function eventToElementIdMap(event) {
        var bar = $(event.target).closest('g'),
            elements = bar.length && d3.select(bar.get(0)).datum().elements,
            ids = elements && _.chain(elements)
                .groupBy('elementType')
                .mapObject(function(elements) {
                    return _.pluck(elements, 'elementId')
                })
                .value();
        if (ids) {
            return {
                vertexIds: ids.vertex,
                edgeIds: ids.edge
            }
        }
    }

    function propertyDisplayName(pair) {
        if (pair[0] === EDGE_LABEL) {
            return i18n('detail.multiple.histogram.edgeLabel');
        }
        if (pair[0] === GRAPH_TYPE) {
            return i18n('detail.multiple.histogram.graph_type');
        }
        var o = ontology.properties.byTitle[pair[0]];
        return o && o.displayName || o;
    }

    function propertyValueDisplay(bin) {
        var propertyValue = bin[0],
            propertyName = bin.name,
            display = propertyValue;

        if (propertyName === GRAPH_TYPE) {
            display = i18n('detail.multiple.histogram.graph_type.' + display);
        } else if (propertyName === CONCEPT_TYPE && ontology.concepts.byId[propertyValue]) {
            display = ontology.concepts.byId[propertyValue].displayName;
        } else if (propertyName === EDGE_LABEL && ontology.relationships.byTitle[propertyValue]) {
            display = ontology.relationships.byTitle[propertyValue].displayName;
        } else if (display === OTHER_PLACEHOLDER) {
            display = i18n('detail.multiple.histogram.other');
        } else if (ontology.properties.byTitle[propertyName]) {
            if ('dx' in bin) {
                display =
                    F.vertex.propDisplay(propertyName, bin.x) +
                    ' – ' +
                    F.vertex.propDisplay(propertyName, bin.x + bin.dx);
            } else {
                display = F.vertex.propDisplay(propertyName, propertyValue);
            }
        }
        if (display === '') return i18n('detail.multiple.histogram.blank');
        return display;
    }


    function shouldDisplayProperty(property) {
        var propertyName = property.name;

        if (property.streamingPropertyValue) {
            return false;
        }

        if (propertyName === CONCEPT_TYPE ||
           propertyName === GRAPH_TYPE ||
           propertyName === EDGE_LABEL) {
            return true;
        } else {
            var ontologyProperty = ontology.properties.byTitle[propertyName];
            if (ontologyProperty && ~NO_HISTOGRAM_DATATYPES.indexOf(ontologyProperty.dataType)) {
                return false;
            }
            return !!(ontologyProperty && ontologyProperty.userVisible);
        }
    }

    function positionTextNumber() {
        var self = this,
            t = this.previousSibling,
            tX = (t.x.baseVal[0] || t.x.baseVal.getItem(0)).value,
            getWidthOfNodeByClass = function(cls) {
                var node = self.parentNode;
                while ((node = node.nextSibling)) {
                    if (node.getAttribute('class').indexOf(cls) !== -1) {
                        return node.getBBox().width;
                    }
                }
                return 0;
            },
            textWidth = getWidthOfNodeByClass('on-bar-text'),
            textNumberWidth = getWidthOfNodeByClass('on-number-bar-text'),
            barRect = this.parentNode.nextSibling,
            barWidth = barRect.getBoundingClientRect().width,
            remainingBarWidth = barWidth - textWidth - tX - PADDING,
            mask0 = this.parentNode.querySelector('defs mask:first-child');

        if (remainingBarWidth <= textNumberWidth) {
            if (mask0) mask0.setAttribute('width', '100%');
            this.setAttribute('x', Math.max(barWidth, tX + textWidth) + PADDING);
            this.setAttribute('text-anchor', 'start');
            this.setAttribute('dx', 0);
        } else {
            if (mask0) mask0.setAttribute('width', (1 - (textNumberWidth + PADDING * 2) / barWidth) * 100 + '%');
            this.setAttribute('x', barWidth);
            this.setAttribute('dx', -PADDING);
            this.setAttribute('text-anchor', 'end');
        }
    }
});

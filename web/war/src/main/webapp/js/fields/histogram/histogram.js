define([
    'flight/lib/component',
    './histogramTpl.hbs',
    'd3',
    'util/withDataRequest',
    'colorjs',
    'data/web-worker/store/product/selectors'
], function(
    defineComponent,
    template,
    d3,
    withDataRequest,
    Color,
    productSelectors) {
    'use strict';

    var HEIGHT = 100,
        ALL_DATES = 'ALL_DATES',
        BRUSH_PADDING = 0,
        BRUSH_TEXT_PADDING = 2,
        BRUSH_BACKGROUND_HEIGHT = 13,
        MAX_ZOOM_IN = 1000,
        MAX_ZOOM_OUT = 100,
        DAY = 24 * 60 * 60 * 1000;

    return defineComponent(Histogram, withDataRequest);

    function inDomain(d, xScale) {
        var domain = xScale.domain();
        return d > domain[0] && d < domain[1];
    }

    function Histogram() {

        var margin = {top: 6, right: 16, bottom: 40, left: 16};

        this.defaultAttrs({
            noDataMessageSelector: '.no-data-message',
            noDataMessageDetailsText: i18n('histogram.no_data_details'),
            includeYAxis: false
        });

        this.before('teardown', function() {
            if (this.subscription) {
                this.subscription();
            }
        })

        this.after('initialize', function() {
            this.$node.html(template({
                noDataMessageDetailsText: this.attr.noDataMessageDetailsText
            }));

            this.triggerChange = _.debounce(this.triggerChange.bind(this), 500);
            this.onGraphPaddingUpdated = _.debounce(this.onGraphPaddingUpdated.bind(this), 500);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'edgesUpdated', this.onEdgesUpdated);
            this.on('propertyConfigChanged', this.onPropertyConfigChanged);
            this.on('fitHistogram', this.onFitHistogram);

            this.watchForProductChanges();

            if (!this.attr.property) {
                this.attr.property = {
                    title: ALL_DATES,
                    dataType: 'date'
                }
            }

            this.currentSelected = { vertexIds: [], edgeIds: [] };
            this.redraw = _.throttle(this.redraw.bind(this), 16);

            this.renderChart();
        });

        this.watchForProductChanges = function() {
            visalloData.storePromise.then(store => {
                var state = store.getState();
                var previous = productSelectors.getProduct(state);
                this.subscription = store.subscribe(() => {
                    state = store.getState();
                    var product = productSelectors.getProduct(state);
                    if (product !== previous && product) {
                        this.renderChart().then(() => this.updateBarSelection(this.currentSelected));
                    }
                    previous = product;
                });
            });
        }

        this.onFitHistogram = function() {
            this.zoom.scale(1).translate([0, 0]).event(this.svg);
        };

        this.onPropertyConfigChanged = function(event, data) {
            this.filteredPropertyIris = data.properties;
            this.redraw(true);
        };

        this.onObjectsSelected = function(event, data) {
            if (data && data.options && (data.options.fromHistogram || data.options.fromFilter)) {
                return;
            }

            var selectedVertices = (data && data.vertices) || [],
                selectedEdges = (data && data.edges) || [],
                selectedEdgeIds = _.pluck(selectedEdges, 'id').sort(),
                selectedVertexIds = _.pluck(selectedVertices, 'id').sort();

            this.clearBrush();
            this.currentSelected = {
                vertexIds: selectedVertexIds,
                edgeIds: selectedEdgeIds
            };
            this.updateBarSelection(this.currentSelected);
        };

        this.onEdgesUpdated = function() {
            var self = this;

            this.renderChart().then(function() {
                self.updateBarSelection(self.currentSelected);
            });
        };

        this.onVerticesUpdated = function() {
            var self = this;

            this.renderChart().then(function() {
                self.updateBarSelection(self.currentSelected);
            });
        };

        this.onWorkspaceLoaded = function() {
            this.renderChart();
        };

        this.onGraphPaddingUpdated = function(event, data) {
            if (this.xScale) {
                var padding = data.padding,
                    width = this.width = this.$node.scrollParent().width() - margin.left - margin.right,
                    height = width / (16 / 9);

                this.$node.find('svg').attr('width', width + margin.left + margin.right);

                this.xScale.range([0, width]);
                this.focus.style('display', 'none');
                this.redraw();
            }
        };

        this.redraw = function(rebin, skipAnimation) {
            var self = this;

            if (rebin) {
                if (!this.debouncedBin) {
                    this.debouncedBin = _.debounce(function(shouldSkipAnimation) {
                        self.binCount = null;
                        self.data = self.binValues();
                        self.yScale.domain([0, d3.max(self.data, function(layer) {
                            return d3.max(layer.values, function(d) {
                                return d.y0 + d.y;
                            });
                        }) || 0]);

                        if (self.attr.includeYAxis) {
                            self.svg.select('.y.axis').call(self.yAxis)
                                                      .selectAll('.tick text')
                        }

                        self.redraw(false, shouldSkipAnimation);
                    }, 250);
                }
                this.debouncedBin(skipAnimation);
            }

            this.createBars(this.data, skipAnimation);
            this.svg.select('.brush').call(this.brush.x(this.xScale));
            this.svg.select('.x.axis').call(this.xAxis.orient('bottom'));

            if (this.currentExtent) {
                var selectedObjectIds = this.getObjectIdsForExtent(this.currentExtent);
                if (!_.isEqual(selectedObjectIds, this.currentSelected)) {
                    this.currentSelected = selectedObjectIds;
                    this.triggerChange(_.extend({ extent: this.currentExtent }, this.currentSelected));
                }
            }
        };

        this.binValues = function() {
            var isDate = this.attr.property.dataType === 'date',
                isDateTime = this.attr.property.displayType !== 'dateOnly',
                allDates = this.attr.property.title === ALL_DATES,
                xScale = this.xScale,
                ontology = this.ontology;

            if (!this.binCount) {
                this.binCount = allDates ? xScale.ticks(100) : isDate ? xScale.ticks(25) : 25;
                if (_.isArray(this.binCount)) {
                    this.binCount = _.map(this.binCount, function(d) {
                        return _.isFunction(d.getTime) ? d.getTime() : d;
                    });
                }
            }

            var self = this,
                count = this.values.length === 0 ? 0 : this.binCount,
                histogram = d3.layout.histogram()
                    .value(_.property('value'))
                    .bins(count),
                histogramsByConcept = _.chain(this.values)
                    .groupBy('conceptIri')
                    .mapObject(function(conceptValues) {
                        var buckets = histogram(_.filter(conceptValues, function(v) {
                            if (self.filteredPropertyIris) {
                                if (!_.contains(self.filteredPropertyIris, v.propertyIri)) {
                                    return false;
                                }
                            }
                            return inDomain(v.value, xScale);
                        }));
                        _.each(buckets, function(bucket) {
                            bucket.x = isDate && !isDateTime ? d3.time.day.floor(bucket.x) : new Date(bucket.x)
                        });
                        return buckets;
                    })
                    .pairs()
                    .map(function(kv) {
                        var conceptIri = kv[0],
                            ontologyConcept = ontology.concepts.byId[conceptIri] || ontology.relationships.byTitle[conceptIri];

                        var normalColor = '#c4c4c4';
                        if (ontologyConcept && ontologyConcept.color) {
                            normalColor = new Color(ontologyConcept.color).setLightness(.6).setSaturation(.5).toString();
                        }
                        var dimColor = new Color(normalColor).setSaturation(.1).setLightness(.3).toString();

                        return { conceptIri: conceptIri, values: kv[1], normalColor: normalColor, dimColor: dimColor };
                    })
                    .value(),
                stack = d3.layout.stack().values(_.property('values'));

            return _.each(stack(histogramsByConcept), function(conceptData) {
                conceptData.values = _.filter(conceptData.values, _.property('length'));
            });
        }

        this.getObjectIdsForExtent = function(extent) {
            extent = extent || this.currentExtent;
            return _.chain(this.data || [])
                    .map(function(d) {
                        return _.filter(d.values, function(v) {
                           return extent &&
                               extent.length === 2 &&
                               v.x >= extent[0] &&
                               v.x <= extent[1];
                        });
                    })
                    .flatten()
                    .partition(function(d) {
                        return 'vertexId' in d;
                    })
                    .map(function(d, i) {
                        return i === 0 ?
                            ['vertexIds', _.pluck(d, 'vertexId').sort()] :
                            ['edgeIds', _.pluck(d, 'edgeId').sort()];
                    })
                    .object()
                    .value();
        };

        this.triggerChange = function(data) {
            this.trigger('updateHistogramExtent', data);
        };

        this.renderChart = function(results) {
            var self = this;

            if (!results) {
                return Promise.all([
                    this.dataRequest('workspace', 'histogramValues', this.attr.property),
                    this.dataRequest('ontology', 'ontology')
                ]).then(function(results) {
                    return self.renderChart(results);
                });
            }

            var vals = results[0],
                isDate = this.attr.property.dataType === 'date',
                isDateTime = isDate && this.attr.property.displayType !== 'dateOnly';

            if (isDate && !isDateTime) {
                vals.values.forEach(function(v) {
                    v.value = v.value + (new Date(v.value).getTimezoneOffset() * 60000);
                })
            }

            this.trigger('ontologyPropertiesRenderered', {
                ontologyProperties: vals.foundOntologyProperties
            });

            var foundOntologyProperties = this.foundOntologyProperties = vals.foundOntologyProperties,

                ontology = this.ontology = results[1],

                values = this.values = vals.values,

                width = this.width = this.$node.scrollParent().width() - margin.left - margin.right,
                height = this.height = HEIGHT - margin.top - margin.bottom,

                valuesExtent = calculateValuesExtent(),

                xScale = this.xScale = createXScale(),

                data = this.data = this.binValues(),

                yScale = this.yScale = d3.scale.linear()
                    .domain([0, d3.max(data, function(layer) {
                        return d3.max(layer.values, function(d) {
                            return d.y0 + d.y;
                        });
                    })])
                    .range([height, 0]),

                xAxis = this.xAxis = d3.svg.axis()
                    .scale(xScale)
                    .ticks(isDate ? 3 : 4)
                    .tickSize(5, 0)
                    .orient('bottom'),

                brushedTextFormat = xAxis.tickFormat(),

                format = (function() {
                    var format = isDate && '%Y-%m-%d';
                    if (isDateTime) {
                        format += ' %I %p';
                    }
                    format = format && d3.time.format(format);
                    if (format) {
                        brushedTextFormat = format;
                    }
                    return format;
                })(),

                yAxis = this.yAxis = d3.svg.axis()
                    .scale(yScale)
                    .ticks(0)
                    .tickSize(5, 0)
                    .tickValues(function() {
                        return yScale.domain()[1] !== 0 ? [0, yScale.domain()[1]] : [];
                    })
                    .orient('right'),

                onZoomedUpdate = _.throttle(function() {
                    updateBrushInfo();
                    updateFocusInfo();
                }, 1000 / 30),

                onZoomed = function() {
                    var scale = d3.event.scale,
                        scaleChange = scale !== self.previousScale,
                        translate = d3.event.translate[0],
                        translateChange = translate !== self.previousTranslate;

                    if (self.currentExtent) {
                        self.brush.extent(self.currentExtent);
                    }

                    self.redraw(scaleChange || translateChange, true);
                    self.previousScale = scale;
                    self.previousTranslate = translate;
                    onZoomedUpdate();
                },

                zoom = this.zoom = createZoomBehavior(),

                brush = (this.brush || (this.brush = d3.svg.brush()
                    .on('brush', _.throttle(function() {
                        updateBrushInfo();
                        updateFocusInfo();
                    }, 1000 / 30))))
                    //.call(function() {
                        //if (this.currentExtent) {
                            //brush = this.brush.extent(this.currentExtent);
                        //}
                    //})
                    .x(self.xScale),

                focus = null,

                svgOuter = this.svg = d3.select(this.node)
                    .selectAll('svg')
                    .data([1])
                    .call(function() {
                        this.enter().append('svg')
                            .on('mousemove', mousemove)
                            .on('mouseover', function() {
                                focus.style('display', null);
                            })
                            .on('mouseout', function() {
                                if (!d3.event.toElement || $(d3.event.toElement).closest('svg').length === 0) {
                                    focus.style('display', 'none');
                                }
                            });
                    })
                    .attr('width', width + margin.left + margin.right)
                    .attr('height', height + margin.top + margin.bottom)
                    .call(zoom),

                preventDragOverGraph = svgOuter
                    .selectAll('rect.preventDrag')
                    .data([1])
                    .call(function() {
                        this.enter().append('rect').attr({
                            class: 'preventDrag',
                            x: 0,
                            y: 0,
                            width: '100%'
                        })
                    })
                    .attr('height', height + margin.top)
                    .on('mousedown', function() {
                        d3.event.stopPropagation();
                    }),

                axisOverlay = svgOuter
                    .selectAll('rect.axis-overlay')
                    .data([1])
                    .call(function() {
                        this.enter().append('rect').attr({
                            class: 'axis-overlay',
                            x: 0,
                            width: '100%'
                        })
                    })
                    .attr({
                        y: height + margin.top,
                        height: margin.bottom
                    }),

                svg = svgOuter
                    .selectAll('g.root')
                    .data([1])
                    .call(function() {
                        this.enter().append('g').attr('class', 'root');
                    })
                    .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')'),

                svgBackground = svg
                    .selectAll('g.background')
                    .data([1])
                    .call(function() {
                        this.enter()
                            .append('g').attr('class', 'background')
                            .call(function() {
                                this.append('g').attr('class', 'focus').style('display', 'none')
                                    .call(function() {
                                        this.append('text').attr('text-anchor', 'middle');
                                        this.append('rect')
                                            .attr('class', 'scrub')
                                            .attr('width', 1)
                                            .attr('height', margin.bottom * 0.6);
                                    })
                                this.append('g').attr({
                                    class: 'x axis'
                                })
                            })

                        this.select('g.axis')
                            .attr('transform', 'translate(0,' + height + ')')
                            .call(xAxis);

                        focus = self.focus = this.select('.focus')
                            .call(function() {
                                this.select('text').attr('y', height + margin.bottom - 5)
                                this.select('rect').attr('y', height)
                            })
                    }),

                gBrush = svg
                    .selectAll('g.brush')
                    .data([1])
                    .call(function() {
                        this.enter().append('g').attr('class', 'brush')
                            .on('mousedown', function() {
                                d3.event.stopPropagation();
                            })

                    })
                    .call(brush),

                barGroup = this.barGroup = gBrush
                    .selectAll('g.barGroup')
                    .data([1])
                    .call(function() {
                        this.enter().insert('g', '.extent').attr('class', 'barGroup')
                    }),

                bars = this.createBars(data),

                gBrushRects = d3.selectAll(this.$node.find('g.brush rect').not('.barGroup rect').toArray())
                    .attr('height', height + BRUSH_PADDING * 2),

                gBrushText = gBrush
                    .selectAll('g.brushText')
                    .data([1])
                    .call(function() {
                        this.enter().append('g').style('display', 'none').attr('class', 'brushText')
                    }),

                gBrushTextStartBackground = gBrushText
                    .selectAll('rect.textstartbg')
                    .data([1])
                    .call(function() {
                        this.enter().append('rect').attr({
                            class: 'textstartbg',
                            x: 0.5,
                            y: 0.5
                        })
                    })
                    .attr('height', BRUSH_BACKGROUND_HEIGHT),

                gBrushTextEndBackground = gBrushText
                    .selectAll('rect.textendbg')
                    .data([1])
                    .call(function() {
                        this.enter().append('rect').attr({
                            class: 'textendbg',
                            x: 0.5
                        })
                    })
                    .attr({
                        y: height - BRUSH_BACKGROUND_HEIGHT,
                        height: BRUSH_BACKGROUND_HEIGHT
                    }),

                gBrushTextStart = gBrushText
                    .selectAll('text.textstart')
                    .data([1])
                    .call(function() {
                        this.enter().append('text').attr({
                            class: 'textstart',
                            x: BRUSH_TEXT_PADDING,
                            y: Math.max(0, BRUSH_BACKGROUND_HEIGHT - BRUSH_PADDING - BRUSH_TEXT_PADDING)
                        })
                    }),

                gBrushTextEnd = gBrushText
                    .selectAll('text.textend')
                    .data([1])
                    .call(function() {
                        this.enter().append('text').attr({
                            class: 'textend',
                            'text-anchor': 'end'
                        })
                    })
                    .attr('y', height + BRUSH_PADDING - BRUSH_TEXT_PADDING);

            if (isDate) {
                xAxis.tickFormat(d3.time.format.multi([
                    ['.%L', function(d) {
                        return d.getMilliseconds();
                    }],
                    [':%S', function(d) {
                        return d.getSeconds();
                    }],
                    ['%I:%M', function(d) {
                        return d.getMinutes();
                    }],
                    ['%I %p', function(d) {
                        return d.getHours();
                    }],
                    ['%a %d', function(d) {
                        return d.getDay() && d.getDate() !== 1;
                    }],
                    ['%b %d', function(d) {
                        return d.getDate() !== 1;
                    }],
                    ['%b', function(d) {
                        return d.getMonth();
                    }],
                    ['%Y', function() {
                        return true;
                    }]
                ]));
            } else {
                xAxis.tickFormat(function(d) {
                    return d3.format('s')(d)
                        .replace(/(\.\d{2})\d+/, '$1');
                });
            }

            if (this.attr.includeYAxis) {
                svg.selectAll('g.y')
                    .data([1])
                    .call(function() {
                        this.enter().append('g')
                            .attr('class', 'y axis')
                            .selectAll('.tick text')
                    })
                    .call(yAxis)
            }

            function calculateValuesExtent() {
                var min, max, delta, rawValues = _.pluck(values, 'value');

                if (isDate) {
                    if (values.length === 0) {
                        return [
                            d3.time.day.offset(new Date(), -1),
                            d3.time.day.offset(new Date(), 2)
                        ];
                    } else if (values.length === 1) {
                        return [
                            d3.time.day.offset(rawValues[0], -1),
                            d3.time.day.offset(rawValues[0], 2)
                        ];
                    } else {
                        min = d3.min(rawValues);
                        max = d3.max(rawValues);
                        delta = max - min;

                        var days = Math.max(1,
                            parseInt(Math.round(delta / 1000 / 60 / 60 / 24 * 0.1), 10)
                        );

                        return [
                            d3.time.day.offset(min, days * -1),
                            d3.time.day.offset(max, days)
                        ];
                    }
                } else if (values.length === 0) {
                    return [0, 100];
                } else if (values.length === 1) {
                    return [rawValues[0] - 1, rawValues[0] + 1];
                }

                min = d3.min(rawValues);
                max = d3.max(rawValues);
                delta = max - min;

                return [
                    min - delta * 0.1,
                    max + delta * 0.1
                ];
            }

            function createZoomBehavior() {
                var delta = valuesExtent[1] - valuesExtent[0],
                    maxZoomIn = delta / (isDateTime ? 36e5 : isDate ? 36e5 * 48 : 10),
                    maxZoomOut = delta / (isDate ? (50 * 365 * 24 * 36e5) : 1);

                return d3.behavior.zoom()
                    .x(xScale)
                    .scaleExtent([1 / 2, maxZoomIn])
                    .on('zoom', onZoomed);
            }

            function createXScale() {
                var scale = (isDateTime || isDate) ? d3.time.scale() : d3.scale.linear();
                return scale.domain(valuesExtent).range([0, width]);
            }

            function updateBrushInfo() {
                var extent = self.brush.extent(),
                    delta = extent[1] - extent[0],
                    width = Math.max(0, self.xScale(
                             isDate ?
                             new Date(self.xScale.domain()[0].getTime() + delta) :
                             (self.xScale.domain()[0] + delta)
                        ) - 1),
                    data = {
                        extent: delta < 0.00001 ? null : extent
                    };

                if (!_.isEqual(data.extent, self.currentExtent) && (data.extent || self.currentExtent)) {
                    self.updateBarSelection();
                    var objectsForExtent = self.getObjectIdsForExtent(data.extent);
                    _.extend(data, objectsForExtent);
                    if (!_.isEqual(objectsForExtent, self.currentSelected)) {
                        self.currentSelected = objectsForExtent;
                        self.triggerChange(data);
                    }
                }

                self.currentExtent = data.extent;

                gBrushTextStartBackground.attr('width', width);
                gBrushTextEndBackground.attr('width', width);
                gBrushTextStart.text(brushedTextFormat(extent[0]));

                gBrushText
                    .style('display', delta < 0.01 ? 'none' : '')
                    .attr('transform', 'translate(' + self.xScale(extent[0]) + ', 0)');

                gBrushTextEnd
                    .text(brushedTextFormat(extent[1]))
                    .attr(
                        'transform',
                        'translate(' + (width - BRUSH_TEXT_PADDING) + ', 0)'
                    );
            }
            this.clearBrush = function() {
                this.currentSelected = {};
                gBrush.call(self.brush.clear());
                delete self.currentExtent;
                updateBrushInfo();
            }

            var mouse = null;
            function mousemove() {
                mouse = d3.mouse(this)[0];
                updateFocusInfo();
            }

            function updateFocusInfo() {
                if (mouse !== null) {
                    var x0 = self.xScale.invert(mouse - margin.left);
                    focus.attr('transform', 'translate(' + self.xScale(x0) + ', 0)');
                    if (isDate) {
                        focus.select('text').text(format(x0));
                    } else {
                        focus.select('text').text(xAxis.tickFormat()(x0));
                    }
                }
            }

            this.redraw(true);
        }

        this.createBars = function(data, skipAnimation) {
            var self = this,
                height = this.height,
                xScale = this.xScale,
                yScale = this.yScale,
                firstNonEmpty = _.find(data, function(d) {
                    return d.values.length;
                }),
                dx = firstNonEmpty ? firstNonEmpty.values[0].dx : 0,
                barLayers = this.barGroup.selectAll('.barlayer').data(data.reverse(), function(d) {
                    return d.conceptIri;
                }).order(),
                bars = barLayers.selectAll('.bar').data(function(groupData) {
                    return groupData.values;
                }, function(d) {
                    return d[0].conceptIri + d.x.getTime();
                }).order(),
                isDate = this.attr.property.dataType === 'date',
                animationDuration = skipAnimation ? 0 : 250,
                hasData = this.data && this.data.length;

            this.select('noDataMessageSelector').css('display', hasData ? 'none' : 'block');
            this.svg.style('display', hasData ? 'block' : 'none');

            barLayers.enter().append('g').attr('class', 'barlayer').style('fill', function(d) {
                return d.normalColor;
            });

            bars.enter()
                .append('g').attr('class', 'bar')
                .classed('selected', function(d) {
                    return !self.currentExtent && d.length && _.any(d, function(o) {
                        return o.vertexId && _.contains(self.currentSelected.vertexIds || [], o.vertexId) ||
                            o.edgeId && _.contains(self.currentSelected.edgeIds || [], o.edgeId);
                    });
                })
                .attr('transform', function(d) {
                    return 'translate(' + xScale(d.x) + ',' + yScale(0) + ')';
                })
                .append('rect')
                .attr('height', 0);

            bars.transition('height-animation').duration(animationDuration)
                .attr('transform', function(d) {
                    return 'translate(' + xScale(d.x) + ',' + yScale(d.y + d.y0) + ')';
                });
            bars.select('rect')
                    .attr('width',
                        Math.max(1,
                            (isDate ?
                             xScale(xScale.domain()[0].getTime() + dx) :
                             xScale(xScale.domain()[0] + dx)
                            ) - 1
                        )
                    )
                    .transition('height-animation').duration(animationDuration)
                    .attr('height', function(d) {
                        return height - yScale(d.y);
                    })

            var exitingBars = bars.exit().transition('height-animation').duration(animationDuration);
            exitingBars.attr('transform', function(d) {
                return 'translate(' + xScale(d.x) + ',' + yScale(0) + ')';
            });
            exitingBars.select('rect').attr('height', 0);
            exitingBars.remove();

            barLayers.exit().remove();

            if (skipAnimation) d3.timer.flush();

            bars.on('mousedown', function(d) {
                var xy = d3.mouse(self.svg.select('.brush').node()),
                    xInv = xScale.invert(xy[0]);
                self.clearBrush();
                self.brush.extent([xInv, xInv]);
                self.currentSelected.vertexIds = _.compact(_.pluck(d, 'vertexId')).sort();
                self.currentSelected.edgeIds = _.compact(_.pluck(d, 'edgeId')).sort();
                self.updateBarSelection(self.currentSelected);
                self.triggerChange(_.extend({ extent: self.currentExtent }, self.currentSelected));
            });

            return bars;
        }

        this.updateBarSelection = function(selected) {
            var barLayers = this.svg.selectAll('.barlayer'),
                bars = barLayers.selectAll('.bar'),
                selectedVertexIds = selected && selected.vertexIds || [],
                selectedEdgeIds = selected && selected.edgeIds || [],
                anySelected = selectedVertexIds.length || selectedEdgeIds.length;

            bars.classed('selected', anySelected && function(d) {
                return d.length && _.any(d, function(o) {
                    return o.vertexId && _.contains(selectedVertexIds, o.vertexId) ||
                        o.edgeId && _.contains(selectedEdgeIds, o.edgeId);
                });
            });
            barLayers.each(function(d) {
                var currentColor = anySelected ? d.dimColor : d.normalColor;
                d3.select(this).transition('fill-animation').style('fill', currentColor);
            });
        }
    }
});

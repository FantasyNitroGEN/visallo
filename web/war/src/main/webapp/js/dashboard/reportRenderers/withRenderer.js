define([
    'util/withDataRequest',
    'util/vertex/formatters',
    'colorjs',
    'd3',
    'util/requirejs/promise!util/service/ontologyPromise',
    'configuration/plugins/registry',
    'require',
    'd3-tip',
    'deep-freeze-strict'
], function(
    withDataRequest,
    F,
    Color,
    d3,
    ontologyPromise,
    registry,
    require,
    d3tip,
    deepFreeze) {
    'use strict';

    var NO_DATA = 'NO_DATA_MARKER',
        TOO_MANY_BUCKETS = 'TOO_MANY_BUCKETS',
        TOO_MANY_BUCKETS_HISTOGRAM = 'TOO_MANY_BUCKETS_HISTOGRAM',
        TOO_MANY_BUCKETS_GEOHASH = 'TOO_MANY_BUCKETS_GEOHASH',
        MAX_BUCKETS_BEFORE_ERROR = 150,
        MAX_GEOHASH_BUCKETS_BEFORE_ERROR = 1024,
        TYPE_ELEMENTS = 'TYPE_ELEMENTS',
        TYPE_AGGREGATION = 'TYPE_AGGREGATION',
        TYPE_UNKNOWN = 'TYPE_UNKNOWN',
        FIELD_CONCEPT_TYPE = 'http://visallo.org#conceptType',
        FIELD_EDGE_LABEL = '__edgeLabel',
        TIP_UNIQUE_IDENTIFIER = 0,
        ontology = ontologyPromise;

    $(document).on('ontologyUpdated', function(event, data) {
        ontology = data.ontology;
    })

    return withRenderer;

    function withRenderer() {

        withDataRequest.call(this);

        this.after('initialize', function() {
            this.CHART_PADDING = 14;
            this.TRANSITION_DURATION = 750;
            this.colorScale = d3.scale.category20c();
            this.colors = this.colorScale.range();
            this.highlightColors = _.map(this.colors, function(color) {
                return new Color(color).lightenByRatio(.1).toString();
            });
            this.LEGEND_COLOR_SWATCH_SIZE = 10;
            this.LEGEND_SWATCH_TO_TEXT_MARGIN = 5;
            this.LEGEND_LABEL_HEIGHT = 15;
            this.OTHER_COLOR_INDEX = 19;

            var self = this,
                displayed = false,
                savedResult,
                savedResultPreProcessing,
                $loading,
                report = this.attr.report,
                warn = _.once(function(msg) {
                    console.warn(msg, self);
                }),
                setAggregations = function(result) {
                    self.aggregations = result.root;
                    self.aggregation = _.first(self.aggregations);
                    return result;
                },
                processData = function(result) {
                    return Promise.resolve(getRootObject(report, result))
                        .then(function(result) {
                            var transformer = report.mapping && report.mapping.transformerModulePath;
                            if (transformer) {
                                return optionallyApplyTransformer(transformer, result);
                            }
                            return result;
                        })
                        .then(function(result) {
                            if (self.attr.item.configuration.reportRenderer && !checkRendererSupportsResponse(self.attr.item.configuration.reportRenderer, result)) {
                                delete self.attr.item.configuration.reportRenderer;
                                self.trigger('configurationChanged', {
                                    extension: self.attr.extension,
                                    item: self.attr.item
                                });
                            }
                            return result;
                        })
                        .then(setAggregations)
                        .then(function(result) {
                            var frozen = deepFreeze(result);
                            savedResultPreProcessing = frozen;
                            if (result.type === TYPE_AGGREGATION && (!_.isArray(result.root) || _.isEmpty(result.root[0].buckets))) {
                                throw new Error(NO_DATA);
                            }
                            if (result.type === TYPE_AGGREGATION && _.isArray(result.root)) {
                                var bucketCount = result.root[0].buckets.length,
                                    aggregationType = result.root[0].type;

                                if (aggregationType === 'geohash' && bucketCount > MAX_GEOHASH_BUCKETS_BEFORE_ERROR) {
                                    throw new Error(TOO_MANY_BUCKETS_GEOHASH);
                                } else if (aggregationType !== 'geohash' && bucketCount > MAX_BUCKETS_BEFORE_ERROR) {
                                    if (aggregationType === 'histogram') {
                                        throw new Error(TOO_MANY_BUCKETS_HISTOGRAM);
                                    }
                                    throw new Error(TOO_MANY_BUCKETS);
                                }
                            }
                            if (_.isFunction(self.processData)) {
                                return self.processData(frozen);
                            }
                            return frozen;
                        });
                },
                saveData = function(result) {
                    savedResult = result;
                },
                requestData = function(params) {
                    return self.dataRequest('dashboard', 'requestData', report.endpoint, params || {});
                },
                render = function() {
                    if (_.isFunction(self.render)) {
                        var containerNode = self.$node;
                        displayed = containerNode.width() && containerNode.height();
                        if (displayed && savedResult) {
                            self.render(d3, containerNode[0], savedResult, d3tip);
                            self.$node.removeClass('error');
                            self.trigger('finishedLoading');
                        } else throw new Error(NO_DATA);
                    } else {
                        warn('Implement render(d3, svgNode, data, [d3tip]) function');
                    }
                },
                errorHandler = function(error) {
                    self.trigger('finishedLoading');

                    var type = 'error',
                        message = type;

                    if (error && error.message === NO_DATA) {
                        type = 'info';
                        message = 'no-data';
                    } else if (error && error.message === TOO_MANY_BUCKETS_GEOHASH) {
                        type = 'info';
                        message = 'bucket-overload.geohash';
                    } else if (error && error.message === TOO_MANY_BUCKETS_HISTOGRAM) {
                        type = 'info';
                        message = 'bucket-overload.histogram';
                    } else if (error && error.message === TOO_MANY_BUCKETS) {
                        type = 'info';
                        message = 'bucket-overload';
                    } else {
                        console.error(error);
                        self.$node.addClass('error');
                    }
                    var subtitle = i18n(true, 'dashboard.' + message + '.subtitle'),
                        $message = $('<h1>').text(i18n('dashboard.' + message)),
                        $dom = $('<div>').addClass(type).append($message);
                    if (subtitle) {
                        $message.append($('<div>').text(subtitle))
                    }
                    $dom.appendTo(self.node)
                },
                loadUsingData = function(data) {
                    return Promise.resolve(data)
                        .then(saveData)
                        .then(render)
                        .catch(errorHandler)
                },
                refresh = function(event, data) {
                    return Promise.resolve(report.endpointParameters)
                        .then(requestData)
                        .then(processData)
                        .then(loadUsingData)
                };

            this.on('reflow', function() {
                if (!displayed) {
                    this.$node.empty();
                }
                Promise.resolve()
                    .then(render)
                    .catch(errorHandler)
            });
            this.on('refreshData', function() {
                this.$node.empty();
                Promise.resolve()
                    .then(refresh)
                    .catch(errorHandler)
            });

            var loadingPromise = this.attr.result ?
                Promise.resolve(this.attr.result)
                    .then(setAggregations)
                    .then(loadUsingData) :
                refresh()
                    .catch(errorHandler);

            loadingPromise
                .catch(errorHandler)

            this.on('getReportResults', function() {
                loadingPromise.then(function() {
                    self.trigger('redirectEventToConfiguration', {
                        name: 'reportResults',
                        data: {
                            results: savedResultPreProcessing
                        }
                    })
                });
            })

            this.displayName = function(d) {
                if ('displayName' in d) return d.displayName;
                if (this.aggregation) {
                    return this.aggregation.displayName(d);
                }
                return d.name;
            }.bind(this);

        });


        this.fillSortedBucketGaps = function(buckets, interval) {
            var nameAsFloat = function(d) {
                    return parseFloat(d.name, 10);
                };

            buckets = _.sortBy(buckets, nameAsFloat);

            var min = nameAsFloat(_.first(buckets)),
                max = nameAsFloat(_.last(buckets));

            for (var i = min, currentData = 0; i <= max + interval; i = i + interval) {
                if (!buckets[currentData] || i !== nameAsFloat(buckets[currentData])) {
                    buckets.splice(currentData, 0, {
                        name: String(i),
                        value: {
                            count: 0
                        }
                    })
                }
                currentData++;
            }
            return buckets;
        };

        this.handleClick = function(object) {
            var self = this,
                report = this.attr.report,
                filters = object.filters;

            if (filters) {
                filters = _.reject(filters, function(filter) {
                    if (filter.propertyId === FIELD_EDGE_LABEL) {
                        object.edgeLabel = filter.values[0];
                        return true;
                    }
                    if (filter.propertyId === FIELD_CONCEPT_TYPE) {
                        object.conceptId = filter.values[0];
                        return true;
                    }
                })
            }

            if (report && _.isString(report.clickHandlerModulePath)) {
                var target = d3.event.target;
                return Promise.require(report.clickHandlerModulePath)
                    .then(function(clickHandler) {
                        if (_.isFunction(clickHandler)) {
                            return Promise.resolve(clickHandler.apply(self, [target, object]));
                        }
                        throw new Error('Click handler must be a function', clickHandler);
                    });
            } else if (object && object.conceptId) {
                // FIXME: extend original search params
                this.popupSearch('/vertex/search', {
                    q: '*',
                    conceptType: object.conceptId,
                    filter: JSON.stringify([])
                })
            } else if (object && object.edgeLabel) {
                // FIXME: extend original search params
                this.popupSearch('/edge/search', {
                    q: '*',
                    edgeLabel: object.edgeLabel,
                    filter: JSON.stringify([])
                })
            } else if (object && _.isArray(filters)) {
                if (report.endpoint === '/search/run') {
                    this.popupSearch(report.endpoint, {
                        id: report.endpointParameters.id,
                        filter: JSON.stringify(filters),
                        includeChildNodes: true
                    })
                } else {
                    var params = _.omit(report.endpointParameters, 'size', 'aggregations');
                    params.filter = JSON.stringify(JSON.parse(params.filter || '[]').concat(filters));
                    this.popupSearch('/element/search', params);
                }
            } else if (object && (/^other$/i).test(object.label)) {
                if (report.endpoint === '/search/run') {
                    var aggregations = report.endpointParameters.aggregations,
                        aggregationStr = _.first(aggregations),
                        aggregation = aggregationStr && JSON.parse(aggregationStr);

                    if (aggregation) {
                        // FIXME: extend original search params
                        this.popupSearch(report.endpoint, {
                            id: report.endpointParameters.id,
                            filter: JSON.stringify([{
                                propertyId: aggregation.field,
                                predicate: 'hasNot'
                            }])
                        })
                    }
                }
            }
        };

        this.popupSearch = function(url, search, target) {
            target = target || d3.event.target;

            if (_.isUndefined(search.includeChildNodes)) {
                search.includeChildNodes = false;
            }

            require(['./resultsPopover'], function(ResultsPopover) {
                ResultsPopover.attachTo(target, {
                    searchUrl: url,
                    search: search,
                    scrollSelector: '.grid-scroller'
                });
            })
        };

        this.createTooltip = function(svg, d3tip, titleFunction, detailFunction) {
            var tip = d3tip()
                .attr('class', 'dashboard-d3tip')
                .html(function(d, i, n) {
                    return $('<div>')
                        .append($('<h1>').text(titleFunction(d, i, n)))
                        .append($('<h2>').text(detailFunction(d, i, n)))
                        .html()
                })
                .attr('id', 'tip' + (TIP_UNIQUE_IDENTIFIER++));
            svg.call(tip);
            tip.show = _.wrap(tip.show, function(fn) {
                var args = _.toArray(arguments).slice(1);
                $('.dashboard-d3tip').empty();
                return fn.apply(tip, args);
            })

            return tip;
        }

    }

    function checkRendererSupportsResponse(reportRendererIdentifier, root) {
        const renderer = _.findWhere(
            registry.extensionsForPoint('org.visallo.dashboard.reportrenderer'),
            { identifier: reportRendererIdentifier }
        );
        return renderer.supportsResponse(root);
    }

    function getRootObject(report, result) {
        if (resultsIncludeAggregations(result)) {
            var aggregations = _.keys(result.aggregates);
            if (aggregations.length) {
                var aggregationsFromRequest = report.endpointParameters.aggregations.map(JSON.parse);
                return {
                    type: TYPE_AGGREGATION,
                    root: _.map(result.aggregates, function(aggregation, name) {
                        var aggregationFromRequest = _.findWhere(aggregationsFromRequest, { name: name }),
                            field = aggregationFromRequest && aggregationFromRequest.field,
                            type = aggregationFromRequest && aggregationFromRequest.type,
                            isConceptType = field === FIELD_CONCEPT_TYPE,
                            isEdgeLabel = field === FIELD_EDGE_LABEL,
                            isSpecialField = isConceptType || isEdgeLabel,
                            ontologyProperty = !isSpecialField && aggregationFromRequest && ontology.properties.byTitle[field],
                            displayNameFn = function(result) {
                                if (isConceptType) {
                                    var concept = ontology.concepts.byId[result.name];
                                    if (!concept) console.warn('No concept matching bucket name:', result.name);
                                    return concept ? concept.displayName : result.name;
                                } else if (isEdgeLabel) {
                                    var relationship = ontology.relationships.byTitle[result.name];
                                    if (!relationship) console.warn('No relationship matching bucket name:', result.name);
                                    return relationship ? relationship.displayName : result.name
                                } else {
                                    return F.vertex.propDisplay(result.field || field, result.name);
                                }
                            }

                        if (!field) throw new Error('Aggregation name not found in request:', name);
                        if (!ontologyProperty && !isSpecialField) throw new Error('Ontology field not found', aggregationFromRequest.field);

                        return _.tap({
                            name: name,
                            field: field,
                            type: type,
                            //ontology: ontologyProperty || undefined,
                            displayName: displayNameFn,
                            buckets: _.map(aggregation.buckets, function mapBucket(bucketValue, nameIri, list, fieldIri) {
                                if ('nestedResults' in bucketValue) {
                                    var key = _.first(_.keys(bucketValue.nestedResults)),
                                        aggregation = _.findWhere(aggregationFromRequest.nested, { name: key })
                                    if (!aggregation) throw new Error('Nested name not found in request', key);
                                    var buckets = _.map(bucketValue.nestedResults[key].buckets, _.partial(mapBucket, _, _, _, aggregation.field));
                                    if (buckets.length) {
                                        return {
                                            name: nameIri,
                                            field: fieldIri || field,
                                            value: {
                                                count: bucketValue.count,
                                                nested: buckets
                                            }
                                        }
                                    }
                                }
                                return {
                                    name: nameIri,
                                    field: fieldIri || field,
                                    value: bucketValue
                                };
                            })
                        }, function(r) {
                            if (type === 'histogram' && aggregationFromRequest) {
                                r.interval = parseInt(aggregationFromRequest.interval, 10);
                            } else if (type === 'geohash' && aggregationFromRequest) {
                                r.precision = parseInt(aggregationFromRequest.precision, 10);
                            }
                        });
                    })
                }
            } else {
                throw new Error(NO_DATA);
            }
        } else if (resultsFromSearch(result)) {
            return {
                type: TYPE_ELEMENTS,
                root: result.elements
            }
        } else {
            if (!report.mapping.transformerModulePath) {
                throw new Error('Report response is not recognized and no transformer configured', result);
            }
            return {
                type: TYPE_UNKNOWN,
                root: result
            };
        }
    }

    function optionallyApplyTransformer(transformerModulePath, object) {
        if (_.isString(transformerModulePath) && transformerModulePath.length) {
            return Promise.require(transformerModulePath)
                .then(function(transformer) {
                    if (_.isFunction(transformer)) {
                        return Promise.resolve(transformer(object));
                    }
                    throw new Error('Transformer must be a function', transformer);
                });
        }

        return object;
    }

    function resultsFromSearch(result) {
        return ('itemCount' in result || _.isObject(result.aggregates) || _.isArray('elements'));
    }

    function resultsIncludeAggregations(result) {
        return result && !_.isEmpty(result.aggregates);
    }
});


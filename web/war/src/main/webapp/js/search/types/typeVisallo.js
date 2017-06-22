define([
    'flight/lib/component',
    './withSearch',
    'util/formatters'
], function(
    defineComponent,
    withSearch,
    F) {
    'use strict';

    var SEARCH_RESULT_HEIGHT = 55,
        SearchComponent = defineComponent(SearchTypeVisallo, withSearch);

    SearchComponent.savedSearchUrl = '/vertex/search';

    return SearchComponent;

    function SearchTypeVisallo() {

        this.attributes({
            infiniteScrolling: true,
            searchType: 'Visallo',
            supportsSorting: true
        });

        this.after('initialize', function() {
            var self = this;
            this.currentFilters = {};

            this.on('filterschange', function(event, data) {
                data.setAsteriskSearchOnEmpty = true;
            })
            this.on('querysubmit', this.onQuerySubmit);
            this.on('queryupdated', this.onQueryUpdated);
            this.on('clearSearch', this.onClearSearch);
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);

            this.dataRequest('config', 'properties').done(function(configProperties) {
                self.exactMatch = (configProperties['search.exactMatch'] === 'true');
            })
        });



        this.onClearSearch = function() {
            if (this.currentRequest && this.currentRequest.cancel) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }
            this.trigger('setCurrentSearchForSaving');
        };

        this.processPropertyFilters = function() {
            var propertyFilters = this.currentFilters.propertyFilters,
                promise = propertyFilters && propertyFilters.length ?
                    this.dataRequest('ontology', 'properties') :
                    Promise.resolve();

            return promise.then(function(ontologyProperties) {
                if (ontologyProperties) {
                    // Coerce currency properties to strings
                    propertyFilters.forEach(function(f) {
                        var ontologyProperty = ontologyProperties.byTitle[f.propertyId];
                        if (ontologyProperty && ontologyProperty.dataType === 'currency') {
                            if (_.isArray(f.values)) {
                                f.values = f.values.map(function(v) {
                                    return String(v);
                                });
                            }
                        }
                    })
                }
            });
        };

        this.onQueryUpdated = function(event, data) {
            var self = this,
                query = data.value;

            if (query && query !== '*' && this.exactMatch) {
                query = '\"' + query + '\"'
            }

            this.currentQuery = query;
            this.currentFilters = data.filters;
            this.processPropertyFilters().done(function() {
                var options = {
                    query: query,
                    conceptFilter: data.filters.conceptFilter,
                    propertyFilters: data.filters.propertyFilters,
                    otherFilters: data.filters.otherFilters,
                    edgeLabelFilter: data.filters.edgeLabelFilter,
                    sort: data.filters.sortFields,
                    matchType: data.filters.matchType
                };
                self.triggerUpdatedSavedSearchQuery(options);
            })
        };

        this.triggerUpdatedSavedSearchQuery = function(options) {
            var self = this;

            return this.dataRequest('vertex', 'queryForOptions', options)
                .then(function(query) {
                    var parameters = _.omit(query.parameters, 'size', 'offset');
                    if (parameters.q && parameters.filter) {
                        self.trigger('setCurrentSearchForSaving', {
                            url: query.originalUrl,
                            parameters: parameters
                        });
                    } else {
                        self.trigger('setCurrentSearchForSaving');
                    }
                });
        }

        this.onQuerySubmit = function(event, data) {
            var self = this,
                query = data.value;

            if (query && query !== '*' && this.exactMatch) {
                query = '\"' + query + '\"';
            }

            this.currentQuery = query;
            this.currentFilters = data.filters;

            this.processPropertyFilters().then(function() {
                self.trigger('searchRequestBegan');
                self.triggerRequest(
                    query,
                    self.currentFilters.propertyFilters,
                    self.currentFilters.matchType,
                    self.currentFilters.conceptFilter,
                    self.currentFilters.edgeLabelFilter,
                    self.currentFilters.otherFilters,
                    self.currentFilters.sortFields,
                    { offset: 0 }
                )
                    .then(function(result) {
                        var unknownTotal = false,
                            verticesLength = result.elements.length;

                        if (!('totalHits' in result)) {
                            unknownTotal = true;
                            result.totalHits = verticesLength;
                        } else if (result.totalHits > verticesLength && verticesLength === 0) {
                            // totalHits includes deleted items so show no results
                            // if no vertices returned and hits > 0
                            result.totalHits = 0;
                        }

                        switch (self.currentFilters.matchType) {
                            case 'vertex': result.vertices = result.elements; break;
                            case 'edge': result.edges = result.elements; break;
                        }

                        self.trigger('searchRequestCompleted', {
                            success: true,
                            result: result,
                            message: i18n('search.types.visallo.hits.' +
                                (
                                    unknownTotal && result.totalHits >= (result.nextOffset - 1) ? 'unknown' :
                                    result.totalHits === 0 ? 'none' :
                                    result.totalHits === 1 ? 'one' :
                                    'many'
                                ),
                                F.number.prettyApproximate(result.totalHits))
                        });
                    }, function() {
                        self.trigger('searchRequestCompleted', { success: false, error: i18n('search.query.invalid') });
                    })
                    .done()
            });
        };

        this.triggerRequest = function(query, propertyFilters, matchType, conceptFilter, edgeLabelFilter, otherFilters, sortFields, paging) {
            if (this.currentRequest && this.currentRequest.cancel) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }

            if (paging && !paging.size) {
                var resultPageSize = Math.ceil(this.select('resultsSelector').height() / SEARCH_RESULT_HEIGHT);
                paging.size = resultPageSize * 2;
            }

            var self = this,
                options = {
                    query: query,
                    propertyFilters: propertyFilters,
                    conceptFilter: conceptFilter,
                    edgeLabelFilter: edgeLabelFilter,
                    otherFilters: otherFilters,
                    paging: paging,
                    sort: sortFields,
                    matchType: matchType
                };

            this.triggerUpdatedSavedSearchQuery(options);
            this.currentRequest = this.dataRequest.apply(
                this, ['vertex', 'search'].concat([options])
            )
            return this.currentRequest;
        };

        this.onInfiniteScrollRequest = function(event, data) {
            var query = this.currentQuery,
                trigger = this.trigger.bind(this,
                   this.select('resultsContainerSelector'),
                   'addInfiniteItems'
                );

            this.triggerRequest(
                query,
                this.currentFilters.propertyFilters,
                this.currentFilters.matchType,
                this.currentFilters.conceptFilter,
                this.currentFilters.edgeLabelFilter,
                this.currentFilters.otherFilters,
                this.currentFilters.sortFields,
                data.paging
            )
                .then(function(results) {
                    trigger({
                        success: true,
                        items: results.elements,
                        total: results.totalHits,
                        nextOffset: results.nextOffset
                    });
                })
                .catch(function() {
                    trigger({ success: false });
                })
        };

    }
});

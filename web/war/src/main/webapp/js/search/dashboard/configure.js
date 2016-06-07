define([
    'flight/lib/component',
    'util/withDataRequest',
    'hbs!./configureTpl',
    'require'
], function(
    defineComponent,
    withDataRequest,
    template,
    require) {
    'use strict';

    var LIMITS = [
            { value: '', name: 'Default' },
            { value: '1', name: '1' },
            { value: '5', name: '5' },
            { value: '10', name: '10' },
            { value: '25', name: '25' }
        ];

    return defineComponent(SavedSearchConfig, withDataRequest);

    function serializeSortField(sortField) {
        if (sortField) {
            return sortField.field + ':' + sortField.direction;
        }
    }

    function SavedSearchConfig() {

        this.defaultAttrs({
            searchSelector: 'select.search',
            aggregationSectionSelector: 'section.aggregation',
            limitSelector: '.limit select'
        });

        this.after('initialize', function() {
            var self = this;
            this.on('change', {
                searchSelector: this.onChangeSearch,
                limitSelector: this.onChangeLimit
            });
            this.on('sortFieldsUpdated', this.onSortFieldsUpdated);
            this.on('aggregationsUpdated', this.onAggregationsUpdated);
            this.on('statisticsForAggregation', this.onStatisticsForAggregation);

            this.aggregations = this.getAggregations();
            this.sortFields = this.getSortFields();
            this.limit = this.getLimit();

            this.render()
                .then(this.updateAggregationVisibility.bind(this, true))
                .then(function() {
                    self.trigger('positionDialog');
                })
        });

        this.onStatisticsForAggregation = function(event, data) {
            var configuration = this.attr.item.configuration,
                searchId = configuration.searchId,
                params = _.omit(configuration.searchParameters, 'aggregations'),
                paramsWithStats = _.extend(params, {
                    size: 0,
                    aggregations: [{
                        type: 'statistics',
                        field: data.aggregation.field,
                        name: 'field'
                    }].map(JSON.stringify)
                })

            return this.dataRequest('search', 'run', searchId, paramsWithStats)
                .then(function(r) {
                    if (r && r.aggregates) {
                        $(event.target).trigger('aggregationStatistics', {
                            success: true,
                            statistics: _.extend({
                                field: data.aggregation.field
                            }, r.aggregates.field)
                        })
                    } else throw new Error('No aggregates');
                })
                .catch(function() {
                    $(event.target).trigger('aggregationStatistics', {
                        success: false
                    });
                })
        };

        this.onAggregationsUpdated = function(event, data) {
            this.aggregations = data.aggregations;
            this.$node.find('.sort,.limit').toggle(this.aggregations.length === 0);

            if (this.aggregations.length) {
                this.attr.item.configuration.report = {
                    endpoint: '/search/run',
                    endpointParameters: {
                        id: this.attr.item.configuration.searchId,
                        size: 0,
                        aggregations: this.aggregations
                            .map(function(aggregation) {
                                if (aggregation.isDate) {
                                    aggregation.interval = aggregation.interval + 'ms';
                                    delete aggregation.isDate;
                                }
                                return aggregation;
                            })
                            .map(JSON.stringify)
                    }
                }
            }
            this.checkToTrigger(true);
        };

        this.onSortFieldsUpdated = function(event, data) {
            this.sortFields = data.sortFields;
            this.checkToTrigger(true);
        };

        this.checkToTrigger = function(changed) {
            if (_.isEmpty(this.aggregations) && !this.sortFields && !changed) return;

            var item = this.attr.item,
                aggregations = this.aggregations;

            if (!_.isEmpty(aggregations)) {
                item.configuration.searchParameters = {
                    aggregations: aggregations.map(JSON.stringify)
                }
            } else {
                delete item.configuration.report;
                item.configuration.searchParameters = {
                    sort: _.compact((this.sortFields || []).map(serializeSortField))
                }
                if (this.limit) {
                    item.configuration.searchParameters.size = this.limit;
                }
            }
            this.triggerChange();
        };

        this.updateAggregationVisibility = function(preventTrigger) {
            var self = this,
                searchId = this.attr.item.configuration.searchId,
                conceptType = searchId ? this.searchesById[searchId].parameters.conceptType : null;

            return Promise.all([
                Promise.require('search/dashboard/aggregation'),
                conceptType && this.dataRequest('ontology', 'propertiesByConceptId', conceptType)
            ]).spread(function(Aggregation, propertiesByConceptId) {
                var node = self.select('aggregationSectionSelector');

                node.toggle(!!self.attr.item.configuration.searchId);
                Aggregation.attachTo(node.teardownComponent(Aggregation), {
                    aggregations: self.aggregations
                })
                if (propertiesByConceptId) {
                    node.trigger('filterProperties', {
                        properties: propertiesByConceptId.list.filter(function(property) {
                            return !property.dependentPropertyIris
                        })
                    });
                }
                var aggregation = _.first(self.aggregations);
                return self.updateAggregationDependents(aggregation && aggregation.type, preventTrigger);

            });
        };

        this.render = function() {
            var self = this;
            this.$node.html(template({ loading: true }));

            return this.dataRequest('search', 'all')
                .then(function(searches) {
                    self.searchesById = _.indexBy(searches, 'id');

                    var config = self.attr.item.configuration;

                    self.$node.html(template({
                        searches: searches.map(function(search) {
                            var selected = false;
                            if (search.id === config.searchId) {
                                selected = true;
                            }
                            return _.extend({}, search, {
                                selected: selected
                            })
                        }),
                        limits: LIMITS.map(function(l) {
                            return _.extend({}, l, {
                                selected: l.value === String(self.limit)
                            })
                        })
                    }));
                })
        };

        this.getLimit = function(event) {
            var searchParameters = this.attr.item.configuration.searchParameters;
            if (searchParameters && searchParameters.size) {
                return searchParameters.size;
            }
        };

        this.getSortFields = function() {
            var sortRegex = /^(.*):(ASCENDING|DESCENDING)$/,
                searchParameters = this.attr.item.configuration.searchParameters;
            if (searchParameters && _.isArray(searchParameters.sort)) {
                return _.chain(searchParameters.sort)
                    .map(function(sort) {
                        var match = sort.match(sortRegex);
                        if (match && match.length === 3) {
                            return {
                                field: match[1],
                                direction: match[2]
                            };
                        }
                    })
                    .compact()
                    .value();
            }
        };

        this.getAggregations = function() {
            var searchParameters = this.attr.item.configuration.searchParameters;
            if (searchParameters && searchParameters.aggregations && searchParameters.aggregations.length) {
                return searchParameters.aggregations.map(JSON.parse)
            }
        }

        this.updateAggregationDependents = function(type, preventTrigger) {
            var self = this,
                item = this.attr.item,
                searchId = this.attr.item.configuration.searchId,
                conceptType = searchId ? this.searchesById[searchId].parameters.conceptType : null;

            if (type) {
                if (item.configuration.searchParameters) {
                    delete item.configuration.searchParameters.sort;
                    delete item.configuration.searchParameters.size;
                }
                this.sortFields = null;
                this.limit = null;
                this.$node.find('.sort, .limit').hide();
            } else {
                this.$node.find('.agg').hide();
                this.$node.find('.limit select').val(this.limit && this.limit || '')
                    .closest('.limit').show();

                Promise.all([
                    this.dataRequest('ontology', 'properties'),
                    Promise.require('search/sort'),
                    conceptType && this.dataRequest('ontology', 'propertiesByConceptId', conceptType)
                ]).spread(function(properties, Sort, propertiesByConceptId) {
                    var node = self.$node.find('.sort').show(),
                        sortFieldsNode = node.find('.sort-fields');

                    Sort.attachTo(sortFieldsNode.teardownComponent(Sort), {
                        sorts: self.sortFields
                    });

                    if (propertiesByConceptId) {
                        sortFieldsNode.trigger('filterProperties', {
                            properties: propertiesByConceptId.list.filter(function(property) {
                                return !property.dependentPropertyIris
                            })
                        });
                    }
                });
                this.aggregationField = null;
                if (item.configuration.searchParameters) {
                    delete item.configuration.searchParameters.aggregations;
                }
                delete this.attr.item.configuration.report;
                if (preventTrigger !== true) {
                    this.triggerChange();
                }

                return Promise.all([
                    this.dataRequest('ontology', 'properties'),
                    Promise.require('search/sort')
                ]).spread(function(properties, Sort) {
                    var node = self.$node.find('.sort').show();

                    Sort.attachTo(node.find('.sort-fields').teardownComponent(Sort), {
                        sorts: self.sortFields
                    })
                });
            }
        };

        this.attachPropertySelection = function(node, options) {
            if (!options) {
                options = {};
            }
            return Promise.all([
                this.dataRequest('ontology', 'properties'),
                Promise.require('util/ontology/propertySelect')
            ]).done(function(results) {
                var properties = results.shift(),
                    FieldSelection = results.shift();

                node.teardownComponent(FieldSelection);
                FieldSelection.attachTo(node, {
                    selectedProperty: options.selected && properties.byTitle[options.selected] || null,
                    properties: properties.list,
                    showAdminProperties: true,
                    placeholder: options.placeholder || ''
                });
            });
        };

        this.setTitle = function(search) {
            var title = this.attr.extension.title;
            if (search) {
                title += ': ' + search.name;
            }
            this.attr.item.title = title;
            this.attr.item.configuration.initialTitle = title;
        };

        this.onChangeLimit = function(event) {
            var val = $(event.target).val();
            if (!this.attr.item.configuration.searchParameters) {
                this.attr.item.configuration.searchParameters = {};
            }

            if (val) {
                this.limit = parseInt(val, 10);
                this.attr.item.configuration.searchParameters.size = this.limit;
            } else {
                this.limit = null;
                delete this.attr.item.configuration.searchParameters.size;
            }
            this.checkToTrigger(true);
        };

        this.onChangeSearch = function(event) {
            var searchId = $(event.target).val(),
                item = this.attr.item;

            item.configuration.searchId = searchId;
            this.setTitle(this.searchesById[searchId]);

            this.updateAggregationVisibility(true);
            this.checkToTrigger(true);
        };

        this.triggerChange = function() {
            this.trigger('configurationChanged', {
                extension: this.attr.extension,
                item: this.attr.item
            });
        };

    }
});

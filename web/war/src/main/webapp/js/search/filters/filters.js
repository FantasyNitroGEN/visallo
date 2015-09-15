
define([
    'flight/lib/component',
    'flight/lib/registry',
    'hbs!./filtersTpl',
    './filterItem',
    'tpl!./entityItem',
    'util/vertex/formatters',
    'util/ontology/conceptSelect',
    'util/withDataRequest',
    'configuration/plugins/registry'
], function(
    defineComponent,
    flightRegistry,
    template,
    FilterItem,
    entityItemTemplate,
    F,
    ConceptSelector,
    withDataRequest,
    registry) {
    'use strict';

    var FILTER_SEARCH_DELAY_SECONDS = 0.5;

    return defineComponent(Filters, withDataRequest);

    function Filters() {
        this.propertyFilters = {};
        this.otherFilters = {};
        this.filterId = 0;

        this.defaultAttrs({
            removeEntityRowSelector: '.entity-filters button.remove',
            removeExtensionRowSelector: '.extension-filters button.remove',
            extensionsSelector: '.extension-filters',
            filterItemsSelector: '.prop-filters .filter',
            conceptDropdownSelector: '.concepts-dropdown'
        });

        this.after('initialize', function() {
            var self = this;

            this.throttledNotifyOfFilters = _.throttle(this.notifyOfFilters.bind(this), 100);
            this.notifyOfFilters = _.debounce(this.notifyOfFilters.bind(this), FILTER_SEARCH_DELAY_SECONDS * 1000);

            this.$node.html(template({}));

            this.addSearchFilterExtensions()
                .done(function() {
                    self.trigger('filtersLoaded');
                });

            this.on('filterItemChanged', this.onFilterItemChanged);

            this.on('savedQuerySelected', this.onSavedQuerySelected);
            this.on('clearfilters', this.onClearFilters);
            this.on('click', {
                removeExtensionRowSelector: this.onRemoveExtensionRow,
                removeEntityRowSelector: this.onRemoveExtensionRow
            });
            this.on('change keyup', {
                extensionsSelector: function(e) {
                    e.stopPropagation();
                    e.preventDefault();
                }
            })
            this.on('conceptSelected', this.onConceptChange);
            this.on('searchByRelatedEntity', this.onSearchByRelatedEntity);
            this.on('searchByProperty', this.onSearchByProperty);
            this.on('filterExtensionChanged', this.onFilterExtensionChanged);

            this.loadPropertyFilters();
            this.loadConcepts();
        });

        this.onFilterItemChanged = function(event, data) {
            var $li = $(event.target).removeClass('newrow'),
                filterId = $li.data('filterId');

            if (_.isUndefined(filterId)) {
                console.error('Something is wrong, filter doesn\'t have id', $li[0]);
            }

            if (data.valid) {
                this.propertyFilters[filterId] = data.filter;
            } else {
                delete this.propertyFilters[filterId];
                if (data.removed) {
                    $li.remove();
                }
            }
            this.notifyOfFilters();
            this.createNewRowIfNeeded();
        };

        this.addSearchFilterExtensions = function() {
            var filterSearchType = this.attr.searchType,
                $container = this.select('extensionsSelector'),
                extensions = _.where(
                    registry.extensionsForPoint('org.visallo.search.filter'),
                    { searchType: filterSearchType }
                ),
                componentPromises = _.map(extensions, function(extension) {
                    return Promise.require(extension.componentPath);
                });

            if (componentPromises.length) {
                componentPromises.splice(0, 0, Promise.require('hbs!search/filters/extensionItem'))
                return Promise.all(componentPromises)
                    .then(function(components) {
                        var template = components.shift();

                        $container.html(
                            components.map(function(C, i) {
                                var extension = extensions[i];

                                if (extension.filterKeys) {
                                    extension.filterKey = JSON.stringify(extension.filterKeys);
                                }
                                var $li = $(template(extension));

                                C.attachTo($li, {
                                    configurationSelector: '.configuration',
                                    changedEventName: 'filterExtensionChanged',
                                    loadSavedQueryEvent: 'loadSavedQuery',
                                    savedQueryLoadedEvent: 'savedQueryLoaded'
                                });

                                if (extension.initHidden) {
                                    $li.hide();
                                }

                                return $li;
                            })
                        )
                    });
            } else return Promise.resolve();
        }

        this.onFilterExtensionChanged = function(event, data) {
            var options = data && data.options,
                newFilters = _.omit(data, 'options');

            if (options && options.clearOtherFilters && !this.disableNotify) {
                this.onClearFilters();
            }
            _.extend(this.otherFilters, newFilters);
            $(event.target).closest('.extension-filter-row').show();
            this.notifyOfFilters();
        };

        this.onSearchByProperty = function(event, data) {
            var self = this;

            event.stopPropagation();

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    var properties = data.properties || _.compact([data.property]);

                    self.onClearFilters();
                    self.disableNotify = true;

                    Promise.resolve(properties)
                        .each(function(property) {
                            var ontologyProperty = ontologyProperties.byTitle[property.name];
                            if (ontologyProperty &&
                                ontologyProperty.dataType === 'geoLocation' &&
                                _.isObject(property.value) &&
                                !('radius' in property.value)) {
                                property.value.radius = 1;
                            }
                            return self.addFilterItem({
                                propertyId: property.name,
                                values: property.values || [property.value],
                                predicate: property.predicate || '='
                            }, { hide: true });
                        })
                        .then(function() {
                            return self.setConceptFilter(data.conceptId);
                        })
                        .done(function() {
                            self.disableNotify = false;
                            self.$node.find('.filter').show();
                            self.notifyOfFilters();
                        });
                });
        };

        this.onSearchByRelatedEntity = function(event, data) {
            event.stopPropagation();
            var self = this;

            this.onClearFilters();

            Promise.resolve(this.setConceptFilter(data.conceptId))
                .then(function() {
                    self.setRelatedToEntityFilter(data.vertexIds);
                })
        };

        this.setRelatedToEntityFilter = function(vertexIds) {
            var self = this;

            return this.dataRequest('vertex', 'store', { vertexIds: vertexIds })
                .done(function(vertices) {
                    var single = vertices[0],
                        title = vertices.length > 1 ? i18n('search.filters.title_multiple', vertices.length)
                                                    : single && F.vertex.title(single) || single.id;

                    self.otherFilters.relatedToVertexIds = _.pluck(vertices, 'id');
                    self.$node.find('.entity-filters')
                        .append(entityItemTemplate({title: title})).show();
                    self.notifyOfFilters();
                });
        };

        this.setConceptFilter = function(conceptId) {
            var self = this;

            this.conceptFilter = conceptId || '';
            this.trigger(this.select('conceptDropdownSelector'), 'selectConceptId', { conceptId: conceptId });

            if (this.conceptFilter) {
                return this.dataRequest('ontology', 'propertiesByConceptId', this.conceptFilter)
                    .then(function(properties) {
                        self.filteredPropertiesList = _.reject(properties && properties.list || [], function(property) {
                            return !_.isEmpty(property.dependentPropertyIris);
                        });
                        self.select('filterItemsSelector').trigger('filterProperties', {
                            properties: self.filteredPropertiesList
                        })
                        self.notifyOfFilters();
                    })
            } else {
                this.filteredPropertiesList = null;
                this.select('filterItemsSelector').trigger('filterProperties', {
                    properties: this.properties
                })
                this.notifyOfFilters();
            }
        };

        this.onConceptChange = function(event, data) {
            this.setConceptFilter(data.concept && data.concept.id || '');
        };

        this.onClearFilters = function(event, data) {
            var self = this,
                filterItems = this.$node.find('.prop-filters .filter');

            filterItems.teardownAllComponents();

            this.setConceptFilter()
            this.createNewRowIfNeeded();
            this.otherFilters = {};
            this.$node.find('.entity-filters').hide().empty();
            this.$node.find('.extension-filter-row').hide();
            if (!data || data.triggerUpdates !== false) {
                this.notifyOfFilters();
            }
        };

        this.hasSomeFilters = function(filters) {
            return !!(filters &&
                !_.isEmpty(filters.conceptFilter) ||
                !_.isEmpty(filters.propertyFilters) ||
                !_.isEmpty(filters.otherFilters)
             );
        }

        this.notifyOfFilters = function(options) {
            if (this.disableNotify) return;

            var ontologyProperties = this.ontologyProperties,
                filters = {
                    otherFilters: this.otherFilters,
                    conceptFilter: this.conceptFilter,
                    propertyFilters: _.chain(this.propertyFilters)
                        .map(function(filter) {
                            var ontologyProperty = ontologyProperties.byTitle[filter.propertyId];
                            if (ontologyProperty && ontologyProperty.dependentPropertyIris) {
                                return ontologyProperty.dependentPropertyIris.map(function(iri, i) {
                                    if (_.isArray(filter.values[i]) && _.reject(filter.values[i], function(v) {
                                        return v === null || v === undefined;
                                    }).length) {
                                        return {
                                            propertyId: iri,
                                            predicate: filter.predicate,
                                            values: filter.values[i]
                                        }
                                    }
                                });
                            }

                            return {
                                propertyId: filter.propertyId,
                                predicate: filter.predicate,
                                values: filter.values
                            };
                        })
                        .flatten(true)
                        .compact()
                        .value()
                };

            filters.hasSome = this.hasSomeFilters(filters);
            filters.options = options;

            this.trigger('filterschange', filters);
        };

        this.onRemoveExtensionRow = function(event, data) {
            var self = this,
                target = $(event.target),
                row = target.closest('.extension-filter-row,.entity-filter-row'),
                keys = row.data('filterKey');

            row.hide();
            if (!_.isArray(keys)) {
                keys = [keys];
            }
            keys.forEach(function(key) {
                delete self.otherFilters[key];
            })
            this.notifyOfFilters();
        };

        this.createNewRowIfNeeded = function() {
            if (!this.properties) {
                return;
            }
            if (this.$node.find('.newrow').length === 0) {
                this.addFilterItem();
            }
        };

        this.onSavedQuerySelected = function(event, data) {
            var self = this,
                filters = JSON.parse(data.query.parameters.filter);

            this.dataRequest('ontology', 'properties')
                .then(function(ontologyProperties) {
                    self.onClearFilters();

                    self.disableNotify = true;
                    if (data.query.parameters['relatedToVertexIds[]']) {
                        return self.setRelatedToEntityFilter(data.query.parameters['relatedToVertexIds[]']);
                    }
                })
                .then(function() {
                    var matching = self.$node.find('.extension-filter-row').filter(function() {
                        var keys = $(this).data('filterKey');
                        if (!_.isArray(keys)) {
                            keys = [keys];
                        }
                        if (_.some(data.query.parameters, function(val, key) {
                            return _.contains(keys, key.replace(/\[\]$/, ''));
                        })) {
                            return true;
                        }
                    });
                    return Promise.resolve(matching.toArray())
                        .each(function(extensionLi) {
                            var $extensionLi = $(extensionLi),
                                keys = $extensionLi.data('filterKey'),
                                delaySecondsBeforeTimeout = 6;

                            if (!_.isArray(keys)) {
                                keys = [keys];
                            }
                            return new Promise(function(fulfill) {
                                var newFilters = _.chain(data.query.parameters)
                                    .map(function(val, key) {
                                        return [key.replace(/\[\]$/, ''), val];
                                    })
                                    .filter(function(pair) {
                                        return _.contains(keys, pair[0]);
                                    })
                                    .object()
                                    .value()

                                $extensionLi
                                    .on('savedQueryLoaded', function loaded() {
                                        $extensionLi.off('savedQueryLoaded', loaded);
                                        fulfill();
                                    })
                                    .trigger('loadSavedQuery', newFilters);
                            }).timeout(delaySecondsBeforeTimeout * 1000, 'savedQueryLoaded not fired for extension that uses keys:' + keys);
                        })
                })
                .then(function() {
                    return self.setConceptFilter(data.query.parameters.conceptType);
                })
                .then(function() {
                    return Promise.resolve(filters).map(function(filter) {
                        return self.addFilterItem(filter, { hide: true });
                    }, { concurrency: 1 });
                })
                .done(function() {
                    self.disableNotify = false;
                    self.$node.find('.filter').show();
                    self.notifyOfFilters({ fromSavedSearch: true });
                });
        };

        this.teardownField = function(node) {
            var self = this,
                instanceInfo = flightRegistry.findInstanceInfoByNode(node[0]);
            if (instanceInfo && instanceInfo.length) {
                instanceInfo.forEach(function(info) {
                    delete self.propertyFilters[info.instance.attr.id];
                    self.notifyOfFilters();
                    info.instance.teardown();
                });
            }

            node.empty();
        };

        this.loadConcepts = function() {
            ConceptSelector.attachTo(this.select('conceptDropdownSelector'), {
                onlySearchable: true,
                defaultText: i18n('search.filters.all_concepts')
            })
        };

        this.loadPropertyFilters = function() {
            var self = this;

            this.ontologyPromise = this.dataRequest('ontology', 'properties')
                .then(function(properties) {
                    self.ontologyProperties = properties;
                    self.properties = _.reject(properties.list, function(property) {
                        return !_.isEmpty(property.dependentPropertyIris);
                    });
                    self.addFilterItem();
                })
        };

        this.addFilterItem = function(filter, options) {
            var self = this,
                $li = $('<li>').data('filterId', this.filterId++),
                attributes = filter ? {
                    property: this.ontologyProperties.byTitle[
                        filter.propertyId
                    ],
                    predicate: filter.predicate,
                    values: filter.values
                } : {
                    properties: this.filteredPropertiesList || this.properties,
                    supportsHistogram: this.attr.supportsHistogram
                },
                $newRow = this.$node.find('.newrow');

            if (filter) {
                $li.addClass('filter')
                    .toggle(!options || !options.hide)

                if ($newRow.length) {
                    $li.insertBefore($newRow);
                } else {
                    $li.appendTo(this.$node.find('.prop-filters'));
                }
            } else {
                $li.addClass('filter newrow')
                    .appendTo(this.$node.find('.prop-filters'));
            }

            return new Promise(function(fulfill) {
                self.on($li, 'fieldRendered', function rendered() {
                    self.off($li, 'fieldRendered', rendered);
                    fulfill();
                });
                FilterItem.attachTo($li, attributes);
            })
        }
    }
});

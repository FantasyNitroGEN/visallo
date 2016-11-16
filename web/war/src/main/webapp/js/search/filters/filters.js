
define([
    'flight/lib/component',
    'flight/lib/registry',
    'hbs!./filtersTpl',
    './filterItem',
    'tpl!./entityItem',
    'search/sort',
    'util/vertex/formatters',
    'util/ontology/conceptSelect',
    'util/ontology/relationshipSelect',
    'util/withDataRequest',
    'configuration/plugins/registry'
], function(
    defineComponent,
    flightRegistry,
    template,
    FilterItem,
    entityItemTemplate,
    SortFilter,
    F,
    ConceptSelector,
    RelationshipSelector,
    withDataRequest,
    registry) {
    'use strict';

    var FILTER_SEARCH_DELAY_SECONDS = 0.5;

    return defineComponent(Filters, withDataRequest);

    function Filters() {
        this.propertyFilters = {};
        this.otherFilters = {};
        this.filterId = 0;

        this.attributes({
            removeEntityRowSelector: '.entity-filters button.remove',
            removeExtensionRowSelector: '.extension-filters button.remove',
            matchTypeSelector: '.match-types input',
            extensionsSelector: '.extension-filters',
            filterItemsSelector: '.prop-filters .filter',
            conceptDropdownSelector: '.concepts-dropdown',
            edgeLabelDropdownSelector: '.edgetype-dropdown',
            sortContentSelector: '.sort-content',
            conceptFilterSelector: '.concepts-dropdown,.concept-filter-header',
            edgeLabelFilterSelector: '.edgetype-dropdown,.edgetype-filter-header',
            match: 'vertex',
            supportsMatch: true,
            supportsSorting: true,
            supportsHistogram: null,
            searchType: null
        });

        this.after('initialize', function() {
            var self = this;

            this.throttledNotifyOfFilters = _.throttle(this.notifyOfFilters.bind(this), 100);
            this.notifyOfFilters = _.debounce(this.notifyOfFilters.bind(this), FILTER_SEARCH_DELAY_SECONDS * 1000);

            this.matchType = this.attr.match;
            this.$node.html(template({
                showMatchType: this.attr.supportsMatch !== false,
                showConceptFilter: this.attr.match === 'vertex',
                showEdgeFilter: this.attr.match === 'edge',
                showSorting: this.attr.supportsSorting
            }));

            this.on('filterItemChanged', this.onFilterItemChanged);

            this.on('searchByParameters', this.onSearchByParameters);
            this.on('clearfilters', this.onClearFilters);
            this.on('sortFieldsUpdated', this.onSortFieldsUpdated);
            this.on('enableMatchSelection', this.onEnableMatchSelection);
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
            this.on('change', {
                matchTypeSelector: this.onChangeMatchType
            })
            this.on('conceptSelected', this.onConceptChange);
            this.on('relationshipSelected', this.onEdgeTypeChange);
            this.on('searchByRelatedEntity', this.onSearchByRelatedEntity);
            this.on('searchByProperty', this.onSearchByProperty);
            this.on('filterExtensionChanged', this.onFilterExtensionChanged);

            this.requestPropertiesByDomainType = function() {
                return this.dataRequest('ontology', 'propertiesByDomainType', this.matchType);
            };

            Promise.resolve(this.addSearchFilterExtensions())
                .then(function() {
                    return self.loadPropertyFilters();
                })
                .done(function() {
                    self.loadConcepts();
                    self.loadEdgeTypes();
                    self.loadSorting();
                    self.trigger('filtersLoaded');
                });
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
            var self = this,
                options = data && data.options,
                newFilters = _.omit(data, 'options');

            this.disableMatchEdges = data.options && data.options.disableMatchEdges === true;
            this.clearFilters({ triggerUpdates: false }).done(function() {
                _.extend(self.otherFilters, newFilters);
                $(event.target).closest('.extension-filter-row').show();
                self.notifyOfFilters();
            })
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

            this.disableNotify = true;
            Promise.resolve(this.clearFilters({ triggerUpdates: false }))
                .then(this.setConceptFilter.bind(this, data.conceptId))
                .then(this.setEdgeTypeFilter.bind(this, data.edgeLabel))
                .then(this.setRelatedToEntityFilter.bind(this, data.vertexIds))
                .then(function() {
                    self.disableNotify = false;
                    self.notifyOfFilters();
                })
                .done();
        };

        this.setRelatedToEntityFilter = function(vertexIds) {
            var self = this;

            return this.dataRequest('vertex', 'store', { vertexIds: vertexIds })
                .then(function(vertices) {
                    var single = vertices[0],
                        title = vertices.length > 1 ? i18n('search.filters.title_multiple', vertices.length)
                                                    : single && F.vertex.title(single) || single.id;

                    self.select('edgeLabelFilterSelector').show();
                    self.otherFilters.relatedToVertexIds = _.pluck(vertices, 'id');
                    self.$node.find('.entity-filters')
                        .append(entityItemTemplate({title: title})).show();
                    self.notifyOfFilters();
                });
        };

        this.setSort = function(sortFields, options) {
            this.currentSort = sortFields || [];
            if (!options || options.propagate !== false) {
                this.select('sortContentSelector').trigger('setSortFields', {
                    sortFields: sortFields
                });
            }
            this.notifyOfFilters();
        };

        this.setEdgeTypeFilter = function(edgeId) {
            var self = this;

            if (this.matchType === 'vertex') {
                return;
            }

            this.edgeLabelFilter = edgeId || '';
            this.trigger(this.select('edgeLabelDropdownSelector'), 'selectRelationshipId', { relationshipId: edgeId });

            if (this.edgeLabelFilter) {
                return this.dataRequest('ontology', 'propertiesByRelationship', this.edgeLabelFilter)
                    .then(function(properties) {
                        self.filteredPropertiesList = _.reject(properties && properties.list || [], function(property) {
                            return !_.isEmpty(property.dependentPropertyIris);
                        });
                        self.select('filterItemsSelector')
                            .add(self.select('sortContentSelector'))
                            .trigger('filterProperties', {
                                properties: self.filteredPropertiesList
                            })
                        self.notifyOfFilters();
                    })
            } else {
                this.filteredPropertiesList = null;
                this.select('filterItemsSelector')
                    .add(self.select('sortContentSelector'))
                    .trigger('filterProperties', {
                        properties: this.propertiesByDomainType[this.matchType]
                    })
                this.notifyOfFilters();
            }
        };

        this.setMatchType = function(type) {
            var self = this;

            this.matchType = type;
            this.$node.find('.match-type-' + type).prop('checked', true);
            this.$node.find('.match-type-edge').closest('label').andSelf()
                .prop('disabled', this.disableMatchEdges === true);
            this.select('conceptFilterSelector').toggle(type === 'vertex');
            this.select('edgeLabelFilterSelector').toggle(type === 'edge' || self.otherFilters.relatedToVertexIds);
            if (this.matchType === 'vertex') {
                this.setConceptFilter(this.conceptFilter);
            } else {
                this.setEdgeTypeFilter(this.edgeLabelFilter);
            }
            this.select('filterItemsSelector').each(function() {
                var $li = $(this);
                    $li.teardownAllComponents();
                    $li.remove();
            });
            this.setSort();
            Promise.resolve(!this.propertiesByDomainType[type] ? this.requestPropertiesByDomainType() : [])
                .then(function(result) {
                    if (result.length) {
                        self.propertiesByDomainType[type] = result;
                    }
                    self.select('sortContentSelector').trigger('filterProperties', {
                        properties: self.propertiesByDomainType[type]
                    });
                    self.createNewRowIfNeeded();
                    self.notifyOfFilters();
                });
        };

        this.setConceptFilter = function(conceptId) {
            var self = this;

            if (this.matchType === 'edge') {
                return;
            }

            this.conceptFilter = conceptId || '';
            this.trigger(this.select('conceptDropdownSelector'), 'selectConceptId', { conceptId: conceptId });

            if (this.conceptFilter) {
                return this.dataRequest('ontology', 'propertiesByConceptId', this.conceptFilter)
                    .then(function(properties) {
                        self.filteredPropertiesList = _.reject(properties && properties.list || [], function(property) {
                            return !_.isEmpty(property.dependentPropertyIris);
                        });
                        self.select('filterItemsSelector')
                            .add(self.select('sortContentSelector'))
                            .trigger('filterProperties', {
                                properties: self.filteredPropertiesList
                            })
                        self.notifyOfFilters();
                    })
            } else {
                this.filteredPropertiesList = null;
                this.select('filterItemsSelector')
                    .add(self.select('sortContentSelector'))
                    .trigger('filterProperties', {
                        properties: this.propertiesByDomainType[this.matchType]
                    })
                this.notifyOfFilters();
            }
        };

        this.onChangeMatchType = function(event, data) {
            this.setMatchType($(event.target).val());
        };

        this.onConceptChange = function(event, data) {
            this.setConceptFilter(data.concept && data.concept.id || '');
        };

        this.onEdgeTypeChange = function(event, data) {
            this.setEdgeTypeFilter(data.relationship && data.relationship.title || '');
        };

        this.onSortFieldsUpdated = function(event, data) {
            this.setSort(data.sortFields, { propagate: false });
        };

        this.onEnableMatchSelection = function(event, data) {
            this.$node.find('.search-options').toggle(data.match === true);
            if (data.match) {
                this.setMatchType(this.matchType);
            }
        };

        this.onClearFilters = function(event, data) {
            var self = this;
            this.clearFilters().done(function() {
                self.trigger('filtersCleared');
            })
        };

        this.clearFilters = function(options) {
            var self = this,
                filterItems = this.$node.find('.prop-filters .filter');

            filterItems.teardownAllComponents();

            this.disableNotify = true;

            return Promise.resolve(this.setConceptFilter())
                .then(this.setMatchType.bind(this, 'vertex'))
                .then(this.setEdgeTypeFilter.bind(this))
                .then(this.setSort.bind(this))
                .then(this.createNewRowIfNeeded.bind(this))
                .then(function() {
                    self.disableMatchEdges = false;
                    self.otherFilters = {};
                    self.$node.find('.entity-filters').hide().empty();
                    self.$node.find('.extension-filter-row').hide();
                    self.disableNotify = false;
                    if (!options || options.triggerUpdates !== false) {
                        self.notifyOfFilters();
                    }
                });
        };

        this.hasSomeFilters = function(filters) {
            return !!(filters &&
                (!_.isEmpty(filters.conceptFilter) && this.matchType === 'vertex') ||
                (!_.isEmpty(filters.edgeLabelFilter) && this.matchType === 'edge') ||
                !_.isEmpty(filters.propertyFilters) ||
                !_.isEmpty(filters.otherFilters) ||
                !_.isEmpty(this.currentSort)
             );
        }

        this.notifyOfFilters = function(options) {
            var self = this;

            if (this.disableNotify) return;

            var filters = {
                    otherFilters: this.otherFilters,
                    conceptFilter: this.conceptFilter,
                    edgeLabelFilter: this.edgeLabelFilter,
                    sortFields: this.currentSort,
                    matchType: this.matchType,
                    propertyFilters: _.chain(this.propertyFilters)
                        .map(function(filter) {
                            var ontologyProperty = self.propertiesByDomainType[self.matchType].find(function(property) {
                                return property.title === filter.propertyId;
                            });

                            if (ontologyProperty && ontologyProperty.dependentPropertyIris) {
                                return ontologyProperty.dependentPropertyIris.map(function(iri, i) {
                                    if (_.isArray(filter.values[i]) && _.reject(filter.values[i], function(v) {
                                        return v === null || v === undefined;
                                    }).length) {
                                        return {
                                            propertyId: iri,
                                            predicate: filter.predicate,
                                            values: filter.values[i],
                                            metadata: filter.metadata
                                        }
                                    }
                                });
                            }

                            return {
                                propertyId: filter.propertyId,
                                predicate: filter.predicate,
                                values: filter.values,
                                metadata: filter.metadata
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
            this.disableMatchEdges = false;
            this.setMatchType(this.matchType);
        };

        this.createNewRowIfNeeded = function() {
            if (!this.propertiesByDomainType[this.matchType]) {
                return;
            }
            if (this.$node.find('.newrow').length === 0) {
                return this.addFilterItem();
            }
        };

        this.onSearchByParameters = function(event, data) {
            var self = this,
                filters = JSON.parse(data.parameters.filter);

            this.disableNotify = true;
            Promise.resolve(this.clearFilters())
                .then(function() {
                    if (data.parameters['relatedToVertexIds[]']) {
                        return self.setRelatedToEntityFilter(data.parameters['relatedToVertexIds[]']);
                    }
                })
                .then(function() {
                    var matching = self.$node.find('.extension-filter-row').filter(function() {
                        var keys = $(this).data('filterKey');
                        if (!_.isArray(keys)) {
                            keys = [keys];
                        }
                        if (_.some(data.parameters, function(val, key) {
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
                                var newFilters = _.chain(data.parameters)
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
                    return self.setMatchType((/edge/).test(data.url) ? 'edge' : 'vertex');
                })
                .then(function() {
                    return self.setConceptFilter(data.parameters.conceptType);
                })
                .then(function() {
                    return self.setEdgeTypeFilter(data.parameters.edgeLabel);
                })
                .then(function() {
                    var sortRaw = data.parameters['sort[]'],
                        sort;
                    if (sortRaw) {
                        sort = _.chain(sortRaw)
                            .map(function(sortStr) {
                                var match = sortStr.match(/^(.*):(ASCENDING|DESCENDING)$/);
                                if (match) {
                                    return {
                                        field: match[1],
                                        direction: match[2]
                                    }
                                }
                            })
                            .compact()
                            .value();
                    }
                    return self.setSort(sort);
                })
                .then(function() {
                    return Promise.resolve(filters).map(function(filter) {
                        return self.addFilterItem(filter, { hide: true });
                    }, { concurrency: 1 });
                })
                .done(function() {
                    self.disableNotify = false;
                    self.$node.find('.filter').show();
                    self.notifyOfFilters({ submit: data.submit === true });
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

        this.loadSorting = function() {
            if (!this.attr.supportsSorting) return;
            SortFilter.attachTo(this.select('sortContentSelector'));
        };

        this.loadEdgeTypes = function() {
            RelationshipSelector.attachTo(this.select('edgeLabelDropdownSelector'), {
                defaultText: i18n('search.filters.all_edgetypes')
            });
        };

        this.loadConcepts = function() {
            ConceptSelector.attachTo(this.select('conceptDropdownSelector'), {
                onlySearchable: true,
                defaultText: i18n('search.filters.all_concepts')
            })
        };

        this.loadPropertyFilters = function() {
            var self = this;

            if (!_.isObject(this.propertiesByDomainType)) {
                this.propertiesByDomainType = {};
            }

            this.ontologyPromise = this.dataRequest('ontology', 'propertiesByDomainType', this.matchType)
                .then(function(properties) {
                    self.propertiesByDomainType[self.matchType] = properties;

                    return self.addFilterItem();
                });

            return this.ontologyPromise;
        };

        this.addFilterItem = function(filter, options) {
            var self = this,
                $li = $('<li>').data('filterId', this.filterId++),
                attributes = filter ? {
                    property: this.propertiesByDomainType[this.matchType].find(function(property) {
                        return property.title === filter.propertyId;
                    }),
                    predicate: filter.predicate,
                    values: filter.values
                } : {
                    properties: this.filteredPropertiesList || this.propertiesByDomainType[this.matchType],
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
                self.createNewRowIfNeeded();
            })
        }
    }
});

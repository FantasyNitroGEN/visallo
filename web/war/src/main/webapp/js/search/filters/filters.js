
define([
    'flight/lib/component',
    'flight/lib/registry',
    'hbs!./filtersTpl',
    'tpl!./item',
    'tpl!./entityItem',
    'util/vertex/formatters',
    'util/ontology/conceptSelect',
    'util/withDataRequest',
    'fields/selection/selection',
    'configuration/plugins/registry'
], function(
    defineComponent,
    flightRegistry,
    template,
    itemTemplate,
    entityItemTemplate,
    F,
    ConceptSelector,
    withDataRequest,
    FieldSelection,
    registry) {
    'use strict';

    var FILTER_SEARCH_DELAY_SECONDS = 0.5;

    return defineComponent(Filters, withDataRequest);

    function Filters() {
        this.propertyFilters = {};
        this.otherFilters = {};
        this.filterId = 0;

        this.defaultAttrs({
            fieldSelectionSelector: '.newrow .add-property',
            removeEntityRowSelector: '.entity-filters button.remove',
            removeExtensionRowSelector: '.extension-filters button.remove',
            extensionsSelector: '.extension-filters',
            removeRowSelector: '.prop-filters button.remove',
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

            this.on('propertychange', this.onPropertyFieldItemChanged);
            this.on('propertyselected', this.onPropertySelected);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('clearfilters', this.onClearFilters);
            this.on('click', {
                removeEntityRowSelector: this.onRemoveEntityRow,
                removeExtensionRowSelector: this.onRemoveExtensionRow,
                removeRowSelector: this.onRemoveRow
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
                                    changedEventName: 'filterExtensionChanged'
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

            if (options && options.clearOtherFilters) {
                this.onClearFilters();
            }
            _.extend(this.otherFilters, newFilters);
            $(event.target).closest('.extension-filter-row').show();
            this.notifyOfFilters();
        };

        this.onSearchByProperty = function(event, data) {
            var self = this;

            event.stopPropagation();

            this.onClearFilters();

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    var properties = data.properties || _.compact([data.property]);

                    Promise.resolve(properties)
                        .each(function(property) {
                            return self.addPropertyRow({
                                property: ontologyProperties.byTitle[property.name],
                                value: property.value,
                                values: property.values,
                                predicate: property.predicate
                            });
                        })
                        .done(function() {
                            self.conceptFilter = data.conceptId || '';
                            self.trigger(self.select('conceptDropdownSelector'), 'selectConceptId', {
                                conceptId: data.conceptId || ''
                            });
                            self.notifyOfFilters();
                        });
                });
        };

        this.onSearchByRelatedEntity = function(event, data) {
            event.stopPropagation();
            var self = this;
            this.dataRequest('vertex', 'store', { vertexIds: data.vertexIds })
                .done(function(vertices) {
                    var single = vertices[0],
                        title = vertices.length > 1 ? i18n('search.filters.title_multiple', vertices.length)
                                                    : single && F.vertex.title(single) || single.id;
                    self.onClearFilters();

                    self.otherFilters.relatedToVertexIds = _.pluck(vertices, 'id');
                    self.conceptFilter = data.conceptId || '';
                    self.trigger(self.select('conceptDropdownSelector'), 'selectConceptId', {
                        conceptId: data.conceptId || ''
                    });
                    self.$node.find('.entity-filters')
                        .append(entityItemTemplate({title: title})).show();
                    self.notifyOfFilters();
                });
        };

        this.onConceptChange = function(event, data) {
            var self = this,
                deferred = $.Deferred().done(function(properties) {
                    self.filteredPropertiesList = properties && properties.list;
                    self.select('fieldSelectionSelector').each(function() {
                        self.trigger(this, 'filterProperties', {
                            properties: properties && properties.list
                        });
                    });
                });

            this.conceptFilter = data.concept && data.concept.id || '';

            // Publish change to filter properties typeaheads
            if (this.conceptFilter) {
                this.dataRequest('ontology', 'propertiesByConceptId', this.conceptFilter)
                    .done(deferred.resolve);
            } else {
                deferred.resolve();
            }

            this.notifyOfFilters();
        };

        this.onClearFilters = function(event, data) {
            var self = this,
                nodes = this.$node.find('.configuration').filter(function() {
                    return $(this).closest('.extension-filters').length === 0;
                });

            nodes.each(function() {
                self.teardownField($(this));
            }).closest('li:not(.newrow)').remove();

            this.trigger(this.select('conceptDropdownSelector'), 'clearSelectedConcept');
            this.conceptFilter = '';

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
            var ontologyProperties = this.ontologyProperties,
                filters = {
                    otherFilters: this.otherFilters,
                    conceptFilter: this.conceptFilter,
                    propertyFilters: _.chain(this.propertyFilters)
                        .map(function(filter) {
                            var ontologyProperty = ontologyProperties.byTitle[filter.propertyId];
                            if (ontologyProperty && ontologyProperty.dependentPropertyIris) {
                                return ontologyProperty.dependentPropertyIris.map(function(iri, i) {
                                    if (!_.isUndefined(filter.values[i])) {
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

        this.onRemoveEntityRow = function(event, data) {
            var target = $(event.target),
                row = target.closest('.entity-filter-row'),
                section = row.closest('.entity-filters'),
                key = row.data('filterKey');

            row.remove();
            section.hide();
            delete this.otherFilters[key];
            this.notifyOfFilters();
        };

        this.onRemoveExtensionRow = function(event, data) {
            var self = this,
                target = $(event.target),
                row = target.closest('.extension-filter-row'),
                section = row.closest('.extension-filters'),
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

        this.onRemoveRow = function(event, data) {
            var target = $(event.target);
            this.teardownField(target.next('.configuration'));
            target.closest('li').remove();
            this.createNewRowIfNeeded();
        };

        this.onPropertySelected = function(event, data) {
            this.addPropertyRow(data, event.target);
        };

        this.addPropertyRow = function(data, optionalRow) {
            var self = this,
                $dropdown = this.$node.find('.newrow .dropdown input'),
                target = optionalRow && $(optionalRow) || $dropdown,
                li = target.closest('li').addClass('fId' + self.filterId),
                property = data.property,
                isCompoundField = property.dependentPropertyIris && property.dependentPropertyIris.length,
                fieldComponent;

            if (property.title === 'http://visallo.org#text') {
                property.dataType = 'boolean';
            }

            if (('value' in data) || ('values' in data)) {
                $dropdown.val(property.displayName);
            }

            if (isCompoundField) {
                fieldComponent = 'fields/compound/compound';
            } else if (property.displayType === 'duration') {
                fieldComponent = 'fields/duration';
            } else {
                fieldComponent = property.possibleValues ? 'fields/restrictValues' : 'fields/' + property.dataType;
            }

            return Promise.require(fieldComponent).then(function(PropertyFieldItem) {
                var node = li.find('.configuration').removeClass('alternate');

                self.teardownField(node);

                if (isCompoundField) {
                    PropertyFieldItem.attachTo(node, {
                        id: self.filterId++,
                        property: property,
                        vertex: null,
                        predicates: true,
                        predicateType: data.predicate,
                        values: data.values,
                        supportsHistogram: false
                    });
                } else {
                    PropertyFieldItem.attachTo(node, {
                        property: property,
                        id: self.filterId++,
                        predicates: true,
                        predicateType: data.predicate,
                        value: data.value,
                        supportsHistogram: self.attr.supportsHistogram
                    });
                }

                li.removeClass('newrow');

                self.createNewRowIfNeeded();
            });
        };

        this.onPropertyInvalid = function(event, data) {
            var li = this.$node.find('li.fId' + data.id);
            li.addClass('invalid');

            delete this.propertyFilters[data.id];
            this.notifyOfFilters();
        };

        this.createFieldSelection = function() {
            FieldSelection.attachTo(this.select('fieldSelectionSelector'), {
                properties: this.filteredPropertiesList || this.properties,
                unsupportedProperties: this.attr.supportsHistogram ?
                    [] :
                    ['http://visallo.org#text'],
                onlySearchable: true,
                placeholder: i18n('search.filters.add_filter.placeholder')
            });
        }

        this.createNewRowIfNeeded = function() {
            if (!this.properties) {
                return;
            }
            if (this.$node.find('.newrow').length === 0) {
                this.$node.find('.prop-filters').append(itemTemplate({properties: this.properties}));
                this.createFieldSelection();
            }
        };

        this.onPropertyFieldItemChanged = function(event, data) {
            this.$node.find('li.fId' + data.id).removeClass('invalid');
            this.propertyFilters[data.id] = data;
            if (data && data.options && data.options.isScrubbing === true) {
                this.throttledNotifyOfFilters(data.options);
            } else {
                this.notifyOfFilters();
            }
            event.stopPropagation();
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

            this.dataRequest('ontology', 'properties')
                .done(function(properties) {
                    self.ontologyProperties = properties;
                    self.properties = properties.list;
                    self.$node.find('.prop-filter-header').after(itemTemplate({}));
                    self.createFieldSelection();
                })
        };
    }
});

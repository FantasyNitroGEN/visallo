
define([
    'flight/lib/component',
    '../dropdowns/propertyForm/propForm',
    'util/vertex/formatters',
    'util/privileges',
    'util/withDataRequest',
    'util/popovers/propertyInfo/withPropertyInfo',
    'd3'
], function(
    defineComponent,
    PropertyForm,
    F,
    Privileges,
    withDataRequest,
    withPropertyInfo,
    d3) {
    'use strict';

    var component = defineComponent(Properties, withDataRequest, withPropertyInfo),
        HIDE_PROPERTIES = ['http://visallo.org/comment#entry'],
        VISIBILITY_NAME = 'http://visallo.org#visibilityJson',
        SANDBOX_STATUS_NAME = 'http://visallo.org#sandboxStatus',
        RELATIONSHIP_LABEL = 'http://visallo.org#relationshipLabel',
        NO_GROUP = '${NO_GROUP}',

        // Property td types
        GROUP = 0, NAME = 1, VALUE = 2, HIDDEN_COLLAPSE = 3,

        alreadyWarnedAboutMissingOntology = {};

    return component;

    function isVisibility(property) {
        return property.name === VISIBILITY_NAME;
    }

    function isSandboxStatus(property) {
        return property.name === SANDBOX_STATUS_NAME;
    }

    function isRelationshipLabel(property) {
        return property.name === RELATIONSHIP_LABEL;
    }

    function isJustification(property) {
        return (
            property.name === 'http://visallo.org#justification' ||
            property.name === '_sourceMetadata'
        );
    }

    function Properties() {

        this.defaultAttrs({
            propertiesInfoSelector: 'button.info'
        });

        this.update = function(properties) {
            var self = this,
                displayProperties = this.transformPropertiesForUpdate(properties),
                expandedSections = visalloData.currentUser.uiPreferences['property-groups-expanded'];

            if (expandedSections) {
                expandedSections = JSON.parse(expandedSections);
            } else {
                expandedSections = [];
            }

            this.reload = this.update.bind(this, properties);

            this.tableRoot.selectAll('tbody.property-group')
                .data(displayProperties)
                .call(
                    _.partial(
                        createPropertyGroups,
                        self.data,
                        self.ontologyProperties,
                        self.showMoreExpanded,
                        parseInt(self.config['properties.multivalue.defaultVisibleCount'], 10),
                        expandedSections,
                        self.config
                    )
                );
        };

        this.transformPropertiesForUpdate = function(properties) {
            var self = this,
                model = self.data,
                isEdge = F.vertex.isEdge(model),
                dependentPropertyIris = this.dependentPropertyIris,
                compoundPropertiesByNameToKeys = {},
                displayProperties = _.chain(properties)
                    .filter(function(property) {
                        if (isJustification(property)) {
                            return false;
                        }

                        if (isVisibility(property)) {
                            return true;
                        }

                        if (~HIDE_PROPERTIES.indexOf(property.name)) {
                            return false;
                        }

                        if (property.streamingPropertyValue) {
                            return false;
                        }

                        // Remove dependent properties from list since they
                        // rollup into compound properties
                        if (~dependentPropertyIris.indexOf(property.name)) {
                            var compoundProperty = self.propertyIriToCompoundProperty[property.name],
                                compoundKey = compoundProperty.title;

                            if (!compoundPropertiesByNameToKeys[compoundKey]) {
                                compoundPropertiesByNameToKeys[compoundKey] = {
                                    property: compoundProperty,
                                    keys: {}
                                };
                            }
                            compoundPropertiesByNameToKeys[compoundKey].keys[property.key] = true;
                            return false;
                        }

                        var ontologyProperty = self.ontologyProperties.byTitle[property.name];
                        return ontologyProperty && ontologyProperty.userVisible;
                    })
                    .tap(function(properties) {
                        var visibility = _.find(properties, isVisibility);
                        if (!visibility) {
                            properties.push({
                                name: VISIBILITY_NAME,
                                value: self.data[VISIBILITY_NAME]
                            });
                        }

                        var sandboxStatus = F.vertex.sandboxStatus(self.data);
                        if (sandboxStatus) {
                            properties.push({
                                name: SANDBOX_STATUS_NAME,
                                displayName: i18n('detail.entity.sandboxStatus'),
                                value: sandboxStatus,
                                hideInfo: true,
                                hideVisibility: true
                            });
                        }

                        // Add compound properties (multi-value if there
                        // dependent properties have multiple keys
                        _.each(compoundPropertiesByNameToKeys, function(compoundInfo, compoundKey) {
                            _.each(compoundInfo.keys, function(value, key) {
                                var matching = F.vertex.props(self.data, compoundInfo.property.title, key),
                                    first = matching && _.first(matching),
                                    property = {
                                        compoundProperty: true,
                                        name: compoundInfo.property.title,
                                        key: key,
                                        values: matching || []
                                    };

                                if (first) {
                                    property.metadata = first.metadata;
                                }

                                properties.push(property);
                            });
                        });
                    })
                    .sortBy(function(property) {
                        var value = F.vertex.prop(model, property.name, property.key);
                        if (_.isString(value)) {
                            return value.toLowerCase();
                        }
                        return 0;
                    })
                    .sortBy(function(property) {
                        if (property.metadata && ('http://visallo.org#confidence' in property.metadata)) {
                            return property.metadata['http://visallo.org#confidence'] * -1;
                        }
                        return 0;
                    })
                    .sortBy(function(property) {
                        if (isVisibility(property)) {
                            return '0';
                        }

                        if (isSandboxStatus(property)) {
                            return '1';
                        }

                        if (isEdge) {
                            return property.name === RELATIONSHIP_LABEL ?
                                '2' :
                                isJustification(property) ?
                                '3' : '4';
                        }

                        var ontologyProperty = self.ontologyProperties.byTitle[property.name];
                        if (ontologyProperty && ontologyProperty.propertyGroup) {
                            return '4' + ontologyProperty.propertyGroup.toLowerCase() + ontologyProperty.displayName;
                        }
                        if (ontologyProperty && ontologyProperty.displayName) {
                            return '2' + ontologyProperty.displayName.toLowerCase();
                        }
                        return '3' + property.name.toLowerCase();
                    })
                    .groupBy('name')
                    .pairs()
                    .groupBy(function(pair) {
                        var ontologyProperty = self.ontologyProperties.byTitle[pair[0]];
                        if (ontologyProperty && ontologyProperty.propertyGroup) {
                            return ontologyProperty.propertyGroup;
                        }

                        return NO_GROUP;
                    })
                    .pairs()
                    .value();

            return displayProperties;
        };

        this.after('initialize', function() {
            this.data = this.attr.data;

            var self = this,
                properties = this.data.properties,
                node = this.node,
                root = d3.select(node);

            node.classList.add('org-visallo-properties');

            this.showMoreExpanded = {};
            this.tableRoot = root
                .append('table')
                .attr('class', 'table')
                .on('click', onTableClick.bind(this));

            var ontologyLoaded = Promise.all([
                this.dataRequest('ontology', 'ontology'),
                this.dataRequest('ontology', 'propertiesByConceptId', F.vertex.prop(self.data, 'conceptType')),
                this.dataRequest('config', 'properties')
            ]).then(function(results) {
                var ontology = results.shift(),
                    conceptProperties = _.keys(results.shift().byTitle),
                    config = results.shift(),
                    ontologyRelationships = ontology.relationships,
                    ontologyProperties = ontology.properties;

                self.config = config;
                self.ontologyProperties = ontologyProperties;

                self.dependentPropertyIris = [];
                self.propertyIriToCompoundProperty = {};

                self.ontologyProperties.list.forEach(function(p) {
                    if (p.dependentPropertyIris &&
                        _.contains(conceptProperties || [], p.title) &&
                        p.dependentPropertyIris.length) {
                        p.dependentPropertyIris.forEach(function(iri) {
                            self.propertyIriToCompoundProperty[iri] = p;
                            self.dependentPropertyIris.push(iri);
                        })
                    }
                });
                self.dependentPropertyIris = _.uniq(self.dependentPropertyIris);

                self.ontologyRelationships = ontologyRelationships;
                self.update(properties);
            });

            this.on('addProperty', this.onAddProperty);
            this.on('deleteProperty', this.onDeleteProperty);
            this.on('editProperty', this.onEditProperty);
            this.on('updateModel', function(event, data) {
                self.data = data.model;
                ontologyLoaded.done(function() {
                    self.update(data.model.properties)
                })
            });

            var positionPopovers = _.throttle(function() {
                    self.trigger('positionPropertyInfo');
                }, 1000 / 60),
                scrollParent = this.$node.scrollParent();

            this.on(document, 'graphPaddingUpdated', positionPopovers);
            if (scrollParent.length) {
                this.on(scrollParent, 'scroll', positionPopovers);
            }
        });

        this.onDeleteProperty = function(event, data) {
            var self = this,
                vertexId = data.vertexId || this.data.id;

            this.dataRequest(
                    F.vertex.isEdge(this.data) ? 'edge' : 'vertex',
                    'deleteProperty',
                    vertexId, data.property
                ).then(this.closePropertyForm.bind(this, data.node))
                 .catch(function(error) { self.requestFailure.call(self, error, data.node) })
        };

        this.onAddProperty = function(event, data) {
            var self = this,
                vertexId = data.vertexId || this.data.id,
                service = data.isEdge ? 'edge' : 'vertex';

            if (data.property.name === 'http://visallo.org#visibilityJson') {
                var visibilitySource = data.property.visibilitySource || '';
                this.dataRequest(service, 'setVisibility', vertexId, visibilitySource)
                    .then(this.closePropertyForm.bind(this, data.node))
                    .catch(function(error) { self.requestFailure.call(self, error, data.node) })
            } else if (this.isStreamingPropertyVisibilityUpdate(data)) {
                this.dataRequest(service, 'setPropertyVisibility', vertexId, data.property)
                    .then(this.closePropertyForm.bind(this, data.node))
                    .catch(function(error) { self.requestFailure.call(self, error, data.node) });
            } else {
                this.dataRequest(service, 'setProperty', vertexId, data.property)
                    .then(this.closePropertyForm.bind(this, data.node))
                    .catch(function(error) { self.requestFailure.call(self, error, data.node) });
            }

        };

        this.isStreamingPropertyVisibilityUpdate = function(data) {
            var prop = _.first(F.vertex.props(this.data, data.property.name, data.property.key));
            return prop && prop.streamingPropertyValue;
        };

        this.closePropertyForm = function(node) {
            $(node).teardownComponent(PropertyForm);
        };

        this.requestFailure = function(error, node) {
            var target = $(node);
            this.trigger(target, 'propertyerror', { error: error });
        };

        this.onEditProperty = function(evt, data) {
            var root = $('<div class="underneath">'),
                property = data && data.property,
                propertyRow = property && $(evt.target).closest('tr')

            this.$node.find('button.info').popover('hide');

            if (propertyRow && propertyRow.length) {
                root.appendTo(
                    $('<tr><td colspan=3></td></tr>')
                        .insertAfter(propertyRow)
                        .find('td')
                );
            } else {
                $('<tr><td colspan="3"></td></tr>').prependTo(this.$node.find('table')).find('td').append(root);
            }

            PropertyForm.teardownAll();
            PropertyForm.attachTo(root, {
                data: this.data,
                property: property
            });
        };

        this.updateJustification = function() {
            this.$node.find('.justification').each(function() {
                var justification = $(this),
                    property = justification.data('property');

                require(['util/vertex/justification/viewer'], function(JustificationViewer) {
                    var attrs = {};
                    attrs[property.name] = property.value;
                    JustificationViewer.attachTo(justification, attrs);
                });
            });
        }

        this.updateVisibility = function() {
            var self = this;

            require([
                'util/visibility/view'
            ], function(VisibilityDisplay) {
                self.$node.find('.visibility').each(function() {
                    var visibility = $(this).data('visibility');
                    VisibilityDisplay.attachTo(this, {
                        value: visibility && visibility.source
                    })
                });
            });
        };

        this.saveSectionTogglePreference = function(sectionEl, expanded) {
            var name = $(sectionEl).data('sectionName'),
                previouslyExpanded = visalloData.currentUser.uiPreferences['property-groups-expanded'],
                shouldSave = false;

            if (previouslyExpanded) {
                previouslyExpanded = JSON.parse(previouslyExpanded);
            } else {
                previouslyExpanded = [];
            }

            var inList = previouslyExpanded.indexOf(name) >= 0;

            if (expanded && !inList) {
                previouslyExpanded.push(name);
                shouldSave = true;
            } else if (!expanded && inList) {
                previouslyExpanded = _.without(previouslyExpanded, name);
                shouldSave = true;
            }

            if (shouldSave) {
                var prefValue = JSON.stringify(previouslyExpanded);
                visalloData.currentUser.uiPreferences['property-groups-expanded'] = prefValue;
                this.dataRequest('user', 'preference', 'property-groups-expanded', prefValue);
            }
        };
    }

    function onTableClick() {
        var $target = $(d3.event.target),
            $header = $target.closest('.property-group-header'),
            $tbody = $header.closest('.property-group'),
            processed = true;

        if ($header.is('.property-group-header')) {
            $tbody.toggleClass('collapsed expanded');
            this.saveSectionTogglePreference($tbody[0], !$tbody.hasClass('collapsed'));
        } else if ($target.is('.show-more')) {
            var isShowing = $target.data('showing');
            $target.data('showing', !isShowing);
            if (isShowing) {
                delete this.showMoreExpanded[$target.data('propertyName')];
            } else {
                this.showMoreExpanded[$target.data('propertyName')] = true;
            }
            this.reload();
        } else if ($target.is('.info')) {
            var datum = d3.select($target.closest('.property-value').get(0)).datum();
            this.showPropertyInfo($target, this.data, datum.property);
        } else {
            processed = false;
        }

        if (processed) {
            d3.event.stopPropagation();
            d3.event.preventDefault();
        }
    }

    function createPropertyGroups(vertex, ontologyProperties, showMoreExpanded, maxItemsBeforeHidden, expandedSections,
                                  config) {
        this.exit().remove();
        this.enter().insert('tbody', '.buttons-row');
        this.order();

        this.attr('class', function(d, groupIndex, j) {
            var cls = 'property-group collapsible';
            if (groupIndex === 0) {
                return cls + ' expanded';
            }

            return cls + ' ' + (
                _.contains(expandedSections, d[0]) ?
                    'expanded' : 'collapsed'
            );
        });

        this.attr('data-section-name', function(d) {
            return d[0];
        })

        var totalPropertyCountsByName = {};

        this.selectAll('tr.property-group-header, tr.property-row, tr.property-hidden')
            .data(function(pair) {
                return _.chain(pair[1])
                    .map(function(p) {
                        var hidden = p[1].length - maxItemsBeforeHidden;
                        totalPropertyCountsByName[p[0]] = hidden;
                        if (p[0] in showMoreExpanded) {
                            var expanded = p[1].slice(0);
                            expanded.splice(maxItemsBeforeHidden, 0, { name: p[0], hidden: hidden, isExpanded: true });
                            return expanded;
                        }
                        var truncated = p[1].slice(0, maxItemsBeforeHidden);
                        if (hidden > 0) {
                            truncated.push({ name: p[0], hidden: hidden, isExpanded: false });
                        }
                        return truncated;
                    })
                    .flatten()
                    .tap(function(list) {
                        if (pair[0] !== NO_GROUP) {
                            list.splice(0, 0, [pair[0], {
                                propertyCount: pair[1].length,
                                valueCount: _.reduce(pair[1], function(sum, p) {
                                    return sum + p[1].length;
                                }, 0)
                            }]);
                        }
                    })
                    .value();
            })
            .call(
                _.partial(createProperties,
                          vertex,
                          ontologyProperties,
                          totalPropertyCountsByName,
                          maxItemsBeforeHidden,
                          showMoreExpanded,
                          config
                )
            )
    }

    function createProperties(vertex,
                              ontologyProperties,
                              totalPropertyCountsByName,
                              maxItemsBeforeHidden,
                              showMoreExpanded,
                              config) {


        this.exit().remove();
        this.enter().append('tr')
        this.order();

        this.attr('class', function(datum) {
            if (_.isString(datum[0])) {
                return 'property-group-header';
            }
            if ('hidden' in datum) {
                return 'property-hidden';
            }
            return 'property-row property-row-' + F.className.to(datum.name + datum.key);
        });

        var currentPropertyIndex = 0,
            lastPropertyName = '';

        this.selectAll('td')
            .data(function(datum, i, j) {
                if (_.isString(datum[0])) {
                    return [{
                        type: GROUP,
                        name: datum[0],
                        count: datum[1]
                    }];
                }
                if ('hidden' in datum) {
                    return [
                        { type: HIDDEN_COLLAPSE, spacer: true },
                        { type: HIDDEN_COLLAPSE, name: datum.name, hidden: datum.hidden, isExpanded: datum.isExpanded }
                    ];
                }

                if (datum.name === lastPropertyName) {
                    currentPropertyIndex++;
                } else {
                    currentPropertyIndex = 0;
                    lastPropertyName = datum.name;
                }

                return [
                    {
                        type: NAME,
                        name: datum.name,
                        property: datum
                    },
                    {
                        type: VALUE,
                        property: datum,
                        propertyIndex: currentPropertyIndex,
                        showToggleLink: currentPropertyIndex === (maxItemsBeforeHidden - 1),
                        isExpanded: datum.name in showMoreExpanded,
                        hidden: Math.max(0, totalPropertyCountsByName[datum.name])
                    }
                ];
            })
            .call(_.partial(createPropertyRow, vertex, ontologyProperties, maxItemsBeforeHidden, config));
    }

    function createPropertyRow(vertex, ontologyProperties, maxItemsBeforeHidden, config) {
        this.exit().remove();
        this.each(function(datum) {
            var d3element = d3.select(this),
                resetUnless = function(selector) {
                    if (d3element.select(selector).size() === 0) {
                        d3element.text('');
                    }
                };
            switch (datum.type) {
                case HIDDEN_COLLAPSE: resetUnless('a.show-more'); break;
                case GROUP: resetUnless('.collapsible-header'); break;
                case NAME: resetUnless('strong'); break;
                case VALUE: resetUnless('.value-container'); break;
            }
        })
        this.enter().append('td')
        this.each(function(datum) {
            if (this.childElementCount > 0) return;
            var d3element = d3.select(this);
            switch (datum.type) {
                case HIDDEN_COLLAPSE:
                    if (!datum.spacer) {
                        d3element.append('a').attr('class', 'show-more');
                    }
                    break;
                case GROUP:
                    d3element.append('h1')
                        .attr('class', 'collapsible-header')
                        .call(function() {
                            this.append('span').attr('class', 'badge');
                            this.append('strong');
                        });
                        break;
                case NAME: d3element.append('strong'); break;
                case VALUE:
                    d3element.append('div').attr('class', 'value-container')
                        .call(function() {
                            this.append('span').attr('class', 'value');
                            this.append('button').attr('class', 'info');
                            this.append('span').attr('class', 'visibility');
                        })
                    break;
            }
        });

        this.order();

        this.attr('class', function(datum) {
                if (datum.type === NAME) {
                    return 'property-name';
                } else if (datum.type === VALUE) {
                    return 'property-value';
                } else if (datum.type === HIDDEN_COLLAPSE && !datum.spacer) {
                    return 'property-hidden-toggle'
                }
            })
            .attr('colspan', function(datum) {
                if (datum.type === GROUP) {
                    return '3';
                } else if (datum.type === VALUE || (datum.type === HIDDEN_COLLAPSE && !datum.spacer)) {
                    return '2';
                }
                return '1';
            })
            .call(function() {
                var previousPropertyName = '';

                this.select('h1.collapsible-header strong').text(_.property('name'))
                this.select('h1.collapsible-header .badge')
                    .text(function(d) {
                        return F.number.pretty(d.count.valueCount)
                    })
                    .attr('title', function(d) {
                        var propertyLabel = 'properties.groups.count.hover.property',
                            valueLabel = 'properties.groups.count.hover.value';

                        if (d.count.propertyCount > 1) {
                            propertyLabel += '.plural';
                        }
                        if (d.count.valueCount > 1) {
                            valueLabel += '.plural';
                        }

                        return [
                            F.number.pretty(d.count.propertyCount),
                            i18n(propertyLabel),
                            F.number.pretty(d.count.valueCount),
                            i18n(valueLabel)
                        ].join(' ')
                    });

                this.select('.property-name strong')
                    .text(function(d) {
                        if (previousPropertyName === d.name) {
                            return '';
                        }
                        previousPropertyName = d.name;

                        if (isVisibility(d)) {
                            return i18n('visibility.label');
                        }

                        if (d.property.displayName) {
                            return d.property.displayName;
                        }

                        var ontologyProperty = ontologyProperties.byTitle[d.name];
                        if (ontologyProperty) {
                            return ontologyProperty.displayName;
                        }

                        console.warn('No ontology definition for ', d.name);
                        return d.name;
                    });

                this.select('.property-value .value')
                    .each(function() {
                        var d3element = d3.select(this),
                            property = d3element.datum().property,
                            valueSpan = d3element.node(),
                            $valueSpan = $(valueSpan),
                            visibilitySpan = $valueSpan.siblings('.visibility')[0],
                            $infoButton = $valueSpan.siblings('.info'),
                            visibility = isVisibility(property),
                            ontologyProperty = ontologyProperties.byTitle[property.name],
                            dataType = ontologyProperty && ontologyProperty.dataType,
                            displayType = ontologyProperty && ontologyProperty.displayType;

                        valueSpan.textContent = '';
                        visibilitySpan.textContent = '';

                        if (visibility) {
                            dataType = 'visibility';
                        } else if (property.hideVisibility !== true) {
                            F.vertex.properties.visibility(
                                visibilitySpan,
                                { value: property.metadata && property.metadata[VISIBILITY_NAME] },
                                vertex);
                        }

                        var isEditable = (
                                Privileges.canEDIT &&
                                (property.updateable !== false || property.deleteable !== false)
                            ),
                            metadataPropertyNames = (config['properties.metadata.propertyNames'] || '').split(','),
                            hasVisibleMetadata = F.vertex.hasMetadata(property, metadataPropertyNames);

                        $infoButton.toggle(Boolean(
                            !property.hideInfo && (
                                isEditable ||
                                ontologyProperty.searchable ||
                                hasVisibleMetadata)
                        ));

                        if (displayType && F.vertex.properties[displayType]) {
                            F.vertex.properties[displayType](valueSpan, property, vertex);
                        } else if (dataType && F.vertex.properties[dataType]) {
                            F.vertex.properties[dataType](valueSpan, property, vertex);
                        } else if (isJustification(property)) {
                            require(['util/vertex/justification/viewer'], function(JustificationViewer) {
                                $(valueSpan).teardownAllComponents();
                                JustificationViewer.attachTo(valueSpan, property.justificationData);
                            });
                        } else if (isSandboxStatus(property) || isRelationshipLabel(property)) {
                            valueSpan.textContent = property.value;
                        } else {
                            valueSpan.textContent = F.vertex.prop(vertex, property.name, property.key);
                        }
                    });

                this.select('.show-more')
                    .attr('data-property-name', function(d) {
                        return d.name;
                    })
                    .text(function(d) {
                        return i18n(
                            'properties.button.' + (d.isExpanded ? 'hide_more' : 'show_more'),
                            F.number.pretty(d.hidden),
                            ontologyProperties.byTitle[d.name].displayName
                        );
                    })
                    .style('display', function(d) {
                        if (d.hidden > 0) {
                            return 'block';
                        }

                        return 'none';
                    });
            })
    }

});

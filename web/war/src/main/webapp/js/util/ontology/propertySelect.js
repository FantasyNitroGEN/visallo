/**
 * Allows a user to select an ontology property from a searchable dropdown component.
 *
 * @module components/PropertySelect
 * @flight Dropdown selection component for selecting properties from the ontology
 * @attr {Array.<object>} properties The ontology properties to populate the list with
 * @attr {string} [placeholder=Select Property] the placeholder text to display
 * @attr {boolean} [showAdminConcepts=false] Whether concepts that aren't user visible should be displayed
 * @attr {boolean} [onlySearchable=false] Only show properties that have searchable attribute equal to true in ontology
 * @attr {boolean} [rollupCompound=true] Hide all dependant properties and only show the compound/parent fields
 * @attr {boolean} [focus=false] Activate the field for focus when finished rendering
 * @attr {number} [maxItems=-1] Limit the maximum items that are shown in search list (-1 signifies no limit)
 * @attr {string} [selectedProperty=''] Default the selection to this property IRI
 * @attr {Array.<string>} [unsupportedProperties=[]] Remove these property IRIs from the list
 * @fires module:components/PropertySelect#propertyselected
 * @listens module:components/PropertySelect#filterProperties
 * @example
 * dataRequest('ontology', 'properties').then(function(properties) {
 *     PropertySelect.attachTo(node, {
 *         properties: properties
 *     })
 * })
 */
define([
    'flight/lib/component',
    './properties.hbs',
    'util/service/propertiesPromise',
    './withSelect'
], function(
    defineComponent,
    template,
    config,
    withSelect) {
    'use strict';

    var HIDE_PROPERTIES = ['http://visallo.org/comment#entry'];

    return defineComponent(PropertySelect, withSelect);

    function PropertySelect() {

        var PLACEHOLDER = i18n('field.selection.placeholder');

        this.defaultAttrs({
            findPropertySelection: 'input',
            showAdminProperties: false,
            rollupCompound: true,
            maxItems: withSelect.maxItemsFromConfiguration('typeahead.properties.maxItems')
        });

        this.after('initialize', function() {
            var self = this;

            /**
             * Trigger to change the list of properties the component works with.
             *
             * @event module:components/PropertySelect#filterProperties
             * @property {object} data
             * @property {Array.<object>} data.properties The properties array to use
             * @example
             * PropertySelect.attachTo($node)
             * //...
             * $node.trigger('filterProperties', { properties: newList })
             */
            this.on('filterProperties', this.onFilterProperties);

            if (this.attr.selectedProperty) {
                this.currentProperty = this.attr.selectedProperty;
            }

            this.$node.html(template({
                placeholder: this.attr.placeholder,
                selected: this.currentProperty && this.currentProperty.displayName || ''
            }));

            if (this.attr.properties.length === 0 || this.attr.properties.length.value === 0) {
                this.select('findPropertySelection')
                    .attr('placeholder', i18n('field.selection.no_valid'))
                    .attr('disabled', true);
            } else {
                this.queryPropertyMap = {};

                var field = this.select('findPropertySelection')
                    .on('focus', function(e) {
                        var target = $(e.target);
                        target.attr('placeholder', PLACEHOLDER)
                        self.focused = true;
                    })
                    .on('click', function(e) {
                        var target = $(e.target);

                        if (target.val()) {
                            target.typeahead('lookup').select();
                        } else {
                            target.typeahead('lookup');
                        }

                        target.attr('placeholder', PLACEHOLDER);
                    })
                    .on('change blur', function(e) {
                        var target = $(e.target);
                        if (self.currentProperty) {
                            target.val(self.currentProperty.displayName || self.currentProperty.title);
                        } else {
                            target.val('');
                        }
                        target.attr('placeholder', self.attr.placeholder);
                        if (e.type === 'blur') {
                            self.focused = false;
                        }
                    })
                    .typeahead({
                        minLength: 0,
                        items: this.attr.maxItems,
                        source: function() {
                            var sourceProperties = self.filterSourceProperties(self.propertiesForSource);

                            return _.map(sourceProperties, mapProperty);

                            function mapProperty(p) {
                                var name = displayName(p),
                                    duplicates = self.groupedByDisplay[name].length > 1;

                                self.queryPropertyMap[p.title] = p;

                                return JSON.stringify({
                                    displayName: name,
                                    title: p.title,
                                    propertyGroup: p.propertyGroup,
                                    duplicates: duplicates
                                });
                            }
                        },
                        matcher: function(itemJson) {
                            if (this.query === ' ') return -1;

                            var item = JSON.parse(itemJson);

                            if (
                                this.query &&
                                self.currentProperty &&
                                self.currentProperty.title === item.title) {
                                return 1;
                            }
                            return Object.getPrototypeOf(this).matcher.apply(this, [item.displayName]);
                        },
                        highlighter: function(itemJson) {
                            var item = JSON.parse(itemJson);
                            if (item.duplicates) {
                                return item.displayName +
                                    _.template('<div title="{title}" class="subtitle">{title}</div>')(item)
                            }
                            return item.displayName;
                        },
                        sorter: function(items) {
                            var query = this.query.toLowerCase();

                            return _.sortBy(items, function(json) {
                                var item = JSON.parse(json),
                                    displayName = item.displayName.toLowerCase(),
                                    group = item.propertyGroup;

                                if (query && displayName === query) {
                                    return '0';
                                }
                                if (group) {
                                    return '1' + group + displayName;
                                }
                                return '0' + displayName;
                            });
                        },
                        updater: function(itemJson) {
                            var item = JSON.parse(itemJson);
                            self.propertySelected(item);
                            return item;
                        }
                    }),
                    typeahead = field.data('typeahead');

                if (this.attr.focus) {
                    this.select('findPropertySelection').focus();
                }

                self.allowEmptyLookup(field);
                typeahead.render = function(items) {
                    var self = this,
                        $items = $(),
                        lastGroup;

                    items.forEach(function(item, i) {
                        var itemJson = JSON.parse(item);
                        if (itemJson.propertyGroup && lastGroup !== itemJson.propertyGroup) {
                            lastGroup = itemJson.propertyGroup;
                            $items = $items.add($('<li class="divider">'));
                            $items = $items.add($('<li class="nav-header">').text(itemJson.propertyGroup)[0]);
                        }

                        var $item = $(self.options.item).attr('data-value', item)
                            .toggleClass('active', i === 0)
                            .find('a').html(self.highlighter(item))
                            .end();
                        $items = $items.add($item);
                    })

                    this.$menu.empty().append($items)
                    return this;
                };
                typeahead.next = function(event) {
                    var active = this.$menu.find('.active').removeClass('active'),
                        next = active.nextAll(':not(.nav-header,.divider)').first();

                    if (!next.length) {
                        next = $(this.$menu.find('li:not(.nav-header,.divider)')[0])
                    }

                    next.addClass('active')
                };
                typeahead.prev = function(event) {
                    var active = this.$menu.find('.active').removeClass('active'),
                        prev = active.prevAll(':not(.nav-header,.divider)').first();

                    if (!prev.length) {
                        prev = this.$menu.find('li:not(.nav-header,.divider)').last()
                    }

                    prev.addClass('active')
                };
            }

            this.updatePropertiesSource();
        });

        this.onFilterProperties = function(event, data) {
            this.updatePropertiesSource(data.properties);
        };

        this.propertySelected = function(item) {
            var property = this.queryPropertyMap[item.title];

            if (property) {
                this.currentProperty = property;

                /**
                 * When the user selects a property, this event will be
                 * triggered
                 *
                 * @event module:components/PropertySelect#propertyselected
                 * @property {object} data
                 * @property {object} data.property The property object that was selected
                 * @example
                 * $node.on('propertyselected', function(event, data) {
                 *     console.log(data.property)
                 * })
                 * PropertySelect.attachTo($node)
                 */
                this.trigger('propertyselected', { property: property });
                _.defer(function() {
                    this.select('findPropertySelection').blur();
                }.bind(this));
            }
        };

        this.updatePropertiesSource = function(filtered) {
            var properties = filtered || this.attr.properties;

            this.groupedByDisplay = _.groupBy(properties, displayName);
            this.propertiesForSource = properties;
            this.dependentPropertyIris = _.chain(properties)
                .pluck('dependentPropertyIris')
                .compact()
                .flatten()
                .value();

            var hasProperties = this.filterSourceProperties(this.propertiesForSource).length > 0;
            var placeholderMessage = this.focused && hasProperties ?
                i18n('field.selection.placeholder') :
                hasProperties ?
                this.attr.placeholder :
                i18n('field.selection.no_valid');

            this.select('findPropertySelection')
                .attr('placeholder', placeholderMessage)
                .attr('disabled', !hasProperties);
        };

        this.filterSourceProperties = function(properties) {
            var self = this;

            return properties.filter(function(p) {
                    var visible = p.userVisible !== false;

                    if (self.attr.showAdminProperties) {
                        return true;
                    }

                    if (self.attr.unsupportedProperties &&
                        ~self.attr.unsupportedProperties.indexOf(p.title)) {
                        return false;
                    }

                    if (~HIDE_PROPERTIES.indexOf(p.title)) {
                        return false;
                    }

                    if (self.attr.onlySearchable !== true &&
                        self.attr.rollupCompound !== false &&
                        ~self.dependentPropertyIris.indexOf(p.title)) {
                        return false;
                    }

                    if (self.attr.rollupCompound === false &&
                       p.dependentPropertyIris) {
                        return false;
                    }

                    if (self.attr.onlySearchable) {
                        if (p.title === 'http://visallo.org#text') {
                            return true;
                        }
                        return visible && p.searchable !== false;
                    }

                    return visible;
                });
        };
    }

    function displayName(p) {
        return p.displayName || p.title;
    }
});

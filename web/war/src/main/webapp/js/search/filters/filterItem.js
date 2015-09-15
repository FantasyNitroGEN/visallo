define([
    'flight/lib/component',
    'fields/selection/selection',
    'hbs!./filterItemTpl'
], function(
    defineComponent,
    FieldSelection,
    template) {
    'use strict';

    var PREDICATES = {
            HAS: 'has',
            HAS_NOT: 'hasNot',
            CONTAINS: '~',
            EQUALS: '=',
            LESS_THAN: '<',
            GREATER_THAN: '>',
            BETWEEN: 'range',
            WITHIN: 'within'
        };

    return defineComponent(FilterItem);

    function FilterItem() {

        this.defaultAttrs({
            fieldsSelector: '.current-property .fields',
            fieldSelector: '.configuration',
            propertySelectionSelector: '.property-selector',
            currentPropertySelector: '.current-property',
            removeSelector: '.header button.remove',
            predicateSelector: '.current-property .header .dropdown .dropdown-menu li a'
        });

        this.before('teardown', function() {
            this.select('fieldSelector').teardownAllComponents();
            this.trigger('filterItemChanged', { removed: true });
        });

        this.after('initialize', function() {
            this.on('propertychange', this.onPropertyChanged);
            this.on('propertyselected', this.onPropertySelected);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('filterProperties', this.onFilterProperties);
            this.on('click', {
                predicateSelector: this.onPredicateClick,
                removeSelector: this.onRemoveRow
            })

            this.$node.html(template({}));

            if (this.attr.property) {
                this.setCurrentProperty(this.attr);
            } else {
                this.createFieldSelection();
            }
        });

        this.predicateNeedsValues = function() {
            return (this.filter.predicate !== PREDICATES.HAS && this.filter.predicate !== PREDICATES.HAS_NOT);
        };

        this.isValid = function() {
            var hasPredicateAndProperty = this.filter.predicate && this.filter.propertyId;
            if (hasPredicateAndProperty) {
                var propertyFieldRequired = this.predicateNeedsValues(),
                    rangeFilter = this.filter.predicate === PREDICATES.BETWEEN;

                if (rangeFilter) {
                    return !_.isEmpty(this.filter.values) && this.filter.values.length === 2;
                }
                if (propertyFieldRequired) {
                    return !_.isEmpty(this.filter.values);
                }
                return true;
            }
            return false;
        };

        this.triggerChange = function() {
            var valid = this.isValid(),
                filter = {
                    propertyId: this.filter.propertyId,
                    predicate: this.filter.predicate
                };

            if (this.predicateNeedsValues()) {

                if (this.filter.predicate === PREDICATES.BETWEEN) {
                    filter.values = _.sortBy(this.filter.values, function(val) {
                        if (_.isObject(val) &&
                            ('amount' in val) && ('unit' in val) &&
                            '_date' in val) {
                            return val._date;
                        }
                        return val;
                    });
                } else if (this.filter.predicate === PREDICATES.WITHIN) {
                    var geo = _.first(this.filter.values);
                    filter.values = geo ? [geo.latitude, geo.longitude, geo.radius] : new Array(3);
                } else {
                    filter.values = this.filter.values.slice(0, 1);
                }

                valid = valid && _.every(filter.values, function(v) {
                    return !_.isUndefined(v);
                });
            }

            this.$node.toggleClass('invalid', !valid);

            // Omit all underscore keys
            if (filter.values) {
                filter.values = filter.values.map(function(val) {
                    return _.isObject(val) ? _.omit(val, function(val, key) {
                        return (/^_/).test(key);
                    }) : val;
                });
            }

            this.trigger('filterItemChanged', {
                valid: valid,
                filter: filter
            });
        };

        this.onFilterProperties = function(event, data) {
            if ($(event.target).is(this.$node)) {
                this.select('propertySelectionSelector').trigger(event.type, data);
            }
        };

        this.onRemoveRow = function(event, data) {
            this.teardown();
        };

        this.onPredicateClick = function(event) {
            var self = this,
                $anchor = $(event.target),
                predicate = this.filter.predicate = $anchor.data('value');

            $anchor.closest('.dropdown').find('.dropdown-toggle span').text($anchor.text());

            this.attachFields().done();
        };

        this.onPropertyChanged = function(event, data) {
            event.stopPropagation();
            var index = $(event.target).closest('.configuration').index();
            if (this.filter.predicate === PREDICATES.BETWEEN) {
                if (!this.filter.values) {
                    this.filter.values = [undefined, undefined];
                }
                this.filter.values[index] = data.value;
            } else {
                if (index !== 0) return;
                this.filter.values = [data.value];
            }
            this.triggerChange();
        };

        this.onPropertySelected = function(event, data) {
            this.setCurrentProperty(data);
        };

        this.onPropertyInvalid = function(event, data) {
            var index = $(event.target).closest('.configuration').index();
            if (this.filter.predicate === PREDICATES.BETWEEN) {
                if (this.filter.values && index < this.filter.values.length) {
                    this.filter.values.splice(index, 1, undefined);
                }
            } else {
                if (index !== 0) return;
                this.filter.values = [];
            }
            this.triggerChange();
        };

        this.setCurrentProperty = function(data) {
            var self = this,
                property = data.property,
                hasProperty = !!property;

            this.currentProperty = property;
            this.filter = {
                predicate: data.predicate,
                propertyId: property && property.title,
                values: data.values || []
            };

            this.select('propertySelectionSelector')
                .toggle(!hasProperty);
            this.select('currentPropertySelector')
                .find('label')
                    .text(property.displayName)
                .end()
                .find('.dropdown-menu')
                    .html(this.predicateItemsForProperty(property))
                .end()
                .find('.dropdown-toggle')
                    .each(function() {
                        var selected = $(this).next().find('.selected a');
                        if (!selected.length) {
                            selected = $(this).next().children('li').first().find('a');
                        }
                        self.filter.predicate = selected.data('value');
                        $(this).find('span').text(selected.text());
                    })
                .end()
                .toggle(hasProperty);

            if (hasProperty) {
                var values = this.filter.values;
                if (property.dataType === 'geoLocation' && values.length > 1) {
                    this.filter.values = [{
                        latitude: values[0] || '',
                        longitude: values[1] || '',
                        radius: values[2] || ''
                    }];
                }
                this.attachFields();
            }
        };

        this.focusField = function() {
            if (_.isEmpty(this.filter.values) && this.predicateNeedsValues()) {
                var index = 0;
                if (!_.isUndefined(this.filter.values[0]) &&
                    this.filter.predicate === PREDICATES.BETWEEN &&
                    (this.filter.values.length < 2 || _.isUndefined(this.filter.values[1]))) {
                    index = 1;
                }
                this.select('fieldSelector').eq(index).trigger('focusPropertyField');
            }
        };

        this.attachFields = function() {
            var self = this,
                fieldComponent,
                property = this.currentProperty,
                isCompoundField = property && property.dependentPropertyIris && property.dependentPropertyIris.length;

            if (isCompoundField) {
                fieldComponent = 'fields/compound/compound';
            } else if (property.displayType === 'duration') {
                fieldComponent = 'fields/duration';
            } else if (property.dataType === 'date') {
                fieldComponent = 'search/filters/dateField';
            } else {
                fieldComponent = property.possibleValues ? 'fields/restrictValues' : 'fields/' + property.dataType;
            }

            return Promise.require(fieldComponent).then(function(PropertyFieldItem) {
                var node = self.select('fieldSelector'),
                    nodesRendered = _.reduce(node.toArray(), function(sum, el) {
                        return sum + ($(el).lookupComponent(PropertyFieldItem) ? 1 : 0);
                    }, 0),
                    fieldsToRender = 1 - nodesRendered;

                if (self.filter.predicate === PREDICATES.BETWEEN) {
                    fieldsToRender = 2 - nodesRendered;
                    node.show();
                    if (node.length < 2) {
                        node = node.add($('<div class="configuration">').appendTo(self.select('fieldsSelector')));
                    }
                } else if (self.predicateNeedsValues()) {
                    node.eq(0).show();
                    node.eq(1).hide();
                } else {
                    node.hide();
                }

                if (fieldsToRender > 0) {
                    self.on('fieldRendered', function rendered(event) {
                        fieldsToRender--;
                        if (fieldsToRender === 0) {
                            self.off(event.type, rendered);
                            self.focusField();
                        }
                    })
                } else {
                    self.focusField();
                }
                if (isCompoundField) {
                    throw new Error('Compound properties not supported in filters.');
                } else {
                    node.each(function(i, el) {
                        PropertyFieldItem.attachTo(el, {
                            onlySearchable: true,
                            focus: false,
                            property: property,
                            value: self.filter.values[i]
                        });
                    })
                }

                self.triggerChange();
            });
        };
        this.predicatesForProperty = function(property) {
            var standardPredicates = [PREDICATES.HAS, PREDICATES.HAS_NOT];

            if (property.possibleValues) {
                return [PREDICATES.EQUALS].concat(standardPredicates);
            }

            switch (property.dataType) {
                case 'string': return [
                        PREDICATES.CONTAINS,
                        PREDICATES.EQUALS
                    ].concat(standardPredicates);

                case 'geoLocation': return [
                        PREDICATES.WITHIN
                    ].concat(standardPredicates);

                case 'boolean': return [
                        PREDICATES.EQUALS
                    ].concat(standardPredicates);

                case 'date':
                case 'currency':
                case 'double':
                case 'integer':
                case 'number': return [
                        PREDICATES.LESS_THAN,
                        PREDICATES.GREATER_THAN,
                        PREDICATES.BETWEEN,
                        PREDICATES.EQUALS
                    ].concat(standardPredicates)

                default:
                    throw new Error('Unknown datatype: ' + property.dataType);
            }
        };

        this.predicateItemsForProperty = function(property) {
            if (!property) return '';

            var self = this;

            return $.map(this.predicatesForProperty(property), function(predicate, i) {
                var displayText = (
                        property.displayType &&
                        i18n(true, 'search.filters.predicates.' + property.dataType + '.' + property.displayType + '.' + predicate, property.displayName)
                    ) ||
                    i18n(true, 'search.filters.predicates.' + property.dataType + '.' + predicate, property.displayName) ||
                    i18n('search.filters.predicates.' + predicate, property.displayName);
                return $('<li><a></a></li>')
                    .toggleClass('selected', self.filter.predicate ? self.filter.predicate === predicate : i === 0)
                    .find('a')
                        .text(displayText)
                        .attr('title', displayText)
                        .data('value', predicate)
                    .end();
            });
        };

        this.createFieldSelection = function() {
            FieldSelection.attachTo(this.select('propertySelectionSelector'), {
                properties: this.attr.properties,
                onlySearchable: true,
                placeholder: i18n('search.filters.add_filter.placeholder')
            });
        };


    }
});

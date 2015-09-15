define([
    'require',
    'flight/lib/component',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(require, defineComponent, withDataRequest, F) {
    'use strict';

    return defineComponent(CompoundField, withDataRequest);

    function CompoundField() {

        this.before('initialize', function(node, config) {
            config.asyncRender = true;
        });

        this.after('initialize', function() {
            var self = this;

            this.compoundValues = {};

            this.on('propertychange', this.onDependentPropertyChange);
            this.on('propertyinvalid', this.onDependentPropertyInvalid);

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    self.ontologyProperties = ontologyProperties;
                    self.render();
                });
        });

        this.triggerFieldUpdated = function() {
            if (this.isValid()) {
                this.trigger('propertychange', {
                    propertyId: this.attr.property.title,
                    values: this.getValues()
                });
            } else {
                this.trigger('propertyinvalid', {
                    propertyId: this.attr.property.title
                });
            }
        }

        this.onDependentPropertyChange = function(event, data) {
            if ($(event.target).is(this.$node)) {
                return;
            }

            event.stopPropagation();

            this.compoundValues[data.propertyId] = data.value;

            this.triggerFieldUpdated();
        };

        this.onDependentPropertyInvalid = function(event, data) {
            if ($(event.target).is(this.$node)) {
                return;
            }

            event.stopPropagation();
            this.compoundValues[data.propertyId] = data.value;
            this.triggerFieldUpdated();
        };

        this.getValues = function() {
            var self = this;
            return _.chain(this.attr.property.dependentPropertyIris)
                .map(function(iri) {
                    var result = self.compoundValues[iri];
                    if (_.isArray(result)) {
                        return result;
                    } else if (_.isUndefined(result)) {
                        return [''];
                    }
                    return [result];
                })
                .value()
        };

        this.isValid = function() {
            var values = this.getValues();

            if (this.attr.vertex) {
                // TODO: should pass key?
                return F.vertex.propValid(this.attr.vertex, values, this.attr.property.title);
            }

            return _.any(values, function(v) {
                return v && v.length;
            })
        };

        this.render = function() {
            var self = this,
                fields = $(),
                names = _.indexBy(this.attr.values, 'name');

            Promise.all(this.attr.property.dependentPropertyIris.map(function(propertyIri, i) {
                var ontologyProperty = self.ontologyProperties.byTitle[propertyIri],
                    fieldContainer = $('<div>').addClass('compound-field'),
                    property = names[propertyIri],
                    previousValue = property ? property.value : '';

                self.compoundValues[propertyIri] = previousValue;

                return Promise.require(
                    ontologyProperty.possibleValues ?
                        'fields/restrictValues' :
                        'fields/' + ontologyProperty.dataType
                ).then(function(PropertyField) {
                    PropertyField.attachTo(fieldContainer, {
                        property: ontologyProperty,
                        newProperty: !property,
                        tooltip: {
                            title: ontologyProperty.displayName,
                            placement: 'left',
                            trigger: 'focus'
                        },
                        vertexProperty: property,
                        value: previousValue,
                        predicates: self.attr.predicates,
                        composite: true,
                        focus: i === 0
                    });
                    fields = fields.add(fieldContainer);
                })
            })).done(function() {
                self.$node.empty().append(fields);
                self.trigger('fieldRendered');
            })
        };
    }
});

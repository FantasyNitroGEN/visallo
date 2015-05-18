define([
    'require',
    'flight/lib/component',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(require, defineComponent, withDataRequest, F) {
    'use strict';

    return defineComponent(CompoundField, withDataRequest);

    function CompoundField() {

        this.after('initialize', function() {
            var self = this;

            this.compoundValues = {};

            this.on('propertychange', this.onDependentPropertyChange);
            this.on('propertyinvalid', this.onDependentPropertyInvalid);

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    self.ontologyProperties = ontologyProperties;
                    self.render();
                    self.triggerIfValid();
                });
        });

        this.onDependentPropertyChange = function(event, data) {
            if ($(event.target).is(this.$node)) {
                return;
            }

            event.stopPropagation();

            this.compoundValues[data.propertyId] = data.values;

            this.triggerIfValid();
        };

        this.triggerIfValid = function() {
            if (this.isValid()) {
                this.trigger('propertychange', {
                    id: this.attr.id,
                    propertyId: this.attr.property.title,
                    values: this.getValues()
                });
            } else {
                this.trigger('propertyinvalid', {
                    id: this.attr.id,
                    propertyId: this.attr.property.title
                });
            }
        }

        this.onDependentPropertyInvalid = function(event, data) {
            if ($(event.target).is(this.$node)) {
                return;
            }

            event.stopPropagation();
            this.compoundValues[data.propertyId] = data.values;
            this.triggerIfValid();
        };

        this.getValues = function() {
            var self = this;
            return _.chain(this.attr.property.dependentPropertyIris)
                .map(function(iri) {
                    var result = self.compoundValues[iri];
                    if (_.isArray(result)) {
                        return result;
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

            this.attr.property.dependentPropertyIris.forEach(function(propertyIri, i) {
                var ontologyProperty = self.ontologyProperties.byTitle[propertyIri],
                    fieldContainer = $('<div>').addClass('compound-field'),
                    property = names[propertyIri],
                    previousValue = property ? property.value : '';

                self.compoundValues[propertyIri] = previousValue;

                require([
                    ontologyProperty.possibleValues ?
                        '../restrictValues' :
                        '../' + ontologyProperty.dataType
                ], function(PropertyField) {
                    PropertyField.attachTo(fieldContainer, {
                        property: ontologyProperty,
                        newProperty: !property,
                        vertexProperty: property,
                        value: previousValue,
                        predicates: self.attr.predicates,
                        composite: true,
                        focus: i === 0
                    });
                    self.trigger('compoundReady');
                })
                fields = fields.add(fieldContainer);
            })

            this.$node.empty().append(fields);
        };
    }
});

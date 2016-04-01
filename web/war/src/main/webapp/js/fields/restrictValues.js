define([
    'flight/lib/component',
    './withPropertyField',
    'hbs!./restrictValuesTpl'
], function(defineComponent, withPropertyField, template) {
    'use strict';

    return defineComponent(RestrictValuesField, withPropertyField);

    function RestrictValuesField() {

        this.after('initialize', function() {
            var val = this.attr.value;

            var propertyPossibleValues = this.attr.property.possibleValues,
                possibleValues = _.map(Object.keys(propertyPossibleValues), function(key, i) {
                    return {
                        i: i,
                        value: key,
                        display: propertyPossibleValues[key],
                        selected: key === String(val)
                    }
                });

            this.$node.html(template({
                displayName: this.attr.property.displayName,
                values: possibleValues,
                placeholderKey: this.attr.placeholderKey || 'field.restrict_values.form.placeholder'
            }));
        });

        this.setValue = function(value) {
            this.select('inputSelector').val(value);
        };

        this.getValue = function() {
            return this.select('inputSelector').val();
        };

        this.isValid = function(value) {
            return value.length;
        };

    }
});

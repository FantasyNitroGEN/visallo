
define([
    'flight/lib/component',
    'hbs!./doubleTpl',
    'util/parsers',
    'util/vertex/formatters',
    './withPropertyField',
    './withHistogram'
], function(defineComponent, template, P, F, withPropertyField, withHistogram) {
    'use strict';

    return defineComponent(DoubleField, withPropertyField, withHistogram);

    function makeNumber(v) {
        return P.number.parseFloat(v);
    }

    function DoubleField() {

        this.after('initialize', function() {
            this.$node.html(template(this.attr));
        });

        this.setValue = function(value) {
            this.select('inputSelector').val(value);
        };

        this.getValue = function() {
            return makeNumber(this.select('inputSelector').val().trim());
        };

        this.getMetadata = function() {
            var currentVal = this.select('inputSelector').val().trim();
            if (currentVal && !isNaN(makeNumber(currentVal))) {
                var inputPrecision = 0,
                    decimalPart = currentVal.match(/\.(\d+)/);
                if (decimalPart) {
                    inputPrecision = decimalPart[1].length;
                }
                return { 'http://visallo.org#inputPrecision': inputPrecision };
            }
            return null;
        };

        this.isValid = function(value) {
            var name = this.attr.property.title;
            return _.isNumber(value) && !isNaN(value) && F.vertex.singlePropValid(value, name);
        };
    }
});

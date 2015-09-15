
define([
    'flight/lib/component',
    'hbs!./currencyTpl',
    'util/parsers',
    'util/vertex/formatters',
    './withHistogram',
    './withPropertyField'
], function(defineComponent, template, P, F, withHistogram, withPropertyField) {
    'use strict';

    return defineComponent(CurrencyField, withPropertyField, withHistogram);

    function makeNumber(v) {
        return P.number.parseFloat(v);
    }

    function CurrencyField() {

        this.after('initialize', function() {
            this.$node.html(template(this.attr));
        });

        this.setValue = function(value) {
            this.select('inputSelector').val(value);
        };

        this.getValue = function() {
            return makeNumber(this.select('inputSelector').val().trim());
        };

        this.isValid = function(value) {
            var name = this.attr.property.title;
            return _.isNumber(value) && !isNaN(value) && F.vertex.singlePropValid(value, name);
        };
    }
});

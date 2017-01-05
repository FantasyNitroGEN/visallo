
define([
    'flight/lib/component',
    './integerTpl.hbs',
    'util/parsers',
    'util/vertex/formatters',
    './withPropertyField',
    './withHistogram'
], function(defineComponent, template, P, F, withPropertyField, withHistogram) {
    'use strict';

    return defineComponent(IntegerField, withPropertyField, withHistogram);

    function makeNumber(v) {
        return P.number.parseInt(v);
    }

    function IntegerField() {

        this.after('initialize', function() {
            this.$node.html(template(this.attr));
        });

        this.isValid = function(value) {
            var name = this.attr.property.title;

            return _.isNumber(value) && !isNaN(value) && F.vertex.singlePropValid(value, name);
        };

        this.setValue = function(value) {
            this.select('inputSelector').val(value);
        };

        this.getValue = function() {
            return makeNumber(this.select('inputSelector').val().trim());
        };
    }
});

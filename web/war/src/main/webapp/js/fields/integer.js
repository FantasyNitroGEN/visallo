
define([
    'flight/lib/component',
    'tpl!./integer',
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

            this.updateRangeVisibility();

            this.on('change keyup', {
                inputSelector: function() {
                    this.updateRangeVisibility();
                    this.triggerFieldUpdated();
                }
            });
        });

        this.triggerFieldUpdated = function() {
            this.filterUpdated(
                this.getValues().map(function(v) {
                    return makeNumber(v);
                }),
                this.select('predicateSelector').val()
            );
        };

        this.isValid = function() {
            var name = this.attr.property.title,
                values = this.getValues();

            return _.every(values, function(v) {
                var n = makeNumber(v);
                return v.length && _.isNumber(n) && !isNaN(n) && F.vertex.singlePropValid(n, name);
            });
        };
    }
});

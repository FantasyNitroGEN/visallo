
define([
    'flight/lib/component',
    'tpl!./double',
    'duration-js',
    'util/formatters',
    'util/parsers',
    './withPropertyField',
    './withHistogram'
], function(defineComponent, template, Duration, F, P, withPropertyField, withHistogram) {
    'use strict';

    return defineComponent(DurationField, withPropertyField, withHistogram);

    function toSeconds(v) {
        try {
            var allValid = _.every(v.trim().split(/\s+/), function(n) {
                return P.number.isValidWithUnits(n);
            });

            if (allValid) {
                return Duration.parse(v).milliseconds() / 1000.0;
            } else {
                return NaN;
            }
        } catch (e) {
            return NaN;
        }
    }

    function DurationField() {

        this.after('initialize', function() {
            if (this.attr.value) {
                this.attr.value = F.number.duration(this.attr.value);
            }

            this.$node.html(template(this.attr));

            $(this.$node.find('.input-row input')).attr('pattern', '^([\\d.]+[wdhms]+[\\s,;:]*)+$');

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
                    return toSeconds(v);
                }),
                this.select('predicateSelector').val()
            );
        };

        this.isValid = function() {
            return !_.any(this.getValues(), function(v) {
                return isNaN(toSeconds(v));
            });
        };
    }
});

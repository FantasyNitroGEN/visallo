
define([
    'flight/lib/component',
    'tpl!./double',
    'duration-js',
    'util/formatters',
    './withPropertyField',
    './withHistogram'
], function(defineComponent, template, Duration, F, withPropertyField, withHistogram) {
    'use strict';

    return defineComponent(DurationField, withPropertyField, withHistogram);

    function toSeconds(v) {
        try {
            return Duration.parse(v).milliseconds() / 1000.0;
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

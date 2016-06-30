
define([
    'flight/lib/component',
    'hbs!./doubleTpl',
    'duration-js',
    'util/parsers',
    'util/vertex/formatters',
    './withPropertyField',
    './withHistogram'
], function(defineComponent, template, Duration, P, F, withPropertyField, withHistogram) {
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
            this.attr.placeholder = i18n('field.double.displaytype.duration.placeholder');

            this.$node
                .html(template(this.attr))
                .find('input').attr('pattern', '^([\\d.]+[wdhms]+[\\s,;:]*)+$');
        });

        this.setValue = function(value) {
            this.select('inputSelector').val(F.number.duration(value));
        };

        this.getValue = function() {
            return toSeconds(this.select('inputSelector').val().trim());
        };

        this.isValid = function(value) {
            var name = this.attr.property.title;
            return _.isNumber(value) && !isNaN(value) && F.vertex.singlePropValid(value, name);
        };

        this.getMetadata = function() {
            var currentVal = this.getValue();
            if (currentVal && !isNaN(currentVal)) {
                return { 'http://visallo.org#inputPrecision': 0 };
            }
            return null;
        };
    }
});

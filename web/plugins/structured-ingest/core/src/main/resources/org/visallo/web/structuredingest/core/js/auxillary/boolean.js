define([
    'flight/lib/component',
    'hbs!../../templates/auxillary/boolean'
], function(
    defineComponent,
    template) {
    'use strict';

    var BLANK_OPTIONS = [
            { value: '', display: 'Skip' },
            { value: 'true', display: 'True' },
            { value: 'false', display: 'False' }
        ],
        SPLIT_REGEX = /\s*,\s*/

    return defineComponent(BooleanAuxillary);

    function process(strlist) {
        var a = _.isArray(strlist) ? strlist : (strlist || '').split(SPLIT_REGEX);
        return a.join(', ');
    }

    function BooleanAuxillary() {

        this.defaultAttrs({
            inputSelector: 'input',
            selectSelector: 'select'
        });

        this.after('initialize', function() {
            var hints = this.attr.mapping.hints;
            this.$node.html(template({
                trueValues: process(hints.trueValues || 'yes, y, true, 1'),
                falseValues: process(hints.falseValues || 'no, n, false, 0'),
                blank: BLANK_OPTIONS.map(function(o) {
                    o.selected = false;
                    if ('defaultValue' in hints) {
                        o.selected = (o.value === 'true') === hints.defaultValue;
                    }
                    return o;
                })
            }));

            this.on('change', {
                selectSelector: this.triggerChange
            });

            this.on('change keyup paste', {
                inputSelector: this.triggerChange
            });

            this.triggerChange();
        });

        this.onChange = function() {
        };

        this.triggerChange = function() {
            var self = this;

            this.trigger('addAuxillaryData', _.tap({
                trueValues: this.$node.find('input.true').val().split(SPLIT_REGEX),
                falseValues: this.$node.find('input.false').val().split(SPLIT_REGEX)
            }, function(d) {
                var blank = self.$node.find('select.blank').val();
                if (blank) {
                    d.defaultValue = blank === 'true';
                }
            }));
        };

    }
});

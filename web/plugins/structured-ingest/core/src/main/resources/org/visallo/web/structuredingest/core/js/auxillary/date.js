define([
    'flight/lib/component',
    '../../templates/auxillary/date.hbs',
    'moment',
    'util/formatters'
], function(
    defineComponent,
    template,
    moment,
    F) {
    'use strict';

    return defineComponent(DateAuxillary);

    function formats(dateOnly, selected) {
        var now = moment(),
            dateTimes = [
                { js: 'YYYY-MM-DD HH:mm', java: 'yyyy-dd-MM HH:mm' },
                { js: 'YYYY-MM-DDTHH:mm', java: 'yyyy-dd-MM\'T\'HH:mm' },
                { js: 'M/D/YYYY h:mm a', java: 'M/d/yyyy h:mm a' }
            ],
            datesOnly = [
                { js: 'YYYY-MM-DD', java: 'yyyy-dd-MM' },
                { js: 'M/D/YYYY', java: 'M/d/yyyy' },
                { js: 'M-D-YYYY', java: 'M-d-yyyy' },
                { js: 'D-M-YYYY', java: 'D-M-yyyy' }
            ];

        return (dateOnly ? datesOnly : dateTimes).map(function(f, i) {
            return {
                format: f.java,
                example: now.format(f.js),
                selected: selected ? (f.java === selected) : i === 0
            }
        })
    }

    function DateAuxillary() {

        this.defaultAttrs({
            inputSelector: 'input',
            formatSelector: 'input.format',
            timezoneSelector: 'input.timezone',
            selectSelector: 'select'
        });

        this.after('initialize', function() {
            var f = formats(this.attr.property.dateOnly || false, this.attr.mapping.hints.format);

            this.$node.html(template({
                timezone: this.attr.mapping.hints.timezone || F.timezone.currentTimezone().name,
                formats: f,
                format: this.attr.mapping.hints.format || f[0].format
            }));

            this.on('change keyup paste', {
                inputSelector: this.onChange
            })
            this.on('change', {
                selectSelector: this.onSelectChange
            })
            this.$node.find('input').trigger('change');

        });

        this.onSelectChange = function() {
            this.select('formatSelector').val(
                this.select('selectSelector').val()
            );
            this.onChange();
        }

        this.onChange = function() {
            this.trigger('addAuxillaryData', {
                format: this.select('formatSelector').val(),
                timezone: this.select('timezoneSelector').val()
            });
        }

    }
});


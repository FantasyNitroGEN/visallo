define([ 'flight/lib/component',
    'fields/date',
    'fields/dateRelative',
    'hbs!./dateFieldTpl'
], function(
    defineComponent,
    DateField,
    DateRelativeField,
    template) {
    'use strict';

    return defineComponent(DateFieldAbsoluteRelative);

    function DateFieldAbsoluteRelative() {

        this.defaultAttrs({
            fieldSelector: '.date-field',
            wrapperSelector: '.date-field-filter-wrap',
            switchSelector: '.switch-absolute, .switch-relative'
        })

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.on('click', {
                switchSelector: this.onSwitch
            });

            this.toggle(this.isRelativeValue(this.attr.value));
        });

        this.isRelativeValue = function(value) {
            return _.isObject(value) &&
                ('amount' in value) &&
                ('unit' in value);
        };

        this.onSwitch = function(event) {
            var toRelative = $(event.target).hasClass('switch-relative');
            this.toggle(toRelative);
        };

        this.toggle = function(toRelative) {
            var $field = this.select('fieldSelector').teardownAllComponents();
            if (toRelative) {
                DateRelativeField.attachTo($field, this.attr);
            } else {
                DateField.attachTo($field, this.attr);
            }

            this.select('wrapperSelector').toggleClass('relative', toRelative);
        }
    }
});

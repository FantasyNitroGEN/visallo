define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(DefaultEditor);

    function DefaultEditor() {

        this.defaultAttrs({
            fieldSelector: 'input',
            placeholder: i18n('visibility.label')
        })

        this.after('initialize', function() {
            this.$node.html(
                $('<input>')
                    .attr('placeholder', this.attr.placeholder)
                    .attr('type', 'text')
                    .attr('value', $.trim(_.isUndefined(this.attr.value) ? '' : this.attr.value))
                    .prop('readonly', this.attr.readonly)
            );

            this.on('visibilityclear', this.onClear);
            this.on('change keyup paste', {
                fieldSelector: this.onChange
            });

            this.onChange();
        });

        this.onClear = function(event, data) {
            this.select('fieldSelector').val('');
        };

        this.onChange = function(event, data) {
            var value = $.trim(this.select('fieldSelector').val());
            this.trigger('visibilitychange', {
                value: value,
                valid: true
            });
        };
    }
});

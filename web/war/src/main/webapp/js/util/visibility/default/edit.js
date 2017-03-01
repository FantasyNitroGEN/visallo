define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(DefaultEditor);

    /**
     * @typedef org.visallo.visibility~Editor
     * @property {string} [value] The visibility source to prepopulate the editor
     * @property {string} [placeholder] The placeholder text to display when no
     * value
     * @property {string} [readonly] Show the form in read-only mode
     * @listens org.visallo.visibility#visibilityclear
     * @fires org.visallo.visibility#visibilitychange
     */
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

        /**
         * Reset the form
         * @event org.visallo.visibility#visibilityclear
         */
        this.onClear = function(event, data) {
            this.select('fieldSelector').val('');
        };

        this.onChange = function(event, data) {
            var value = $.trim(this.select('fieldSelector').val());
            var valid = this.checkValid(value);
            /**
             * The user has adjusted the visibility so notify
             *
             * @event org.visallo.visibility#visibilitychange
             * @param {object} data
             * @param {string} data.value The new visibility value
             * @param {boolean} data.valid Whether the value is valid
             */
            this.trigger('visibilitychange', {
                value: value,
                valid: valid
            });
        };

        this.checkValid = function(value) {
            var visibilities = value.replace(/\(|\)/g, '').split(/\&|\|/g);
            var authorizations = visalloData.currentUser.authorizations;

            return !value.length || !_.difference(visibilities, authorizations).length;
        };
    }
});

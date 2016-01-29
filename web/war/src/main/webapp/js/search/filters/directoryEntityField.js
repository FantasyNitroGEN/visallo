define([
    'flight/lib/component',
    'fields/directory/entity',
    'fields/directory/currentUser',
    'hbs!./directoryEntityFieldTpl'
], function(
    defineComponent,
    DirectoryEntityField,
    DirectoryCurrentUserField,
    template) {
    'use strict';

    return defineComponent(DirectoryEntityFieldAbsoluteCurrent);

    function DirectoryEntityFieldAbsoluteCurrent() {

        this.attributes({
            fieldSelector: '.directory-entity-field',
            wrapperSelector: '.directory-entity-field-filter-wrap',
            switchSelector: '.switch-choose-user, .switch-current-user',
            value: '',
            onlySearchable: null,
            focus: null,
            property: null
        });

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.on('click', {
                switchSelector: this.onSwitch
            });

            this.toggle(this.isCurrentUserValue(this.attr.value));
        });

        this.isCurrentUserValue = function(value) {
            return _.isObject(value) && ('currentUser' in value);
        };

        this.onSwitch = function(event) {
            var toCurrentUser = $(event.target).hasClass('switch-current-user');
            this.toggle(toCurrentUser);
        };

        this.toggle = function(toCurrentUser) {
            var $field = this.select('fieldSelector').teardownAllComponents();
            if (toCurrentUser) {
                DirectoryCurrentUserField.attachTo($field, this.attr);
            } else {
                DirectoryEntityField.attachTo($field, this.attr);
            }

            this.select('wrapperSelector').toggleClass('current-user', toCurrentUser);
        };
    }
});

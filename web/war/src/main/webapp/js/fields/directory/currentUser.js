define([
    'flight/lib/component',
    '../withPropertyField'
], function(
    defineComponent,
    withPropertyField) {
    'use strict';

    return defineComponent(CurrentUser, withPropertyField);

    function CurrentUser() {
        this.after('initialize', function() {
            this.$node.html($('<span>' + i18n('field.directory.current_user') + '</span>'));
        })

        this.getValue = function() {
            return {
                currentUser: true
            };
        };

        this.isValid = function(value) {
            return true;
        };

        this.setValue = function() {
        }
    }

});

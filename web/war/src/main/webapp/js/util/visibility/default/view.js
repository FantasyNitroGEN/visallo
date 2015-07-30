define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(DefaultViewer);

    function DefaultViewer() {

        this.after('initialize', function() {
            var value = this.attr.value;

            if (_.isUndefined(value) || value === '') {
                this.$node.html(
                    $('<i>').text(i18n('visibility.blank'))
                );
            } else {
                this.$node.text(value);
            }
        });
    }
});

define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(DefaultViewer);

    /**
     * @typedef org.visallo.visibility~Viewer
     * @property {string} [value] The visibility source to view
     * @property {string} [property] The property that this visibility is
     * attached. Could be undefined
     * @property {string} [element] The element that the visibility is a part
     * of. Could be undefined
     */
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

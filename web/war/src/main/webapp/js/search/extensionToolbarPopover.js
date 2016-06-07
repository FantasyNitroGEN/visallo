define([
    'flight/lib/component',
    'util/popovers/withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    return defineComponent(SearchToolbarExtensionPopover, withPopover);

    function SearchToolbarExtensionPopover() {

        this.defaultAttrs({
            Component: null,
            extension: null,
            model: null
        })

        this.before('teardown', function() {
            this.popover.find('.popover-content').teardownAllComponents();
        })

        this.before('initialize', function(node, config) {
            config.template = '/search/extensionToolbarPopoverTpl';
            this.after('setupWithTemplate', function() {
                this.attr.Component.attachTo(this.popover.find('.popover-content'), {
                    model: _.extend({}, this.attr.model, {
                        extension: this.attr.extension,
                        element: this.$node.closest('.search-pane').get(0)
                    })
                });
                this.positionDialog();
            });
        });

    }
});

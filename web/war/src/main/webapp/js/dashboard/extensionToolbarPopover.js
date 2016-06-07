define([
    'flight/lib/component',
    'util/popovers/withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    return defineComponent(ToolbarExtensionPopover, withPopover);

    function ToolbarExtensionPopover() {

        this.defaultAttrs({
            Component: null,
            item: null,
            extension: null
        })

        this.before('teardown', function() {
            this.$node.closest('.card-toolbar').removeClass('active');
            this.popover.find('.popover-content').teardownAllComponents();
        })

        this.before('initialize', function(node, config) {
            config.template = '/dashboard/extensionToolbarPopoverTpl';
            this.after('setupWithTemplate', function() {
                this.$node.closest('.card-toolbar').addClass('active');
                var $element = this.$node.closest('.grid-stack-item');
                this.attr.Component.attachTo(this.popover.find('.popover-content'), {
                    model: {
                        item: this.attr.item,
                        extension: this.attr.extension,
                        element: $element.length ? $element.get(0) : this.node
                    }
                });
                this.positionDialog();
            });
        });

    }
});

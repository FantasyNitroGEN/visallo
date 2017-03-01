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
            config.template = '/search/extensionToolbarPopoverTpl.hbs';
            this.after('setupWithTemplate', function() {
                /**
                 * Flight component to render in the popover
                 * created when clicking a search toolbar item.
                 *
                 * @typedef org.visallo.search.toolbar~Component
                 * @property {object} model
                 * @property {object} model.search Current search object or
                 * null
                 * @property {object} model.extension The toolbar extension
                 * @property {Element} model.element The toolbar extension
                 * element
                 */
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

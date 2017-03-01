
define([
    'flight/lib/component',
    '../withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    return defineComponent(ExportWorkspace, withPopover);

    function ExportWorkspace() {

        this.defaultAttrs({
            cancelButtonSelector: 'button.btn-default'
        });

        this.before('teardown', function() {
            this.popover.find('.plugin-content').teardownAllComponents();
        });

        this.before('initialize', function(node, config) {
            config.template = 'exportWorkspace/template';
            config.showTitle = config.exporter.showPopoverTitle !== false;
            config.showCancel = config.exporter.showPopoverCancel !== false;
            config.title = i18n('popovers.export_workspace.title', config.exporter.menuItem);
            config.hideDialog = true;

            this.after('setupWithTemplate', function() {
                var self = this,
                    node = this.popover.find('.plugin-content'),
                    workspaceId = this.attr.workspaceId,
                    productId = this.attr.productId,
                    exporter = this.attr.exporter;

                this.on(this.popover, 'click', {
                    cancelButtonSelector: this.onCancel
                });

                require([exporter.componentPath], function(C) {
                    var attrs = {
                        workspaceId,
                        productId,
                        exporter,
                        cy: self.attr.cy
                    };

                    if (_.isFunction(exporter.attributes)) {
                        attrs = exporter.attributes(attrs);
                    }

                    /**
                     * If the exporter extension configuration includes an
                     * `attributes` function, all those attributes will be
                     * available as well.
                     *
                     * @typedef org.visallo.graph.export~Exporter
                     * @property {string} workspaceId
                     * @property {string} productId
                     * @property {object} exporter
                     * @property {object} cy The cytoscape object
                     */
                    C.attachTo(node, attrs);
                    self.dialog.show();
                    self.positionDialog();
                });
            });

            this.onCancel = function() {
                this.teardown();
            }
        });
    }
});

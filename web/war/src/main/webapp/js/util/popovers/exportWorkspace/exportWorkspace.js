
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
                    exporter = this.attr.exporter;

                this.on(this.popover, 'click', {
                    cancelButtonSelector: this.onCancel
                });

                require([exporter.componentPath], function(C) {
                    var attrs = {
                        workspaceId: workspaceId,
                        exporter: exporter,
                        cy: self.attr.cy
                    };

                    if (_.isFunction(exporter.attributes)) {
                        attrs = exporter.attributes(attrs);
                    }
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

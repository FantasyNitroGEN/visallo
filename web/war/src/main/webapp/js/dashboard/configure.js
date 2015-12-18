define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'util/popovers/withPopover'
], function(
    defineComponent,
    registry,
    withPopover) {
    'use strict';

    var reportRenderers = registry.extensionsForPoint('org.visallo.web.dashboard.reportrenderer'),
        extensions = registry.extensionsForPoint('org.visallo.web.dashboard.item');

    return defineComponent(ConfigPopover, withPopover);

    function ConfigPopover() {

        this.before('teardown', function() {
            this.$node.removeClass('active');
        })

        this.before('initialize', function(node, config) {
            config.template = '/dashboard/configureTpl';
            var paths = config.configurationPaths || [],
                extension = _.findWhere(extensions, { identifier: config.item.extensionId }),
                report = config.item.configuration.report || extension.report,
                reportConfigurationPath = 'dashboard/configs/report',
                addDefaultConfiguration = !extension.options ||
                    extension.options.preventDefaultConfig !== true;

            this.extension = extension;

            if (extension.configurationPath) {
                paths.push(extension.configurationPath);
            }

            if (report) {
                paths.push(reportConfigurationPath);
            }

            if (addDefaultConfiguration) {
                paths.splice(0, 0, 'dashboard/configs/default');
            }

            config.empty = paths.length === 0;

            var configPathPromises = paths.map(function(path) {
                return Promise.all([path, Promise.require(path)]);
            });

            this.after('setupWithTemplate', function() {
                var self = this,
                    item = this.attr.item;

                this.$node.addClass('active');
                this.on(this.popover, 'redirectEventToItem', function(event, data) {
                    this.$node.closest('.grid-stack-item').find('.item-content').trigger(data.name, data.data);
                });
                this.on(this.$node.closest('.grid-stack-item').find('.item-content'), 'redirectEventToConfiguration', function(event, data) {
                    this.popover.find('.popover-content > div').trigger(data.name, data.data);
                })
                this.on(this.popover, 'configurationChanged', function(event, data) {
                    self.trigger(event.type, data);

                    var reportAdded = data.item.configuration.report || extension.report,
                        reportRemoved = !data.item.configuration.report && !extension.report;
                    if (reportAdded) {
                        this.getDivForPath(reportConfigurationPath)
                            .teardownAllComponents()
                            .remove();
                        this.renderConfigurations([
                            Promise.all([reportConfigurationPath, Promise.require(reportConfigurationPath)])
                        ]);
                    } else if (reportRemoved) {
                        this.getDivForPath(reportConfigurationPath)
                            .teardownAllComponents()
                            .remove();
                    }
                });

                this.renderConfigurations(configPathPromises);
            });
        });

        this.getDivForPath = function(path) {
            return this.popover.find('.popover-content > div').filter(function() {
                return ($(this).data('path') === path);
            });
        }

        this.renderConfigurations = function(promises) {
            var self = this,
                item = this.attr.item;

            Promise.all(promises).done(function(loaded) {
                var root = self.popover.find('.popover-content');

                loaded.forEach(function(promise) {
                    var path = promise.shift(),
                        Component = promise.shift();
                    Component.attachTo(
                        $('<div>')
                            .data('path', path)
                            .appendTo(root),
                        {
                            extension: self.extension,
                            report: item.configuration.report || self.extension.report,
                            item: item
                        }
                    );
                })
                self.positionDialog();
            })
        };

    }
});


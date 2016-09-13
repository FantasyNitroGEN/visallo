define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'util/popovers/withPopover',
    'util/component/attacher'
], function(
    defineComponent,
    registry,
    withPopover,
    Attacher) {
    'use strict';

    const reportConfigurationPath = 'dashboard/configs/report';

    var reportRenderers = registry.extensionsForPoint('org.visallo.web.dashboard.reportrenderer'),
        extensions = registry.extensionsForPoint('org.visallo.web.dashboard.item');

    return defineComponent(ConfigPopover, withPopover);

    function ConfigPopover() {

        this.before('teardown', function() {
            this.popover.find('.popover-content').children().each(function() {
                Attacher().node(this).teardown();
            })
            this.$node.closest('.card-toolbar').removeClass('active');
        })

        this.before('initialize', function(node, config) {
            config.template = '/dashboard/configureTpl';
            var paths = config.configurationPaths || [],
                extension = _.findWhere(extensions, { identifier: config.item.extensionId }),
                report = config.item.configuration.report || extension.report,
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

                this.$node.closest('.card-toolbar').addClass('active');
                this.on(this.popover, 'redirectEventToItem', function(event, data) {
                    this.$node.closest('.grid-stack-item').find('.item-content').trigger(data.name, data.data);
                });
                this.on(this.$node.closest('.grid-stack-item').find('.item-content'), 'redirectEventToConfiguration', function(event, data) {
                    this.popover.find('.popover-content > div').trigger(data.name, data.data);
                })
                this.on(this.popover, 'configurationChanged', this.onConfigurationChanged);
                this.renderConfigurations(configPathPromises);
            });
        });

        this.onConfigurationChanged = function(event, data) {
            this.trigger('configurationChanged', data);

            var extension = this.extension,
                reportAdded = data.item.configuration.report || extension.report,
                reportRemoved = !data.item.configuration.report && !extension.report;
            if (reportAdded) {
                this.teardownConfigPath(reportConfigurationPath)
                this.renderConfigurations([
                    Promise.all([reportConfigurationPath, Promise.require(reportConfigurationPath)])
                ]);
            } else if (reportRemoved) {
                this.teardownConfigPath(reportConfigurationPath)
            }
        };

        this.teardownConfigPath = function(path) {
            return this.popover.find('.popover-content > div').filter(function() {
                return ($(this).data('path') === path);
            }).each(function() {
                Attacher().node(this).teardown();
                $(this).empty();
            });
        }

        this.renderConfigurations = function(promises) {
            var self = this,
                item = this.attr.item;

            Promise.all(promises).done(function(loaded) {
                var root = self.popover.find('.popover-content');

                Promise.all(loaded.map(function(promise) {
                    var path = promise.shift(),
                        Component = promise.shift(),
                        node = $('<div>').data('path', path).appendTo(root);

                    return Attacher().node(node)
                        .component(Component)
                        .params({
                            extension: self.extension,
                            report: item.configuration.report || self.extension.report,
                            item: item
                        })
                        .behavior({
                            configurationChanged: function(attacher, data) {
                                self.onConfigurationChanged(null, data);
                                // Only if react, update props
                                if (attacher._reactElement) {
                                    attacher.params(data).attach();
                                }
                                self.attr.item = data.item;
                            }
                        })
                        .attach();
                })).then(function() {
                    self.positionDialog();
                })
            })
        };

    }
});


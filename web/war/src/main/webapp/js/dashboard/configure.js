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

    var reportRenderers = registry.extensionsForPoint('org.visallo.dashboard.reportrenderer'),
        extensions = registry.extensionsForPoint('org.visallo.dashboard.item');

    return defineComponent(ConfigPopover, withPopover);

    function ConfigPopover() {

        this.before('teardown', function() {
            this.popover.find('.popover-content').children().each(function() {
                Attacher().node(this).teardown();
            })
            this.$node.closest('.card-toolbar').removeClass('active');
        })

        this.before('initialize', function(node, config) {
            config.template = '/dashboard/configureTpl.hbs';
            var paths = config.configurationPaths || [],
                extension = _.findWhere(extensions, { identifier: config.item.extensionId }),
                report = config.item.configuration.report || extension.report,
                addDefaultConfiguration = !extension.options ||
                    extension.options.preventDefaultConfig !== true;

            this.extension = extension;
            this.components = [];

            if (extension.configurationPath) {
                /**
                 * FlightJS or React component that renders card configuration
                 * content.
                 *
                 * For Flight, `trigger` an event with the name of the
                 * function instead of invoking directly.
                 *
                 * @typedef org.visallo.dashboard.item~ConfigComponent
                 * @property {object} extension
                 * @property {object} item
                 * @property {object} [report]
                 * @property {org.visallo.dashboard.item~configurationChanged} configurationChanged Change the configuration
                 */
                paths.push(extension.configurationPath);
            }

            if (report) {
                paths.push(reportConfigurationPath);
            }

            if (addDefaultConfiguration) {
                paths.splice(0, 0, 'dashboard/configs/default');
            }

            config.empty = paths.length === 0;

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
                this.renderConfigurations(paths);
            });
        });

        this.onConfigurationChanged = function(event, data) {
            this.trigger('configurationChanged', data);

            if (data.options && data.options.changed === 'item.title') return;

            var extension = this.extension,
                reportAdded = data.item.configuration.report || extension.report,
                reportRemoved = !data.item.configuration.report && !extension.report;

            this.attr.item = data.item;

            if (reportAdded) {
                this.teardownConfigPath(reportConfigurationPath)
                this.renderConfigurations([reportConfigurationPath]).then(() => this.updateComponents(data));
            } else if (reportRemoved) {
                this.teardownConfigPath(reportConfigurationPath)
                this.updateComponents(data);
            } else {
                this.updateComponents(data);
            }
        };

        this.updateComponents = function(data) {
            this.components.forEach((attacher) => {
                attacher.params(data).attach({
                   teardown: true,
                   teardownOptions: {
                      react: false
                   }
                });
            });
        };

        this.teardownConfigPath = function(path) {
            this.components = _.chain(this.components)
                .map((attacher) => {
                    if (attacher.path() === path) {
                        attacher.teardown();
                        attacher.node().remove()
                        return null;
                    } else {
                        return attacher;
                    }
                })
                .compact()
                .value();
        }

        this.renderConfigurations = function(paths) {
            const item = this.attr.item;
            const root = this.popover.find('.popover-content');

            return Promise.map(paths, (path) => {
                const node = $('<div>').data('path', path).appendTo(root);

                return Attacher().node(node)
                    .path(path)
                    .params({
                        extension: this.extension,
                        report: item.configuration.report || this.extension.report,
                        item: item
                    })
                    .behavior({
                        configurationChanged: (attacher, data) => {
                            this.onConfigurationChanged(null, data);
                        }
                    })
                    .attach();
            }).then((components) => {
               this.components.push(...components);
               this.positionDialog();
            });
        };

    }
});


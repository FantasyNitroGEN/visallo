define([
    'flight/lib/component',
    'configuration/plugins/registry',
    './reportTpl.hbs'
], function(
    defineComponent,
    registry,
    template) {
    'use strict';

    return defineComponent(ReportConfiguration);

    function ReportConfiguration() {

        this.defaultAttrs({
            typeSelector: 'select.reportType',
            configSelector: '.custom_report_config'
        });

        this.after('initialize', function() {
            var self = this,
                configuration = this.attr.item.configuration || {},
                renderers = registry.extensionsForPoint('org.visallo.web.dashboard.reportrenderer');

            this.on('change', {
                typeSelector: this.onChange
            })

            this.$node.html(template({
                extension: this.attr.extension,
                item: this.attr.item
            }));

            var reportRenderer = _.findWhere(renderers, { identifier: configuration.reportRenderer });
            if (reportRenderer && reportRenderer.configurationPath) {
                require([reportRenderer.configurationPath], function(Config) {
                    Config.attachTo(self.select('configSelector'), {
                        extension: self.attr.extension,
                        item: self.attr.item
                    })
                })
            }

            this.on('reportResults', function(event, data) {
                var $select = this.select('typeSelector'),
                    validReportRenderers = _.filter(registry.extensionsForPoint('org.visallo.web.dashboard.reportrenderer'), function(e) {
                        try {
                            return e.supportsResponse(data.results);
                        } catch(error) {
                            console.error(error);
                        }
                    });

                if (validReportRenderers.length) {
                    $select.html($.map(validReportRenderers, function(r) {
                        return $('<option>')
                            .val(r.identifier)
                            .text(r.label)
                            .prop('selected', r.identifier === configuration.reportRenderer)
                    })).prop('disabled', false);
                } else {
                    $select.html($('<select>').text('No Supported Visualizations Found'))
                        .prop('disabled', true)
                }
            })
            this.trigger('redirectEventToItem', {
                name: 'getReportResults'
            });
        });

        this.onChange = function(event) {
            this.attr.item.configuration.reportRenderer = $(event.target).val();

            this.trigger('configurationChanged', {
                extension: this.attr.extension,
                item: this.attr.item
            });
        };
    }
});

define([
    'flight/lib/component',
    './withRenderer',
    'configuration/plugins/registry'
], function(
    defineComponent,
    withRenderer,
    registry) {
    'use strict';

    return defineComponent(UnknownType, withRenderer);

    function UnknownType() {
        this.render = function(d3, node, data) {
            var self = this,
                reportRenderers = registry.extensionsForPoint('org.visallo.web.dashboard.reportrenderer'),
                renderer = _.find(reportRenderers, function(reportRenderer) {
                    try {
                        return reportRenderer.supportsResponse(data);
                    } catch(error) {
                        console.error(error);
                    }
                })

            if (renderer) {
                this.attr.item.configuration.reportRenderer = renderer.identifier;
                this.trigger('configurationChanged', {
                    extension: this.attr.extension,
                    item: this.attr.item
                });
                //require([renderer.componentPath], function(Renderer) {
                    //Renderer.attachTo(self.node, _.extend({}, self.attr, {
                        //result: data
                    //}));
                    //self.teardown();
                //})
            } else {
                this.trigger('showError', 'No renderer found that supports response', data);
            }
        }
    }

});

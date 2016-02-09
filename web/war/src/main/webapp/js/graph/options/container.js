define([
    'flight/lib/component',
    'configuration/plugins/registry'
], function(
    defineComponent,
    registry) {
    'use strict';

    return defineComponent(GraphOptions);

    function GraphOptions() {

        this.after('initialize', function() {
            registry.documentExtensionPoint('org.visallo.graph.options',
                'Add components to graph options dropdown',
                function(e) {
                    return ('identifier' in e) && ('optionComponentPath' in e);
                }
            );
            registry.registerExtension('org.visallo.graph.options', {
                identifier: 'toggleEdgeLabel',
                optionComponentPath: 'graph/options/edgeLabel'
            });
            registry.registerExtension('org.visallo.graph.options', {
                identifier: 'toggleSnapToGrid',
                optionComponentPath: 'graph/options/snapToGrid'
            });
            registry.registerExtension('org.visallo.graph.options', {
                identifier: 'togglePanOrSelect',
                optionComponentPath: 'graph/options/panOrSelect'
            });

            var self = this,
                $options = $(),
                components = registry.extensionsForPoint('org.visallo.graph.options')
                    .map(function(option) {
                        return Promise.require(option.optionComponentPath);
                    });

            if (!components.length) {
                return;
            }

            self.attr.cy.done(function(cy) {
                Promise.all(components).done(function(Components) {
                    Components.forEach(function(Component) {
                        var $node = $('<li>');
                        $options = $options.add($node);
                        Component.attachTo($node, {
                            cy: cy
                        });
                    });
                    $('<ul>')
                        .append($options)
                        .appendTo(
                            self.$node.empty()
                        )
                });
            });

        });

    }
});

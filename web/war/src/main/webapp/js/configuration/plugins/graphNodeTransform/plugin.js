define(['../registry'], function(registry) {
    'use strict';

    return {
        registerClassModifier: function(handler) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.node.class', handler);
        },

        registerNodeDataTransformer: function(handler) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.node.transformer', handler);
        }
    };
});

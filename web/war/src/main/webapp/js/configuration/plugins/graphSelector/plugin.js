define(['../registry'], function(registry) {
    'use strict';

    return {
        registerGraphSelector: function(identifier, selector, visibility) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.selection', selector);
        }
    };
});

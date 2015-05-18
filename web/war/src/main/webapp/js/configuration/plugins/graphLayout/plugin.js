define(['../registry'], function(registry) {
    'use strict';

    var layouts = [];

    return {
        layouts: layouts,

        registerGraphLayout: function(name, layout) {
            layout.identifier = name;
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.layout', layout);
        }
    };
});

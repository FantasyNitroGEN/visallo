define(['../registry'], function(registry) {
    'use strict';

    return {
        registerGraphView: function(viewComponentPath, viewClassName /*optional*/) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.view', {
                componentPath: viewComponentPath,
                className: viewClassName || ''
            });
        }
    };
});

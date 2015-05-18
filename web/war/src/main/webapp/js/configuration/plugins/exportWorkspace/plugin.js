define(['../registry'], function(registry) {
    'use strict';

    return {
        registerWorkspaceExporter: function(exporter) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.export', exporter);
        }
    };
});

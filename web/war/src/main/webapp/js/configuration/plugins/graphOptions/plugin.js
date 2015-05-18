define(['../registry'], function(registry) {
    'use strict';

    var api = {
            registerGraphOption: function(option) {
                console.warn('Deprecated: Use registry instead')
                registry.registerExtension('org.visallo.graph.options', option);
            }
        };

    return api;
});

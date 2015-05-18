define(['../registry'], function(registry) {
    'use strict';

    return {
        registerGraphStyler: function(styler) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.graph.style', styler);
        }
    };
});

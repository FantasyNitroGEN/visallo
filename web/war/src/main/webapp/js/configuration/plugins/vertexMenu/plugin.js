define(['../registry'], function(registry) {
    'use strict';

    return {
        DIVIDER: 'DIVIDER',
        registerVertexMenuItem: function(item) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.vertex.menu', item);
        }
    };
});

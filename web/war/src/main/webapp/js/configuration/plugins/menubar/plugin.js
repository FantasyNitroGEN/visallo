define(['../registry'], function(registry) {
    'use strict';

    return {
        registerMenubarItem: function(item) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.menubar', item);
        }
    };
});

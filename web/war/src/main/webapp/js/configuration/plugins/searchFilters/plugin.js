define(['../registry'], function(registry) {
    'use strict';

    return {
        registerSearchFilter: function(item) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.search.filter', item);
        }
    };
});

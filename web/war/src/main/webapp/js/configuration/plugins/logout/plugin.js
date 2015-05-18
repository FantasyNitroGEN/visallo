define(['../registry'], function(registry) {
    'use strict';

    return {
        registerLogoutHandler: function(handler) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.logout', handler);
        }
    };
});

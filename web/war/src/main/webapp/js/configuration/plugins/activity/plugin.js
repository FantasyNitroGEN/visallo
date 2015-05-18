define(['../registry'], function(registry) {
    'use strict';

    var api = {
            registerActivityHandler: function reg(handler) {
                console.warn('Deprecated: Use registry instead')
                registry.registerExtension('org.visallo.activity', handler);
            },

            registerActivityHandlers: function(handlers) {
                handlers.forEach(function(h) {
                    api.registerActivityHandler(h);
                })
            }
        };

    return api;
});

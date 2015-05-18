define(['../registry'], function(registry) {
    'use strict';

    return {
        registerWebsocketHandler: function(name, handler) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.websocket.message', {
                name: name,
                handler: handler
            });
        }
    };
});

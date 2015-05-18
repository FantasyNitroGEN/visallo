define([], function() {
    'use strict';
    return function(message) {
        if (message.close === true) {
            closeSocket();
        } else if (typeof atmosphere !== 'undefined') {
            publicData.atmosphereConfiguration = message.configuration;
            // TODO: add sourceId to filter current users requests
            publicData.socket = atmosphere.subscribe(_.extend(message.configuration, {
                onOpen: function(response) {
                    if (atmosphere.util.__socketPromiseFulfill) {
                        atmosphere.util.__socketPromiseFulfill(publicData.socket);
                    } else {
                        atmosphere.util.__socketOpened = true;
                    }
                },
                onError: function(request) {
                    dispatchMain('websocketStateOnError', {
                        reason: request.reasonPhrase,
                        error: request.error
                    });
                },
                onClose: function(request) {
                    atmosphere.util.__socketOpened = false;
                    dispatchMain('websocketStateOnClose', {
                        reason: request.reasonPhrase,
                        error: request.error,
                        status: request.status
                    });
                },
                onMessage: function(response) {
                    processMainMessage({
                        type: 'websocketMessage',
                        responseBody: response.responseBody
                    });
                }
            }));
        }
    };
})

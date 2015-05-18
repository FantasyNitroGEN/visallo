define(['util/websocket'], function(websocketUtils) {
    'use strict';

    return withWebsocket;

    function withWebsocket() {

        var overlayPromise = new Promise(function(fulfill, reject) {
            this.after('initialize', function() {
                _.defer(function() {
                    Promise.require('util/offlineOverlay').done(fulfill);
                })
            })
        }.bind(this));

        this.after('initialize', function() {
            var self = this;
            this.on('applicationReady currentUserVisalloDataUpdated', function() {
                if (!visalloData.socketSourceGuid) {
                    self.setPublicApi('socketSourceGuid', websocketUtils.generateSourceGuid());
                    self.worker.postMessage({
                        type: 'atmosphereConfiguration',
                        configuration: this.getAtmosphereConfiguration()
                    })
                }
            });

            if (window.DEBUG) {
                DEBUG.pushSocket = this.pushSocket.bind(this);
            }
        });

        this.pushSocket = function(message) {
            this.worker.postMessage({
                type: 'websocketSend',
                message: message
            });
        };

        this.rebroadcastEvent = function(message) {
            this.trigger(message.eventName, message.data);
        };

        this.getAtmosphereConfiguration = function() {
            // https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-atmosphere.js-API
            return {
                url: location.origin + location.pathname.replace(/jsc.*$/, '') + 'messaging',
                transport: 'websocket',
                fallbackTransport: 'long-polling',
                contentType: 'application/json',
                trackMessageLength: true,
                suspend: true,
                shared: false,
                pollingInterval: 5000,
                connectTimeout: -1,
                enableProtocol: true,
                maxReconnectOnClose: 2,
                maxStreamingLength: 2000,
                logLevel: 'warn'
            };
        };

        this.websocketStateOnError = function(error) {
            overlayPromise.done(function(Overlay) {
                // Might be closing because of browser refresh, delay
                // so it only happens if server went down
                _.delay(function() {
                    Overlay.attachTo(document);
                }, 1000);
            });
        };

        this.websocketStateOnClose = function(message) {
            if (message && message.error) {
                console.error('Websocket closed', message.reasonPhrase, message.error);
            } else console.error('Websocket closed', message.status)
        };
    }
});

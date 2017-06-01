define([], function() {
    'use strict';

    var FAST_PASSED = {
        'ontology/ontology': null,
        'ontology/properties': null,
        'ontology/relationships': null,
        'config/properties': null,
        'config/messages': null
    };

    return withDataRequestHandler;

    function fixParameter(obj) {
        if (obj instanceof FileList) {
            return _.map(obj, function(o) {
                return o;
            });
        }

        return obj;
    }

    function deferred() {
        var resolve, reject, promise = new Promise((f, r) => { resolve = f; reject = r; });
        return { promise, resolve, reject };
    }

    function fastPassNoWorker(message, trigger) {
        var path = message.data.service + '/' + message.data.method;
        if (path in FAST_PASSED) {
            if (FAST_PASSED[path]) {
               FAST_PASSED[path].promise.then(r => {
                   trigger(r.type, { ...r, requestId: message.data.requestId });
               })
               return true;
            } else {

                // Special case check for properties/relationship request and
                // resolve using ontology if already requested
                if (message.data.service === 'ontology' && (
                message.data.method === 'properties' || message.data.method === 'relationships'
                )) {
                    if (FAST_PASSED['ontology/ontology']) {
                        FAST_PASSED['ontology/ontology'].promise.then(r => {
                            trigger(r.type, {
                                ...r,
                                result: r.result[message.data.method],
                                requestId: message.data.requestId
                            });
                        })
                        return true;
                    }
                }
                FAST_PASSED[path] = deferred();
            }
        }
        return false;
    }

    function checkForFastPass(message) {
        var path = message.originalRequest.service + '/' + message.originalRequest.method;
        if (FAST_PASSED[path]) {
            FAST_PASSED[path].resolve(message);
        }
    }

    function withDataRequestHandler() {

        this.after('initialize', function() {
            this.on('dataRequest', this.handleDataRequest);
            this.on('dataRequestCancel', this.handleDataRequestCancel);
            this.visalloData.readyForDataRequests = true;
            this.trigger('readyForDataRequests');
        });

        this.handleDataRequestCancel = function(event, data) {
            // TODO
            //this.worker.postMessage({
                //type: 'cancelDataRequest',
                //data: data
            //});
        };

        this.handleDataRequest = function(event, data) {
            var self = this;

            this.trigger('dataRequestStarted', _.pick(data, 'requestId'));

            if (data.parameters) {
                data.parameters = _.map(data.parameters, fixParameter);
            }
            if (data && data.service === 'config') {
                var l = {};
                if (typeof localStorage !== 'undefined') {
                    l.language = localStorage.getItem('language');
                    l.country = localStorage.getItem('country');
                    l.variant = localStorage.getItem('variant');
                    data.parameters.push(l);
                }
            }
            var message = { type: event.type, data };

            if (!fastPassNoWorker(message, this.trigger.bind(this))) {
                this.worker.postMessage(message);
            }
        };

        this.dataRequestCompleted = function(message) {
            checkForFastPass(message);
            this.trigger(message.type, message);
        };

        this.dataRequestProgress = function(message) {
            this.trigger(message.type, message);
        };

        this.dataRequestFastPassClear = function(message) {
            message.paths.forEach(function(path) {
                FAST_PASSED[path] = null;
            })
        };

    }
});

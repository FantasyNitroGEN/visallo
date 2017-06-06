define([], function() {
    'use strict';

    var CACHES = {
        ontology: null
    };
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


    function checkForFastPass(message) {
        var path = message.originalRequest.service + '/' + message.originalRequest.method;
        if (FAST_PASSED[path]) {

            if (path === 'ontology/ontology') {
                CACHES.ontology = message.result;
                message.result = {};
                wrap(message.result, 'ontology', 'concepts')
                wrap(message.result, 'ontology', 'properties')
                wrap(message.result, 'ontology', 'relationships')
            }
            FAST_PASSED[path].resolve(message);
        }
    }

    function wrap(obj, cacheKey, key) {
        Object.defineProperty(obj, key, {
            get: function() {
                return CACHES[cacheKey][key];
            },
            enumerable: true
        });
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

            if (!this.fastPassNoWorker(message)) {
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

        this.fastPassNoWorker = function(message) {
            var path = message.data.service + '/' + message.data.method;
            if (path in FAST_PASSED) {
                if (FAST_PASSED[path]) {
                   FAST_PASSED[path].promise.then(r => {
                       this.trigger(r.type, { ...r, requestId: message.data.requestId });
                   })
                   return true;
                } else {

                    // Special case check for properties/relationship request and
                    // resolve using ontology if already requested
                    if (message.data.service === 'ontology' && (
                    message.data.method === 'properties' || message.data.method === 'relationships'
                    )) {
                        const ontologyPath = 'ontology/ontology';
                        const existing = FAST_PASSED[ontologyPath] && FAST_PASSED[ontologyPath].promise;
                        Promise.resolve(existing || this.refreshOntology()).then(r => {
                            this.trigger(r.type, {
                                ...r,
                                // TODO: decorate with getters
                                result: r.result[message.data.method],
                                requestId: message.data.requestId
                            });
                        })
                        return true;
                    }
                    FAST_PASSED[path] = deferred();
                }
            }
            return false;
        }

        this.refreshOntology = function() {
            return this.dataRequestPromise
                .then(dr => dr('ontology', 'ontology'))
                .then(ontology => {
                    return FAST_PASSED['ontology/ontology'].promise;
                });
        };

        this.dataRequestFastPassClear = function(message) {
            var ontologyCleared = false;
            message.paths.forEach(function(path) {
                ontologyCleared = ontologyCleared || (path.indexOf('ontology') === 0)
                FAST_PASSED[path] = null;
            })

            if (ontologyCleared) {
                this.refreshOntology();
            }
        };

    }
});

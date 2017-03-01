/**
 * Make data requests using events, but wrapped in Promise interface.
 * These requests get routed to the web worker thread where one of
 * the service interfaces will handle the AJAX request and process the
 * response.
 *
 * The service name maps to a module defined in `data/web-worker/services/*`,
 * and the service method is the method to invoke in that modules exported
 * object.
 *
 * See the documented **Services** in the navigation menu on the left.
 *
 * To create custom services in plugins refer to the
 * {@link http://docs.visallo.org/tutorials/webplugin.html|Web Plugin Tutorial}.
 *
 * @module dataRequest
 * @classdesc Invoke service requests on Web Worker
 * @example
 * // Invoke the "me" function in "data/web-worker/services/user.js" module
 * dataRequest('user', 'me')
 *  .then(function(user) {
 *      // The current logged in user info
 *  }).catch(function(err) {
 *    // handle error
 *  }
 */
define([
    'util/promise',
    'underscore',
    'jquery',
    'util/requirejs/promise!util/service/dataPromise'
], function(Promise, _, $)
    /**
     * @alias module:util/withDataRequest
     */
{
    'use strict';

    var NO_DATA_RESPONSE_TIMEOUT_SECONDS = 4,
        currentDataRequestId = 0,
        requests = {};

    $(document)
        .on('dataRequestStarted', function(event, data) {
            var request = requests[data.requestId];
            if (request) {
                clearTimeout(request.timeoutTimer);
            }
        })
        .on('dataRequestProgress', function(event, data) {
            var request = requests[data.requestId];
            if (request) {
                request.promiseFulfill.updateProgress(data.progress);
            }
        })
        .on('dataRequestCompleted', function(event, data) {
            var request = cleanRequest(data.requestId);
            if (request) {
                if (data.success) {
                    request.promiseFulfill(data.result);
                } else {
                    var error = data.error;
                    if (_.isString(error)) {
                        error = new Error(error);
                    }
                    request.promiseReject(error);
                }
            }
        });

    function cleanRequest(requestId) {
        var request = requests[requestId];
        if (request) {
            clearTimeout(request.timeoutTimer);
            delete requests[requestId];
            return request;
        }
    }


    /**
     * Make a data request
     *
     * @name module:util/withDataRequest.dataRequest
     * @function
     * @param {string} service The name of service
     * @param {string} method The method to invoke in service
     * @param {...object} [args] arguments to pass to service
     * @returns {Promise} The service request promise
     */
    function dataRequestFromNode(node, service, method /*, args */) {
        if (!service || !method) {
            throw new Error('Service and method parameters required for dataRequest');
        }

        var argsStartIndex = 3,
            thisRequestId = currentDataRequestId++,
            $node = $(node),
            nodeEl = $node[0],
            $nodeInDom = $.contains(document.documentElement, nodeEl) ? $node : $(document),
            args = arguments.length > argsStartIndex ? _.rest(arguments, argsStartIndex) : [],
            promise = new Promise(function(fulfill, reject, onCancel) {
                Promise.require('util/requirejs/promise!util/service/dataPromise')
                    .then(function() {
                        requests[thisRequestId] = {
                            promiseFulfill: fulfill,
                            promiseReject: reject,
                            timeoutTimer: _.delay(function() {
                                console.error('Data request went unhandled', service + '->' + method);
                                $nodeInDom.trigger('dataRequestCompleted', {
                                    requestId: thisRequestId,
                                    success: false,
                                    error: 'No data request handler responded for ' + service + '->' + method
                                })
                            }, NO_DATA_RESPONSE_TIMEOUT_SECONDS * 1000)
                        };

                        $nodeInDom.trigger('dataRequest', {
                            requestId: thisRequestId,
                            service: service,
                            method: method,
                            parameters: args
                        });
                    })
                onCancel(function() {
                    var request = cleanRequest(thisRequestId);
                    if (request) {
                        $nodeInDom.trigger('dataRequestCancel', {
                            requestId: thisRequestId
                        });
                    }
                })
            });

        return promise;
    }
    withDataRequest.dataRequest = _.partial(dataRequestFromNode, document);

    return withDataRequest;


    function withDataRequest() {

        if (!('dataRequest' in this)) {
            this.dataRequest = function(service, method /*, args */) {
                Array.prototype.splice.call(arguments, 0, 0, this.$node);
                return dataRequestFromNode.apply(this, arguments);
            }
        }
    }
});

/*global self:true*/
/*global onmessage:true*/
/*global console:true*/
/*global publicData:true*/
/*global store:false*/
/*global BASE_URL:true*/
/*global importScripts:false*/

/*eslint strict:0*/
var BASE_URL = '../../..',
    self = this,
    cacheBreaker = null,
    publicData = {};

onmessage = function(event) {
    if (!cacheBreaker) {
        cacheBreaker = event.data;
        setupAll();
        return;
    }

    require([
        'underscore',
        'util/promise',
        'data/web-worker/util/store'
    ], function(_, Promise, store) {
        self.store = store;
        onMessageHandler(event);
    })
};

function setupAll() {
    setupConsole();
    setupWebsocket();
    setupRequireJs();
    documentExtensionPoints();
}

function setupConsole() {
    var noop = function() {};

    if (typeof console === 'undefined') {
        console = {
            log: log('log'),
            info: log('info'),
            debug: log('debug'),
            error: log('error'),
            warn: log('warn'),
            group: noop,
            groupCollapsed: noop,
            groupEnd: noop
        };
    }
    function log(type) {
        return function() {
            dispatchMain('brokenWorkerConsole', {
                logType: type,
                messages: Array.prototype.slice.call(arguments, 0).map(function(arg) {
                    return JSON.stringify(arg);
                })
            });
        }
    }
}

function setupWebsocket() {
    var isFirefox = navigator && navigator.userAgent && ~navigator.userAgent.indexOf('Firefox'),
        supportedInWorker = !!(this.WebSocket || this.MozWebSocket) && !isFirefox;

    if (supportedInWorker) {
        self.window = self;
        importScripts(BASE_URL + '/libs/atmosphere-javascript/modules/javascript/target/javascript-2.2.11/javascript/atmosphere-min.js?' + cacheBreaker);
        atmosphere.util.getAbsoluteURL = function() {
            return publicData.atmosphereConfiguration.url;
        }
        self.closeSocket = function() {
            if (publicData.socket) {
                publicData.socket.close();
            }
        }
        self.pushSocketMessage = function(message) {
            Promise.all([
                Promise.require('util/websocket'),
                new Promise(function(fulfill, reject) {
                    if (atmosphere.util.__socketOpened) {
                        fulfill(publicData.socket);
                    }
                    atmosphere.util.__socketPromiseFulfill = fulfill;
                    atmosphere.util.__socketPromiseReject = reject;
                })
            ]).done(function(results) {
                var websocketUtils = results[0],
                    socket = results[1];

                websocketUtils.pushDataToSocket(socket, publicData.socketSourceGuid, message);
            });
        }
    } else {
        dispatchMain('websocketNotSupportedInWorker');
        self.closeSocket = function() {
            dispatchMain('websocketLegacyClose');
        }
        self.pushSocketMessage = function(message) {
            dispatchMain('websocketFromWorker', { message: message });
        }
    }
}

function setupRequireJs() {
    if (typeof File === 'undefined') {
        self.File = Blob;
    }
    if (typeof FormData === 'undefined') {
        importScripts('./util/formDataPolyfill.js?' + cacheBreaker);
    }
    importScripts(BASE_URL + '/jsc/require.config.js?' + cacheBreaker);
    require.baseUrl = BASE_URL + '/jsc/';
    require.urlArgs = cacheBreaker;
    require.deps = ['../plugins-web-worker'];
    importScripts(BASE_URL + '/libs/requirejs/require.js?' + cacheBreaker);
}

function onMessageHandler(event) {
    var data = event.data;
    processMainMessage(data);
}

function processMainMessage(data) {
    if (data.type) {
        require(['data/web-worker/handlers/' + data.type], function(handler) {
            handler(data);
        });
    } else console.warn('Unhandled message to worker', event);
}

function documentExtensionPoints() {
    require(['configuration/plugins/registry'], function(registry) {
        registry.documentExtensionPoint('org.visallo.websocket.message',
            'Add custom websocket message handlers',
            function(e) {
                return ('name' in e) && _.isFunction(e.handler)
            }
        );
    })
}

var lastPost = 0,
    MAX_SEND_RATE_MILLIS = 500,
    postMessageQueue = [],
    drainTimeout;
function dispatchMain(type, message) {
    var now = Date.now(),
        duration = now - lastPost;

    if (drainTimeout) {
        clearTimeout(drainTimeout);
    }

    if (!type) {
        throw new Error('dispatchMain requires type argument');
    }
    message = message || {};
    message.type = type;

    postMessageQueue.push(message);

    if (type === 'rebroadcastEvent' && duration < MAX_SEND_RATE_MILLIS) {
        drainTimeout = setTimeout(drainMessageQueue, MAX_SEND_RATE_MILLIS - duration);
        return;
    }

    drainMessageQueue();
}

function drainMessageQueue() {
    try {
        postMessage(postMessageQueue);
        postMessageQueue.length = 0;
        lastPost = Date.now();
    } catch(e) {
        var jsonString = JSON.stringify(postMessageQueue);
        postMessage({
            type: 'brokenWorkerConsole',
            logType: 'error',
            messages: ['error posting', e.message, jsonString]
        });
    }
}

function ajaxPrefilter(xmlHttpRequest, method, url, parameters) {
    if (publicData) {
        var filters = [
                setWorkspaceHeader,
                setCsrfHeader,
                setGraphTracing
                // TODO: set timezone
            ], invoke = function(f) {
                f();
            };

        filters.forEach(invoke);
    }

    function setWorkspaceHeader() {
        var hasWorkspaceParam = typeof (parameters && parameters.workspaceId) !== 'undefined';
        if (publicData.currentWorkspaceId && !hasWorkspaceParam) {
            xmlHttpRequest.setRequestHeader('Visallo-Workspace-Id', publicData.currentWorkspaceId);
        }
    }
    function setCsrfHeader() {
        var eligibleForProtection = !(/get/i).test(method),
            user = publicData.currentUser,
            token = user && user.csrfToken;

        if (eligibleForProtection && token) {
            xmlHttpRequest.setRequestHeader('Visallo-CSRF-Token', token);
        }
    }
    function setGraphTracing() {
        if (publicData.graphTraceEnable) {
            xmlHttpRequest.setRequestHeader('graphTraceEnable', 'true');
        }
    }
}

function ajaxPostfilter(xmlHttpRequest, jsonResponse, request) {
    if (!jsonResponse) {
        return;
    }

    var params = request.parameters,
        workspaceId = params && params.workspaceId;

    if (!workspaceId) {
        workspaceId = publicData.currentWorkspaceId;
    }

    store.checkAjaxForPossibleCaching(xmlHttpRequest, jsonResponse, workspaceId, request);
}

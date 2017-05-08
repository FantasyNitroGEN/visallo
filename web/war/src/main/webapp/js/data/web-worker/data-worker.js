/*global self:true*/
/*global onmessage:true*/
/*global console:true*/
/*global publicData:true*/
/*global store:false*/
/*global BASE_URL:true*/
/*global importScripts:false*/

/*eslint strict:0*/
this.importScripts('../../../libs/babel-polyfill/dist/polyfill.min.js');

var BASE_URL = '../../..',
    self = this,
    needsInitialSetup = true,
    publicData = {},
    pluginsLoaded = (function() {
        var _resolve, _isFinished = false;
        return {
            promise: new Promise(r => {_resolve = r}),
            isFinished() { return _isFinished; },
            resolve() { _isFinished = true; _resolve() }
        }
    })();

var timer, todo = [], setupInProgress = true;
onmessage = function(event) {
    if (needsInitialSetup) {
        todo = [];
        needsInitialSetup = false;
        setupInProgress = true;
        setupAll(JSON.parse(event.data));
        return;
    }

    if (setupInProgress) {
        todo.push(event);
        return;
    }

    require([
        'underscore',
        'util/promise'
    ], function(_, Promise) {
        onMessageHandler(event);
    })
};

function setupComplete() {
    setupInProgress = false;
    todo.forEach(event => onmessage(event));
}

function setupAll(data) {
    self.visalloEnvironment = data.environment;
    setupConsole();
    setupWebsocket(data);
    var resolveStore;
    publicData.storePromise = new Promise(f => {resolveStore = f});
    setupRequireJs(data, () => {
        pluginsLoaded.resolve();
        documentExtensionPoints();
        setupRedux(data).then(resolveStore);
        setupComplete();
    });
}

function setupRedux(data) {
    return new Promise(resolve => {
        require(['data/web-worker/store'], function(_store) {
            try {
                const store = _store.getStore();
                const state = store.getState();
                dispatchMain('reduxStoreInit', { state });
                resolve(store);
            } catch(e) {
                console.error(e)
                throw e;
            }
        });
    })
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

function setupWebsocket(data) {
    var isFirefox = navigator && navigator.userAgent && ~navigator.userAgent.indexOf('Firefox'),
        supportedInWorker = !!(this.WebSocket || this.MozWebSocket) && !isFirefox;

    if (supportedInWorker) {
        self.window = self;
        importScripts(BASE_URL + '/libs/atmosphere.js/lib/atmosphere.js?' + data.cacheBreaker);
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

function setupRequireJs(data, callback) {
    if (typeof File === 'undefined') {
        self.File = Blob;
    }
    if (typeof FormData === 'undefined') {
        importScripts('./util/formDataPolyfill.js?' + data.cacheBreaker);
    }
    importScripts(BASE_URL + '/jsc/require.config.js?' + data.cacheBreaker);
    require.baseUrl = BASE_URL + '/jsc/';
    require.urlArgs = data.cacheBreaker;
    require.deps = data.webWorkerResources;
    require.callback = callback;
    importScripts(BASE_URL + '/libs/requirejs/require.js?' + data.cacheBreaker);
    require.load = asyncRequireJSLoader
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
        /**
         * Extension to register new listeners for websocket messages. Must be registered in JavaScript file registered with `app.registerWebWorkerJavaScript` in web app plugin.
         *
         * @param {string} name The message name to listen for. Matches the
         * `type` parameter in message json
         * @param {function} handler The function to invoke when messages
         * arrive. Accepts one parameter: `data`
         */
        registry.documentExtensionPoint('org.visallo.websocket.message',
            'Add custom websocket message handlers',
            function(e) {
                return ('name' in e) && _.isFunction(e.handler)
            },
            'http://docs.visallo.org/extension-points/front-end/websocket'
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
                setSourceGuidHeader,
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
    function setSourceGuidHeader() {
        var isUpdate = !(/get/i).test(method),
            guid = publicData.socketSourceGuid;

        if (isUpdate && guid) {
            xmlHttpRequest.setRequestHeader('Visallo-Source-Guid', guid);
        }
    }
    function setGraphTracing() {
        if (publicData.graphTraceEnable) {
            xmlHttpRequest.setRequestHeader('graphTraceEnable', 'true');
        }
    }
}

function asyncRequireJSLoader(context, moduleName, url) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'blob';
    xhr.onload = function(e) {
        if (this.status === 200) {
            var blob = new Blob([this.response], { type: 'text/javascript' });
            var blobURL = URL.createObjectURL(blob);
            importScripts(blobURL);
            URL.revokeObjectURL(blobURL);
            context.completeLoad(moduleName);
        } else {
            context.onError(new Error('Require for ' + moduleName + ' failed at ' + url));
        }
    };
    xhr.onerror = function() {
        context.onError(new Error('Require for ' + moduleName + ' failed at ' + url));
    }
    xhr.send();
}

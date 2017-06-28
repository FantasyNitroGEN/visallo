define([
    'redux',
    'fast-json-patch'
], function(
    redux,
    jsonpatch) {
    'use strict';

    return withReduxStore;

    function withReduxStore() {

        this.before('initialize', function() {
            visalloData.storePromise = new Promise(done => { this.storeReady = done });
        })

        this.reduxStoreInit = function(message) {
            var initialState = message.state,
                devTools = _.isFunction(window.devToolsExtension) ? window.devToolsExtension() : null,
                store = redux.createStore(
                    rootReducer(initialState),
                    devTools ?
                        redux.compose(redux.applyMiddleware(webworkerMiddleware(this.worker)), devTools) :
                        redux.applyMiddleware(webworkerMiddleware(this.worker))
                );

            this._reduxStore = store;
            this.setupInitialStoreState();
            this.storeReady(store);
        };

        this.reduxStoreAction = function(message) {
            this._reduxStore.dispatch(message.action);
        };

        this.setupInitialStoreState = function() {
            var self = this;

            loadOntology();
            loadPixelRatio();
            loadConfiguration();

            function loadPixelRatio() {
                require(['util/retina', 'data/web-worker/store/screen/actions'], (retina, screenActions) => {
                    $(document).on('devicePixelRatioChanged', (event, { devicePixelRatio }) => {
                        self._reduxStore.dispatch(screenActions.setPixelRatio(devicePixelRatio))
                    });
                    self._reduxStore.dispatch(screenActions.setPixelRatio(retina.devicePixelRatio))
                })
            }
            function loadConfiguration() {
                require(['data/web-worker/store/configuration/actions'], configActions => {
                    var locale = {};
                    if ('localStorage' in window) {
                        try {
                            locale.language = localStorage.getItem('language');
                            locale.country = localStorage.getItem('country');
                            locale.variant = localStorage.getItem('variant');
                        } catch(e) { /*eslint no-empty:0 */ }
                    }
                    self._reduxStore.dispatch(configActions.get(locale))
                });
            }
            function loadOntology() {
                if (!visalloData.currentUser) {
                    self.on(document, 'currentUserVisalloDataUpdated', function handler() {
                        self.off(document, 'currentUserVisalloDataUpdated', handler);
                        loadOntology();
                    });
                    return;
                }
                const workspaceId = visalloData.currentUser.currentWorkspaceId;
                require(['data/web-worker/store/ontology/actions'], ontologyActions => {
                    self._reduxStore.dispatch(ontologyActions.get(workspaceId))
                });
                if (!workspaceId) {
                    console.error(`Current user doesn't have a currentWorkspaceId property set
That should not be possible as /user/me ensures a workspace is created in that case.`);
                }
            }
        }
    }

    function rootReducer(initialState) {
        return (state, action) => {
            if (!state) {
                return initialState
            }

            var { type, payload } = action;

            switch (type) {
                case 'STATE_APPLY_DIFF': return applyDiff(state, payload);
            }

            return state;
        }
    }

    function webworkerMiddleware(webWorker) {
        return () => (next) => (action) => {
            if (action.meta && action.meta.originator && action.meta.originator === 'webworker') {
                return next(action);
            }

            webWorker.postMessage({
                type: 'reduxStoreActions',
                data: {
                    action: action
                }
            });
        };
    }

    /*
     * Apply diff in a redux safe manner by not mutating existing objects.
     */
    function applyDiff(state, diff) {
        // Need to copy all changed paths, since jsonpatch mutates
        // Straight clone is faster for many changes
        let copy = diff.length > 25 ?
            jsonpatch.deepClone(state) :
            copyChangedPaths(state, diff);

        const newState = jsonpatch.applyPatch(copy, diff).newDocument;

        return newState;
    }

    function copyChangedPaths(tree, patches) {
        var alreadyCopiedObjs = [];

        if (patches.length) {
            tree = copyIfNeeded(alreadyCopiedObjs, tree);
        }
        patches.forEach(function(patch) {
            var obj = tree,
                keys = (patch.path || '').split('/');
            for (var i = 1; i < keys.length; i++) {
                var key = keys[i]
                    .replace(/~1/g, '/')
                    .replace(/~0/g, '~')
                if (key in obj) {
                    obj[key] = copyIfNeeded(alreadyCopiedObjs, obj[key]);
                    obj = obj[key]
                }
            }
        });

        return tree;
    }

    function copyIfNeeded(alreadyCopiedObjs, obj) {
        var cloned = obj;
        if (_.isArray(obj)) {
            if (!alreadyCopied(alreadyCopiedObjs, obj)) {
                cloned = obj.concat([]);
                alreadyCopiedObjs.push(cloned);
            }
        } else if (_.isObject(obj)) {
            if (!alreadyCopied(alreadyCopiedObjs, obj)) {
                cloned = Object.assign({}, obj);
                alreadyCopiedObjs.push(cloned);
            }
        }
        return cloned;
    }

    function alreadyCopied(alreadyCopiedObjs, obj) {
        return _.contains(alreadyCopiedObjs, obj);
    }
});


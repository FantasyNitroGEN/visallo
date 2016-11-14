(function() {
    'use strict';

    define([
        'configuration/plugins/registry',
        'fast-json-patch',
        'redux',
        './store/rootReducer',

        // Middleware
        './store/middleware/actionRouter',
        './store/middleware/thunk',
        './store/middleware/promise',
        './store/middleware/dataRequest',
        //'./store/middleware/logger'
    ], function(registry, jsonpatch, redux, rootReducer, ...middleware) {
        var store;

        return {
            getStore() {
                if (!store) {
                    store = redux.createStore(
                        rootReducer,
                        redux.applyMiddleware(...middleware)
                    );
                    store.subscribe(stateChanged(store.getState()))
                }
                return store;
            },

            getOrWaitForNestedState(getterFn, waitForConditionFn) {
                const check = waitForConditionFn ||
                    (s => {
                        const v = getterFn(s);
                        return !_.isUndefined(v) && !_.isEmpty(v)
                    });

                return Promise.try(function() {
                    var state = store.getState();
                    if (check(state)) {
                        return getterFn(state)
                    } else {
                        return new Promise(done => {
                            const unsubscribe = store.subscribe(() => {
                                const state = store.getState();
                                if (check(state)) {
                                    const newValue = getterFn(store.getState())
                                    unsubscribe();
                                    done(newValue);
                                }
                            })
                        })
                    }
                })
            }
        };

        // Send worker state changes to main thread as JSON-patches
        function stateChanged(initialState) {
            var previousState = initialState;
            return function storeSubscription() {
                var newState = store.getState();
                if (newState !== previousState) {
                    var diff = jsonpatch.compare(previousState, newState);
                    if (diff && diff.length) {
                        previousState = newState;
                        dispatchMain('reduxStoreAction', {
                            action: {
                                type: 'STATE_APPLY_DIFF',
                                payload: diff,
                                meta: {
                                    originator: 'webworker'
                                }
                            }
                        })
                    }
                }
            }
        }
    })
})()

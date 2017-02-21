/*eslint strict:0 */
(function(global) {
define('wait-for-webworker-plugins', [], function() {
    return global.pluginsLoaded.promise;
})

if (!global.pluginsLoaded.isFinished()) {
    console.error(
`Web worker plugins should not require the store using requirejs.  Please check any plugins that require "data/web-worker/store" and replace with "publicData.storePromise"

For example:

    define([
        'data/web-worker/store' //Remove this
    ], function(store) {

        // Add this
        publicData.storePromise.then(store => {
            ...
        })
    })`);
    console.error('Visallo is deadlocked until circular dependency is resolved.')
}

define([
    'configuration/plugins/registry',
    'redux',
    'util/requirejs/promise!wait-for-webworker-plugins',

    // Reducers
    './configuration/reducer',
    './element/reducer',
    './ontology/reducer',
    './panel/reducer',
    './product/reducer',
    './screen/reducer',
    './selection/reducer',
    './undo/reducer',
    './user/reducer',
    './workspace/reducer'

    // Add reducers above, the name of the function will be used as the key
], function(registry, redux, pluginsFinished, ...reducers) {

    registry.markUndocumentedExtensionPoint('org.visallo.store');

    const composeReducers = (reducers) => {
        return (state, payload) => {
            return reducers.reduce((previous, fn) => {
                const returnedState = fn(previous, payload);
                if (!returnedState) {
                    console.warn('No state returned from reducer, ignoring', fn);
                    return previous;
                }
                return returnedState;
            }, state)
        }
    }
    const reducerExtensions = registry.extensionsForPoint('org.visallo.store');
    const reducersByKey = _.groupBy(reducerExtensions, 'key');
    const baseReducers = _.object(
        reducers.map(reducerFn => {
            const { name } = reducerFn;
            if (_.isUndefined(name)) {
                throw new Error('Undefined name for reducer: ' + reducerFn);
            }
            if (name in reducersByKey) {
                const reducers = _.pluck(reducersByKey[name], 'reducer');
                reducers.splice(0, 0, reducerFn);
                delete reducersByKey[name];
                return [name, composeReducers(reducers)]
            }

            return [name, reducerFn]
        })
    );
    const rootExtensions = reducersByKey[undefined];
    const rootReducerExtensions = rootExtensions ? _.pluck(rootExtensions, 'reducer') : [];
    delete reducersByKey[undefined];

    const reducerMap = redux.combineReducers({
        ...baseReducers,
        ..._.mapObject(reducersByKey, extensions => {
            const reducers = extensions.map(e => e.reducer)
            return reducers.length === 1 ? reducers[0] : composeReducers(reducers)
        })
    });

    return composeReducers([reducerMap, ...rootReducerExtensions]);
});
})(this);

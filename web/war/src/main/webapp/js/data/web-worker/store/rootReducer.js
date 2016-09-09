/*eslint strict:0 */
define([
    'configuration/plugins/registry',
    'redux',

    // Reducers
    './configuration/reducer',
    './element/reducer',
    './ontology/reducer',
    './panel/reducer',
    './product/reducer',
    './screen/reducer',
    './selection/reducer',
    './user/reducer',
    './workspace/reducer'

    // Add reducers above, the name of the function will be used as the key
], function(registry, redux, ...reducers) {

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

    const reducerMap = redux.combineReducers({ ...baseReducers, ...reducersByKey });

    return composeReducers([reducerMap, ...rootReducerExtensions]);
});

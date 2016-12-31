/**
 * Route actions with type: ROUTE_TO_WORKER_ACTION,
 * to a method supplied in the module `meta.workerImpl`.
 *
 * Allows actions to be dispatched that have complex behavior
 * to be processed by redux-thunk/promise, etc.
 */
define(['../actions'], function(actions) {
    return ({ getState }) => (next) => (action) => {
        if (_.isFunction(action)) return next(action);

        var { type, payload, meta } = action;
        if (type === 'ROUTE_TO_WORKER_ACTION' && meta) {

            var { workerImpl, name } = meta;

            if (workerImpl && name) {
                require([workerImpl], function(worker) {
                    if (name in worker) {
                        var impl = worker[name];
                        if (_.isFunction(impl)) {
                            var result = impl(payload);
                            if (result) {
                                next(result)
                            }
                        } else {
                            next(impl)
                        }
                    } else {
                        throw new Error('Action dispatched with no matching worker impl: ' + name + ', worker = ' + workerImpl)
                    }
                }, function(error) {
                    console.error('Action dispatched with worker that got error: ', error);
                    throw error;
                })
            } else {
                throw new Error('workerImpl and name required in meta for type = ' + type)
            }
        } else {
            return next(action);
        }
    }
})

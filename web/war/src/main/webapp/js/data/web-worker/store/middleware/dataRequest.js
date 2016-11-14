define(['../actions'], function(storeActions) {
    'use strict';

    return dataRequest;

    // Transforms dataRequest actions into promise chain which
    // dispatches multiple actions about the state of request
    function dataRequest({ dispatch }) {
        return function (next) {
            return function (action) {
                if (storeActions.isValidAction(action)) {
                    if (action.type === 'dataRequest') {
                        const { service, name, params = [] } = action.payload;
                        const prefix = [service, name, 'dataRequest'].join('_');
                        return Promise.require(`data/web-worker/services/${service}`)
                            .then(function(service) {
                                dispatch({
                                    type: prefix + 'Loading',
                                    payload: { ...action.payload }
                                })
                                return service[name].apply(null, params)
                            })
                            .then(function(result) {
                                dispatch({
                                    type: prefix + 'Success',
                                    payload: { ...action.payload, result }
                                })
                            })
                            .catch(function(error) {
                                dispatch({
                                    type: prefix + 'Failure',
                                    payload: { ...action.payload, error }
                                })
                            })
                    }
                }

                next(action);
            };
        };

        function isPromise(obj) {
            return obj && _.isFunction(obj.then);
        }
    }
})

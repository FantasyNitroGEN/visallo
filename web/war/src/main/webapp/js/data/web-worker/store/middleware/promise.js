// Similar to redux-promise but without es6 deps
// (lodash, flux-standard-action)
define(['../actions'], function(storeActions) {
    'use strict';

    return function reduxPromise({ dispatch }) {
        return function (next) {
            return function (action) {
                if (!storeActions.isValidAction(action)) {
                    return isPromise(action) ? action.then(dispatch) : next(action);
                }

                return isPromise(action.payload) ? action.payload.then(function(result) {
                    return dispatch({ ...action, payload: result });
                }, function (error) {
                    return dispatch({ ...action, payload: error, error: true });
                }) : next(action);
            };
        };

        function isPromise(obj) {
            return obj && _.isFunction(obj.then);
        }
    }
});


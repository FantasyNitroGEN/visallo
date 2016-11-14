define(['data/web-worker/store', '../store/actions'], function(store, actions) {
    'use strict';

    return function(message) {
        var action = message.data.action;
        if (actions.isValidAction(action)) {
            store.getStore().dispatch(action);
        } else {
            console.error('Store action is invalid: ', action);
        }
    };

});

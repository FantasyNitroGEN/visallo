define(['underscore'], function(_) {
    'use strict';

    var store = {},
        componentsByIdentifier = {},
        update = _.debounce(updateImmediately, 100),
        api = {
            execNowAndOnUpdate: function(identifier, callback) {
                componentsByIdentifier[identifier] = callback;
                callback(store);
            },
            removeCallback: function(identifier) {
                delete componentsByIdentifier[identifier];
            },
            update: function(key, value) {
                var trigger = true;
                if (key in store && _.isEqual(store[key], value)) {
                    trigger = false;
                }
                store[key] = value;

                if (trigger) {
                    update();
                }
            },
            replace: function(replaced) {
                var trigger = !_.isEqual(replaced, store);
                store = replaced;
                if (trigger) {
                    update();
                }
            }
        };

    return api;

    function updateImmediately() {
        var cloned = Object.assign({}, store);
        _.each(componentsByIdentifier, function(callback, identifier) {
            callback(cloned);
        });
    }
});

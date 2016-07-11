define([
    'underscore',
    'configuration/plugins/registry'
], function(_, registry) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.store.updater',
        'Registry a store updater that can populate and keep the store fresh',
        function(e) { return true }
    )

    var store = {},
        componentsByIdentifier = {},
        registeredListenersByIdentifier = {},
        update = _.debounce(updateImmediately, 100),
        updateChangedKeys = [],
        api = {
            _clear: function() {
                store = {};
                componentsByIdentifier = {};
                registeredListenersByIdentifier = {};
            },
            execNowAndOnUpdate: function(identifier, callback) {
                if (!_.isString(identifier) || identifier.length === 0) {
                    throw new Error('Identifier [String] is required');
                }
                if (!_.isFunction(callback)) {
                    throw new Error('Callback [Function] is required');
                }
                if (identifier in componentsByIdentifier) {
                    throw new Error('Identifier must be unique and already used: ' + identifier)
                }
                componentsByIdentifier[identifier] = callback;
                execCallbackForIdentifier(identifier);
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
                    updateChangedKeys.push(key)
                    update();
                }
            },
            replace: function(replaced) {
                var trigger = !_.isEqual(replaced, store);
                store = replaced;
                if (trigger) {
                    updateChangedKeys.push(key)
                    update();
                }
            }
        };

    return api;


    function createRegisterForStoreUpdaters(identifier) {
        return function registerForStoreUpdater(updaterKey, configuration) {
            var updaters = registry.extensionsForPoint('org.visallo.store.updater'),
                cache = registeredListenersByIdentifier,
                oldKeys = cache[identifier] || (cache[identifier] = {});

            if (updaterKey in oldKeys) {
                oldKeys[updaterKey]++;
            } else {
                oldKeys[updaterKey] = 1;
            }

            var updater = _.findWhere(updaters, { identifier: updaterKey })
            if (updater) {
                updater.activate(_.partial(api.update, updaterKey));
            }
            // TODO: deactivate not needed.
        }
    }

    function createUnregisterForStoreUpdaters(identifier) {
        return function unregisterForStoreUpdater(updaterKey) {
            var cache = registeredListenersByIdentifier,
                oldKeys = cache[identifier] || (cache[identifier] = {});

            if (updaterKey in oldKeys) {
                oldKeys[updaterKey]--;
            }

            if (oldKeys[updaterKey] === 0) {
                delete oldKeys[updaterKey];
            }
        }
    }

    function execCallbackForIdentifier(identifier) {
        var listeningForKeys = registeredListenersByIdentifier[identifier],
            trimmedStore = _.pick(store, _.keys(listeningForKeys));

        trimmedStore.registerForStoreUpdater = createRegisterForStoreUpdaters(identifier);
        trimmedStore.unregisterForStoreUpdater = createUnregisterForStoreUpdaters(identifier);
        componentsByIdentifier[identifier](trimmedStore);
    }

    function updateImmediately() {
        _.each(componentsByIdentifier, function(callback, identifier) {
            var registered = registeredListenersByIdentifier[identifier],
                registeredKeys = Object.keys(registered),
                registeredKeyWasChanged = _.intersection(registeredKeys, updateChangedKeys).length;

            if (registeredKeyWasChanged) {
                execCallbackForIdentifier(identifier);
            }
        });
        updateChangedKeys.length = 0;
    }
});

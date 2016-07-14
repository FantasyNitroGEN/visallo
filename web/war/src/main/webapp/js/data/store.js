define([
    'underscore',
    'configuration/plugins/registry'
], function(_, registry) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.store.updater',
        'Registry a store updater that can populate and keep the store fresh',
        function(e) { return true }
    )

    var MAX_UPDATE_MILLIS = 100,
        DEFAULT_NO_CONFIG = 'DEFAULT_NO_CONFIG',
        _store = {},

        // Throttle calls to update
        update = _.throttle(updateImmediately, MAX_UPDATE_MILLIS),

        // Component is root level of component tree
        _componentsByIdentifier = {},

        // What store updaters does this component (and it's descendants) require
        _updatersRequestedByIdentifier = {},

        // What configuration is being used by updators
        _configurationValuesByUpdater = {},

        // Changed keys / update loop invocation (cleared after called)
        _updateChangedKeys = [],

        api = {
            _clear: function() {
                console.warn('Store clearing is only for testing purposes');

                _store = {};
                _componentsByIdentifier = {};
                _updatersRequestedByIdentifier = {};
            },
            execNowAndOnUpdate: function(identifier, callback) {
                if (!_.isString(identifier) || identifier.length === 0) {
                    throw new Error('Identifier [String] is required');
                }
                if (!_.isFunction(callback)) {
                    throw new Error('Callback [Function] is required');
                }
                if (identifier in _componentsByIdentifier) {
                    throw new Error('Identifier must be unique and already used: ' + identifier)
                }
                _componentsByIdentifier[identifier] = callback;
                execCallbackForIdentifier(identifier);
            },
            removeCallback: function(identifier) {
                delete _componentsByIdentifier[identifier];
            }
        };

    return api;

    function _update(key, value) {
        var trigger = true;
        if (key in _store && _.isEqual(_store[key], value)) {
            trigger = false;
        }
        _store[key] = value;

        if (trigger) {
            _updateChangedKeys.push(key)
            update();
        }
    }

    function createRegisterForStoreUpdaters(identifier) {
        return function registerForStoreUpdater(updaterKey, configuration) {
            var updaters = registry.extensionsForPoint('org.visallo.store.updater'),
                configKey = generateKey(updaterKey, configuration),
                previousUpdaters = _updatersRequestedByIdentifier[identifier] ||
                    (_updatersRequestedByIdentifier[identifier] = {}),
                updaterConfigs = previousUpdaters[updaterKey] ||
                    (previousUpdaters[updaterKey] = {});

            if (configKey in updaterConfigs) {
                updaterConfigs[configKey]++;
            } else {
                updaterConfigs[configKey] = 1;
            }

            var updater = _.findWhere(updaters, { identifier: updaterKey }),
                key = keyForUpdaterAndConfig(updaterKey, configKey);

            if (updater) {
                _.defer(function() {
                    updater.activate(_.partial(_update, key), configuration);
                })
            } else throw new Error('No store updater found for identifier: ' + updaterKey);

            return createRegisterGetResultFunction(key, updaterKey, configuration)
        }
    }

    function createUnregisterForStoreUpdaters(identifier) {
        return function unregisterForStoreUpdater(updaterKey, configuration) {
            if (arguments.length === 1) {
                if (_.isFunction(updaterKey)) {
                    configuration = updaterKey._configuration;
                    updaterKey = updaterKey._updaterIdentifier;
                }
            } else if (arguments.length !== 2) {
                throw new Error('Either a store updator/configuration combo or a store getter function should be passed');
            }

            var configKey = generateKey(updaterKey, configuration),
                previousUpdaters = _updatersRequestedByIdentifier[identifier] ||
                    (_updatersRequestedByIdentifier[identifier] = {}),
                updaterConfigs = previousUpdaters[updaterKey] ||
                    (previousUpdaters[updaterKey] = {});

            if (configKey in updaterConfigs) {
                updaterConfigs[configKey]--;
            }

            if (updaterConfigs[configKey] === 0) {
                delete updaterConfigs[configKey];
            }
        }
    }

    function execCallbackForIdentifier(identifier) {
        var listeningForKeys = registeredKeysForIdentifier(identifier),
            trimmedStore = _.pick(_store, listeningForKeys);

        trimmedStore.registerForStoreUpdater = createRegisterForStoreUpdaters(identifier);
        trimmedStore.unregisterForStoreUpdater = createUnregisterForStoreUpdaters(identifier);
        _componentsByIdentifier[identifier](trimmedStore);
    }

    function updateImmediately() {
        _.each(_componentsByIdentifier, function(callback, identifier) {
            var registeredKeys = registeredKeysForIdentifier(identifier);

            var registeredKeyWasChanged = _.intersection(registeredKeys, _updateChangedKeys).length;

            if (registeredKeyWasChanged) {
                execCallbackForIdentifier(identifier);
            }
        });
        _updateChangedKeys.length = 0;
    }

    function registeredKeysForIdentifier(identifier) {
        var registered = _updatersRequestedByIdentifier[identifier],
            registeredKeys = _.flatten(_.map(registered, function(configs, updaterKey) {
                return _.map(configs, function(count, configKey) {
                    return keyForUpdaterAndConfig(updaterKey, configKey);
                })
            }), true);

        return _.flatten(registeredKeys)
    }

    function keyForUpdaterAndConfig(updaterIdentifier, configKey) {
        return updaterIdentifier + '_' + configKey;
    }

    function generateKey(updaterKey, configuration) {
        if (!configuration) return DEFAULT_NO_CONFIG;

        var previousConfigs = (
                _configurationValuesByUpdater[updaterKey] ||
                (_configurationValuesByUpdater[updaterKey] = [])
            ),
            match = _.find(previousConfigs, function(c) {
                return _.isEqual(c.configuration, configuration);
            });

        if (match) {
            return match.key;
        }

        var key = ['config', previousConfigs.length].join('_');

        _configurationValuesByUpdater[updaterKey] = {
            key: key,
            configuration: configuration
        }

        return key;
    }

    function createRegisterGetResultFunction(key, updaterKey, configuration) {
        var getStoreUpdaterResult = function getStoreUpdaterResult(store) {
            if (!_.isObject(store)) {
                throw new Error('Store object must be provided');
            }
            return store[key];
        };

        // Later we can unregister with this function
        getStoreUpdaterResult._updaterIdentifier = updaterKey;
        getStoreUpdaterResult._configuration = configuration;

        return getStoreUpdaterResult;
    }

});

define([
    'react-dom',
    'react',
    'util/promise'
], function(
    ReactDOM,
    React,
    Promise) {
    'use strict';

    var API_VERSIONS = ['v1'],
        cachedApiVersions = null;

    /**
     * Abstracts the attachment of flight and react nodes
     */
    function Attacher() {}

    ['path', 'component', 'params', 'behavior', 'legacyMapping'].forEach(createSetter);

    Attacher.prototype.node = function(node) {
         this._node = _.isFunction(node.get) ? node.get(0) : node;
         return this;
    };
    Attacher.prototype.mapLegacyBehaviors = function() { };
    Attacher.prototype.verifyState = function() {
        if (!_.isElement(this._node)) {
            throw new Error('Node is not an Element')
        }
        if (!_.isString(this._path) && !this._component) {
            throw new Error('Valid component or path is required')
        }
        if (this._params) {
            if ('visalloApi' in this._params) {
                throw new Error('Refrain from setting visalloApi key in params to avoid collisions');
            }
            if (!_.isObject(this._params) || _.isArray(this._params) || _.isFunction(this._params)) {
                throw new Error('Params must be an object')
            }
        }
    };
    Attacher.prototype.teardown = function() {
        if (!this._node) throw new Error('No node specified');
        ReactDOM.unmountComponentAtNode(this._node);
        $(this._node).teardownAllComponents();
    };
    Attacher.prototype.attach = function(options) {
        var self = this,
            params = _.extend({}, this._params) || {};

        return Promise.try(this.verifyState.bind(this))
            .then(function() {
                return Promise.all([
                    self._component || Promise.require(self._path),
                    cachedApiVersions || (cachedApiVersions = loadApiVersions())
                ]);
            })
            .spread(function(Component, api) {
                params.visalloApi = api;

                if (options && options.teardown) {
                    self.teardown();
                }
                if (options && options.empty) {
                    self._node.textContent = '';
                }
                if (isReact(Component)) {
                    var reactElement = React.createElement(Component, _.extend(params, self._behavior));
                    ReactDOM.render(reactElement, self._node);
                    self._reactElement = reactElement;
                } else {
                    var addedEvents = addLegacyListeners(self._node, self._behavior, self._legacyMapping);
                    Component.attachTo(self._node, params);
                    removeLegacyListenersOnTeardown(self._node, Component, addedEvents)
                    self._flightComponent = Component;
                }
                return self;
            })
    };

    return function() {
        return new Attacher();
    };

    function addLegacyListeners(node, behavior, legacyMapping) {
        var mapping = legacyMapping || {},
            addedEvents = {};
        _.each(behavior, function(callback, name) {
            if (name in mapping) {
                name = mapping[name];
            }
            if (!(name in addedEvents)) {
                addedEvents[name] = function(event, data) {
                    event.stopPropagation();
                    callback(data);
                };
                $(node).on(name, addedEvents[name]);
            }
        })
        return addedEvents;
    }

    function removeLegacyListenersOnTeardown(node, Component, addedEvents) {
        var comp = $(node).lookupComponent(Component)
        if (comp) {
            comp.before('teardown', function() {
                var $node = this.$node;
                _.each(addedEvents, function(handler, name) {
                    $node.off(name, handler);
                })
            })
        }
    }

    function loadApiVersions() {
        return Promise.map(API_VERSIONS, function(version) {
                return Promise.require('public/' + version + '/api')
                    .then(function(api) {
                        return api.connect()
                            .then(function(asyncApi) {
                                var baseApi = _.omit(api, 'connect');
                                return [version, _.extend(baseApi, asyncApi)];
                            });
                    })
            })
            .then(function(apis) {
                return _.object(apis);
            })
    }

    function isReact(Component) {
        return Component.prototype && Component.prototype.isReactComponent;
    }

    function createSetter(name) {
        Attacher.prototype[name] = function(value) {
            this['_' + name] = value;
            return this;
        }
    }
});


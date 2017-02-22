/**
 * This module returns a function that creates a higher-order
 * component that calls `connect` on the {@link module:public/v1/api}
 * and will defer rendering of the component until the promise resolves.
 * In which case the props will contain the
 * {@link module:public/v1/api.connected|connected} components.
 *
 * @module public/connectReact
 * @react Higher-order Component that automatically resolves the `connect`
 * promise from the public API
 * @example
 * define(['react', 'public/v1/api'], function(React, api) {
 *     const MyComponent = React.createClass({
 *         render() {
 *             const { formatters, dataRequest, components } = this.props;
 *             // ...
 *         }
 *     })
 *     const MyConnectedComponent = api.connectReact()(MyComponent)
 *     return MyConnectedComponent
 * })
 */
define([
    'react'
], function(React) {
    'use strict';

    const API_VERSIONS = ['v1'];
    const loadApiPromise = loadApiVersions();

    /**
     * Maps from the available Apis to what the component gets as props.
     *
     * @callback mapApiToProps
     * @param {object} availableApis
     * @param {object} availableApis.v1
     * @returns {object} Api requested
     * @example
     * function(apiVersions) {
     *  return apiVersions['v1']
     * }
     */
    const defaultMapApiToProps = (apiVersions) => {
        var defaultVersion = API_VERSIONS[0];
        return apiVersions[defaultVersion];
    };
    const Connect = (mapApi, Component) => React.createClass({
        getInitialState() {
            return {
                connected: false,
                api: null
            };
        },

        componentWillMount() {
            loadApiPromise
                .then((apiVersions) => {
                    this.setState({
                        connected: true,
                        api: apiVersions
                    });
                });
        },

        displayName: 'Connect(' + getDisplayName(Component) + ')',

        render() {
            const { connected, api } = this.state;

            return connected ? <Component {...this.props} {...mapApi(api)} /> : null;
        }
    });

    /**
     *
     * @memberof module:public/connectReact
     * @see module:public/v1/api.connected
     * @param {mapApiToProps} [mapApiToProps] Passes the latest API to
     * component by default
     * @returns {function} Connect HOC function
     * @example
     * const MyConnectedComponent = api.connectReact()(MyComponent)
     * @example <caption>Custom mapApiToProps</caption>
     * const MyConnectedComponent = api.connectReact(apis => {
     *     return apis['v1']
     * })(MyComponent)
     */
    function connectReact(mapApiToProps) {
        var mapApi = mapApiToProps ? mapApiToProps : defaultMapApiToProps;

        return _.partial(Connect, mapApi);
    }

    return connectReact;

    function loadApiVersions() {
        return Promise.map(API_VERSIONS, (version) => {
                return Promise.require('public/' + version + '/api')
                    .then((api) => {
                        return api.connect()
                            .then((asyncApi) => {
                                var baseApi = _.omit(api, 'connect');
                                return [version, _.extend(baseApi, asyncApi)];
                            });
                    });
            })
            .then((apis) => {
                return _.object(apis);
            })
    }

    function getDisplayName(Component) {
        return Component.displayName || Component.name || 'Component';
    }

});

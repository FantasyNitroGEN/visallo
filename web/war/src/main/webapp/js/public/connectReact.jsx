define([
    'react'
], function(React) {
    'use strict';

    const API_VERSIONS = ['v1'];
    const loadApiPromise = loadApiVersions();
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

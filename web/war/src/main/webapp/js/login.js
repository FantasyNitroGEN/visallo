
define([
    'flight/lib/component',
    'loginTpl.hbs',
    'configuration/plugins/registry',
    'util/withDataRequest',
    'tpl!util/alert',
    'util/requirejs/promise!util/service/propertiesPromise'
], function(
    defineComponent,
    template,
    registry,
    withDataRequest,
    alertTemplate,
    configProperties) {
    'use strict';

    return defineComponent(Login, withDataRequest);

    function Login() {

        this.defaultAttrs({
            authenticationSelector: '.authentication'
        });

        this.before('teardown', function() {
            this.$node.remove();
        });

        this.after('initialize', function() {

            /**
             * Provide custom authentication interface to login users.
             *
             * _Visallo will display an error if:_
             *
             * * No authentication plugins are registered
             * * More than one plugins are registered
             *
             * @param {string} componentPath {@link org.visallo.authentication~Component|Component} that renders the interface to login users
             */
            registry.documentExtensionPoint('org.visallo.authentication',
                'Provides interface for authentication',
                function(e) {
                    return _.isString(e.componentPath);
                },
                'http://docs.visallo.org/extension-points/front-end/authentication'
            );

            this.$node.html(template({ showPoweredBy: configProperties['login.showPoweredBy'] === 'true' }));
            var self = this,
                authPlugins = registry.extensionsForPoint('org.visallo.authentication'),
                authNode = this.select('authenticationSelector'),
                error = '',
                componentPath = '';

            this.on('showErrorMessage', function(event, data) {
                authNode.html(alertTemplate({ error: data.message }));
            })

            if (authPlugins.length === 0) {
                error = 'No authentication extension registered.';
            } else if (authPlugins.length > 1) {
                error = 'Multiple authentication extensions registered. (See console for more info)';
                console.error('Authentication plugins:', authPlugins);
            } else {
                componentPath = authPlugins[0].componentPath;
            }

            if (error) {
                authNode.html(alertTemplate({ error: error }));
            } else if (componentPath) {
                require([componentPath], function(AuthenticationPlugin) {

                    /**
                     * Custom authentication interface. Trigger `loginSucess`
                     * upon successful login.
                     *
                     * Display `errorMessage` property somewhere in interface
                     * if it is non-empty.
                     *
                     * @typedef org.visallo.authentication~Component
                     * @property {string} [errorMessage=''] Error Message to display
                     * @fires org.visallo.authentication#loginSuccess
                     */
                    AuthenticationPlugin.attachTo(authNode, {
                        errorMessage: self.attr.errorMessage || ''
                    });

                    /**
                     * Notify Visallo that user is valid and application should
                     * start.
                     *
                     * Will fail if `/user/me` actually returns `403` errors
                     *
                     * @event org.visallo.authentication#loginSuccess
                     * @example
                     * this.trigger('loginSuccess')
                     */
                });
            }
        });

    }

});

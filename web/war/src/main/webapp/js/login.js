
define([
    'flight/lib/component',
    'tpl!login',
    'configuration/plugins/registry',
    'util/withDataRequest',
    'tpl!util/alert'
], function(
    defineComponent,
    template,
    registry,
    withDataRequest,
    alertTemplate) {
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
            var self = this;

            this.$node.html(template({}));

            registry.documentExtensionPoint('org.visallo.authentication',
                'Provides interface for authentication',
                function(e) {
                    return _.isString(e.componentPath);
                }
            );

            var authPlugins = registry.extensionsForPoint('org.visallo.authentication'),
                authNode = this.select('authenticationSelector'),
                error = '',
                componentPath = '';

            if (authPlugins.length === 0) {
                console.warn('No authentication extension registered, Falling back to old plugin');
                componentPath = 'configuration/plugins/authentication/authentication';
            } else if (authPlugins.length > 1) {
                error = 'Multiple authentication extensions registered. (See console for more)';
                console.error('Authentication plugins:', authPlugins);
            } else {
                componentPath = authPlugins[0].componentPath;
            }

            if (error) {
                authNode.html(alertTemplate({ error: error }));
            } else if (componentPath) {
                require([componentPath], function(AuthenticationPlugin) {
                    AuthenticationPlugin.attachTo(authNode, {
                        errorMessage: self.attr.errorMessage || ''
                    });
                });
            }
        });

    }

});

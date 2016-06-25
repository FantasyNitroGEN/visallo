
define([
    'flight/lib/component',
    'hbs!loginTpl',
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
            registry.documentExtensionPoint('org.visallo.authentication',
                'Provides interface for authentication',
                function(e) {
                    return _.isString(e.componentPath);
                }
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
                    AuthenticationPlugin.attachTo(authNode, {
                        errorMessage: self.attr.errorMessage || ''
                    });
                });
            }
        });

    }

});

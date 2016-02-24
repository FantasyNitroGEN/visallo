define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.visallo.authentication', {
        componentPath: 'org/visallo/web/auth/usernameonly/authentication'
    })
});

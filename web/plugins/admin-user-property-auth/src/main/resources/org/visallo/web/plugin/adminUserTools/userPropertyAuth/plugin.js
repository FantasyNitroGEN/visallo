define([
    'public/v1/api'
], function(visallo) {
    'use strict';

    var adminUserAuthorizationsExtensionPoint = 'org.visallo.admin.user.authorizations';

    visallo.registry.registerExtension(adminUserAuthorizationsExtensionPoint, {
        componentPath: 'org/visallo/web/plugin/adminUserTools/userPropertyAuth/UserAdminAuthorizationPlugin'
    });
});

define([
    'public/v1/api'
], function(visallo) {
    'use strict';

    var adminUserPrivilegesExtensionPoint = 'org.visallo.admin.user.privileges';

    visallo.registry.registerExtension(adminUserPrivilegesExtensionPoint, {
        componentPath: 'org/visallo/web/plugin/adminUserTools/userPropertyPrivileges/UserAdminPrivilegesPlugin'
    });
});

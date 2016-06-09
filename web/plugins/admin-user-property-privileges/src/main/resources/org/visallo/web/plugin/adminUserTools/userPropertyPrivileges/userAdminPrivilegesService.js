define('data/web-worker/services/com-visallo-userAdminPrivileges', ['public/v1/workerApi'], function (workerApi) {
    'use strict';

    var api = {
        userUpdatePrivileges: function (userName, privileges) {
            return workerApi.ajax('POST', '/user/privileges/update', {
                'user-name': userName,
                privileges: _.isArray(privileges) ? privileges.join(',') : privileges
            });
        }
    };

    return api;
});

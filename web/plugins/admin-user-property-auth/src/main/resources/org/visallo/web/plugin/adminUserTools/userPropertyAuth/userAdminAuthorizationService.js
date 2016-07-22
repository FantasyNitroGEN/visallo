define('data/web-worker/services/com-visallo-userAdminAuthorization', ['public/v1/workerApi'], function (workerApi) {
    'use strict';

    var api = {
        userAuthAdd: function(userName, auth) {
            return workerApi.ajax('POST', '/user/auth/add', {
                'user-name': userName,
                auth: auth
            });
        },

        userAuthRemove: function(userName, auth) {
            return workerApi.ajax('POST', '/user/auth/remove', {
                'user-name': userName,
                auth: auth
            });
        }
    };

    return api;
});

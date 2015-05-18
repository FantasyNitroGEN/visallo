define(['../registry'], function(registry) {
    'use strict';

    var api = {
        registerUserAccountPage: function(page) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.user.account.page', page);
        }
    };

    return api;
});

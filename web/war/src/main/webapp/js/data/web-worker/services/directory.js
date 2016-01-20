
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {
        getById: function(id) {
            if (!id) {
                throw new Error('id cannot be null');
            }
            return ajax('GET', '/directory/get', {
                id: id
            });
        },

        search: function(search) {
            return ajax('GET', '/directory/search', {
                search: search
            });
        }
    };

    return api;
});

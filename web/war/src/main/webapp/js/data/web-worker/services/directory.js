
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {
        getById: function(id) {
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

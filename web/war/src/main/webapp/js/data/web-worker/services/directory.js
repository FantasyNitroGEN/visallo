
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

        search: function(search, options) {
            var params = _.extend({}, options, { search: search });
            return ajax('GET', '/directory/search', params);
        }
    };

    return api;
});

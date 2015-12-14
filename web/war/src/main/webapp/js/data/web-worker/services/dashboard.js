define(['../util/ajax'], function(ajax) {
    'use strict';

    return {
        requestData: function(endpoint, params) {
            return ajax('GET', endpoint, params);
        },

        postData: function(endpoint, params) {
            return ajax('POST', endpoint, params);
        },

        dashboards: function() {
            return ajax('GET', '/dashboard/all')
                .then(function(result) {
                    return result.dashboards.map(function(dashboard) {
                        dashboard.items = dashboard.items.map(function(item) {
                            if (item.configuration) {
                                try {
                                    item.configuration = JSON.parse(item.configuration);
                                } catch(e) {
                                    console.error(e);
                                }
                            }
                            return item;
                        })
                        return dashboard;
                    })
                })
        },

        dashboarditemdelete: function(itemId) {
            return ajax('DELETE', '/dashboard/item', {
                dashboardItemId: itemId
            });
        },

        dashboardnew: function(options) {
            var params = {};
            if (options && options.title) {
                params.title = options.title;
            }
            if (options && options.items) {
                params.items = options.items.map(function(item) {
                    var mapped = _.extend({}, item);
                    if (mapped.configuration) {
                        mapped.configuration = JSON.stringify(mapped.configuration);
                    }
                    return JSON.stringify(mapped);
                })
            }
            return ajax('POST', '/dashboard', params);
        },

        dashboardupdate: function(params) {
            return ajax('POST', '/dashboard', params);
        },

        dashboarditemupdate: function(item) {
            return ajax('POST', '/dashboard/item', {
                dashboardItemId: item.id,
                extensionId: item.extensionId,
                title: item.title,
                configuration: JSON.stringify(item.configuration || {})
            });
        },

        dashboarditemnew: function(dashboardId, item) {
            if (!dashboardId) throw new Error('dashboardId required if new item');

            var params = {
                dashboardId: dashboardId
            };
            if ('title' in item) {
                params.title = item.title;
            }
            if (item.configuration) {
                params.configuration = JSON.stringify(item.configuration);
            }
            if ('extensionId' in item) {
                params.extensionId = item.extensionId;
            }
            return ajax('POST', '/dashboard/item', params);
        }
    };
});

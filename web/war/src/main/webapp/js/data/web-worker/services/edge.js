
define([
    '../util/ajax',
    './storeHelper'
], function(ajax, storeHelper) {
    'use strict';

    var api = {

        create: function(options) {
            return ajax('POST', '/edge/create', options);
        },

        'delete': function(edgeId) {
            return ajax('DELETE', '/edge', {
                edgeId: edgeId
            });
        },

        exists: function(edgeIds) {
            return ajax(edgeIds.length > 1 ? 'POST' : 'GET', '/edge/exists', {
                edgeIds: edgeIds
            });
        },

        properties: function(edgeId) {
            return ajax('GET', '/edge/properties', {
                graphEdgeId: edgeId
            });
        },

        setProperty: function(edgeId, property, optionalWorkspaceId) {
            var url = storeHelper.edgePropertyUrl(property);
            return ajax('POST', url, _.tap({
                 edgeId: edgeId,
                 propertyName: property.name,
                 value: property.value,
                 visibilitySource: property.visibilitySource,
                 justificationText: property.justificationText
            }, function(params) {
                if (property.sourceInfo) {
                    params.sourceInfo = JSON.stringify(property.sourceInfo);
                }
                if (property.key) {
                    params.propertyKey = property.key;
                }
                if (property.metadata) {
                    params.metadata = JSON.stringify(property.metadata)
                }
                if (optionalWorkspaceId) {
                    params.workspaceId = optionalWorkspaceId;
                }
            }));
        },

        deleteProperty: function(edgeId, property) {
            var url = storeHelper.edgePropertyUrl(property);
            return ajax('DELETE', url, {
                edgeId: edgeId,
                propertyName: property.name,
                propertyKey: property.key
            })
        },

        history: function(edgeId) {
            return ajax('GET', '/edge/history', {
                graphEdgeId: edgeId
            });
        },

        propertyHistory: function(edgeId, property, options) {
            return ajax('GET', '/edge/property/history', _.extend(
                {},
                options || {},
                {
                    graphEdgeId: edgeId,
                    propertyName: property.name,
                    propertyKey: property.key
                }
            ));
        },

        multiple: function(options) {
            return ajax('POST', '/edge/multiple', options);
        },

        store: storeHelper.createStoreAccessorOrDownloader(
            'edge', 'edgeIds', 'edges',
            function(toRequest) {
                return api.multiple({
                    edgeIds: toRequest
                });
            }),

        setVisibility: function(edgeId, visibilitySource) {
            return ajax('POST', '/edge/visibility', {
                graphEdgeId: edgeId,
                visibilitySource: visibilitySource
            });
        },

        acl: function(edgeId) {
            return ajax('GET', '/edge/acl', { elementId: edgeId });
        }
    };

    return api;
});

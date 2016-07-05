
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

        setPropertyVisibility: function(edgeId, property) {
            return ajax('POST', '/edge/property/visibility', {
                graphEdgeId: edgeId,
                newVisibilitySource: property.visibilitySource,
                oldVisibilitySource: property.oldVisibilitySource,
                propertyKey: property.key,
                propertyName: property.name
            })
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

        details: function(edgeId) {
            return ajax('GET', '/edge/details', { edgeId: edgeId });
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
        }
    };

    return api;
});

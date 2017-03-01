/**
 * Routes for edges
 *
 * @module services/edge
 * @see module:dataRequest
 */
define([
    '../util/ajax',
    './storeHelper',
    'require'
], function(ajax, storeHelper, require) {
    'use strict';

    /**
     * @alias module:services/edge
     */
    var api = {

        /**
         * Create an edge
         *
         * @param {object} options
         */
        create: function(options) {
            return ajax('POST', '/edge/create', options);
        },

        /**
         * Delete an edge (sandboxed)
         *
         * @param {string} edgeId
         */
        'delete': function(edgeId) {
            return ajax('DELETE', '/edge', {
                edgeId: edgeId
            });
        },

        /**
         * Check if the edge(s) exists (in current workspace)
         *
         * @param {Array.<string>} edgeIds
         */
        exists: function(edgeIds) {
            return ajax(edgeIds.length > 1 ? 'POST' : 'GET', '/edge/exists', {
                edgeIds: edgeIds
            });
        },

        /**
         * Get edge properties
         *
         * @param {string} edgeId
         */
        properties: function(edgeId) {
            return ajax('GET', '/edge/properties', {
                graphEdgeId: edgeId
            });
        },

        /**
         * Set visibility on a property
         *
         * @param {string} edgeId
         * @param {object} property
         * @param {string} property.visibilitySource
         * @param {string} property.oldVisibilitySource
         * @param {string} property.key
         * @param {string} property.name
         */
        setPropertyVisibility: function(edgeId, property) {
            return ajax('POST', '/edge/property/visibility', {
                graphEdgeId: edgeId,
                newVisibilitySource: property.visibilitySource,
                oldVisibilitySource: property.oldVisibilitySource,
                propertyKey: property.key,
                propertyName: property.name
            })
        },

        /**
         * Change/add property
         *
         * @param {string} edgeId
         * @param {object} property
         * @param {string} property.visibilitySource
         * @param {string} property.justificationText
         * @param {string} property.value
         * @param {string} property.name
         * @param {string} [property.key]
         * @param {object} [property.metadata]
         * @param {object} [property.sourceInfo]
         * @param {string} [workspaceId]
         */
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
            })).tap(storeHelper.updateElement);
        },

        /**
         * Delete a property
         *
         * @param {string} edgeId
         * @param {object} property
         * @param {string} property.name
         * @param {string} property.key
         */
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

        /**
         * Get history of edge (property changes, etc)
         *
         * @param {string} edgeId
         */
        history: function(edgeId) {
            return ajax('GET', '/edge/history', {
                graphEdgeId: edgeId
            });
        },

        /**
         * Get history for single property
         *
         * @param {string} edgeId
         * @param {object} property
         * @param {string} property.name
         * @param {string} property.key
         * @param {object} [options]
         */
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

        /**
         * @see module:services/edge.store
         * @function
         */
        multiple: storeHelper.createStoreAccessorOrDownloader('edge'),

        /**
         * Get the edgeIds from the cache or request multiple edges
         * if they aren't yet cached.
         *
         * @function
         * @param {object} obj
         * @param {Array.<string>} obj.edgeIds
         * @return {Array.<object>} edges
         * @example
         * dataRequest('edge', 'store', {
         *    edgeIds: ['e1', 'e2']
         * }).then(function(edges) {
         *     // ...
         * })
         */
        store: function(options) {
            return api.multiple(options);
        },

        /**
         * Set visibility on an edge
         *
         * @param {string} edgeId
         * @param {string} visibilitySource
         */
        setVisibility: function(edgeId, visibilitySource) {
            return ajax('POST', '/edge/visibility', {
                graphEdgeId: edgeId,
                visibilitySource: visibilitySource
            }).tap(storeHelper.updateElement);
        }
    };

    return api;
});

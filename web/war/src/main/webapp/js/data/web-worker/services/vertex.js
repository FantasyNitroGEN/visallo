/**
 * Routes for vertices
 *
 * @module services/vertex
 * @see module:util/withDataRequest
 */
define([
    '../util/ajax',
    './storeHelper'
], function(ajax, storeHelper) {
    'use strict';

    /**
     * @alias module:services/vertex
     */
    var api = {

        queryForOptions: function(options) {
            var params = {},
                q = _.isUndefined(options.query.query) ?
                    options.query :
                    options.query.query,
                matchType = options.matchType || 'vertex',
                url = '/' + matchType + '/search',
                originalUrl = url;

            if (options.conceptFilter && matchType === 'vertex') {
                if (_.isArray(options.conceptFilter)) {
                    params.conceptTypes = JSON.stringify(options.conceptFilter);
                } else {
                    params.conceptType = options.conceptFilter
                }
            }
            if (options.edgeLabelFilter
                && (matchType === 'edge' || (options.otherFilters && options.otherFilters.relatedToVertexIds))) {
                if (_.isArray(options.edgeLabelFilter)) {
                    params.edgeLabels = JSON.stringify(options.edgeLabelFilter);
                } else {
                    params.edgeLabel = options.edgeLabelFilter
                }
            }
            if (options.paging) {
                if (options.paging.offset) params.offset = options.paging.offset;
                if (options.paging.size) params.size = options.paging.size;
            }
            if (!_.isEmpty(options.sort)) {
                params.sort = options.sort.map(function(sort) {
                    return [sort.field, sort.direction.toUpperCase()].join(':');
                });
            }

            if (q) {
                params.q = q;
            }

            if (options.otherFilters) {
                _.each(options.otherFilters, function(value, key, options) {
                    if (key === 'url') {
                        url = value;
                        delete options.url;
                    } else if (_.isString(value) && !value) {
                        delete options[key];
                    }
                });
                _.extend(params, options.otherFilters);
            }

            params.filter = JSON.stringify(options.propertyFilters || []);
            return Promise.resolve({
                url: url,
                originalUrl: originalUrl,
                parameters: params
            });
        },

        search: function(options) {
            return api.queryForOptions(options)
                .then(function(query) {
                    return ajax('POST', query.url, query.parameters);
                })
                .tap(function({ elements }) {
                    storeHelper.putSearchResults(elements)
                })
        },

        'geo-search': function(lat, lon, radius) {
            return ajax('GET', '/vertex/geo-search', {
                lat: lat,
                lon: lon,
                radius: radius
            });
        },

        findPath: function(options) {
            return ajax('GET', '/vertex/find-path', options);
        },

        /**
         * Get history of vertex (property changes, etc)
         *
         * @param {string} vertexId
         */
        history: function(vertexId) {
            return ajax('GET', '/vertex/history', {
                graphVertexId: vertexId
            });
        },

        /**
         * Get history for single property
         *
         * @param {string} vertexId
         * @param {object} property
         * @param {string} property.name
         * @param {string} property.key
         * @param {object} [options]
         */
        propertyHistory: function(vertexId, property, options) {
            return ajax('GET', '/vertex/property/history', _.extend(
                {},
                options || {},
                {
                    graphVertexId: vertexId,
                    propertyName: property.name,
                    propertyKey: property.key
                }
            ));
        },

        details: function(vertexId) {
            return ajax('GET', '/vertex/details', { vertexId: vertexId });
        },

        /**
         * @see module:services/vertex.store
         * @function
         */
        multiple: storeHelper.createStoreAccessorOrDownloader('vertex'),

        /**
         * Get vertex properties
         *
         * @param {string} vertexId
         */
        properties: function(vertexId) {
            return ajax('GET', '/vertex/properties', {
                graphVertexId: vertexId
            });
        },

        propertyDetails: function(vertexId, name, key, visibility) {
            return ajax('GET', '/vertex/property/details', {
                vertexId: vertexId,
                propertyName: name,
                propertyKey: key,
                visibilitySource: visibility || ''
            });
        },

        propertyValue: function(vertexId, name, key) {
            return ajax('GET->HTML', '/vertex/property', {
                graphVertexId: vertexId,
                propertyName: name,
                propertyKey: key
            });
        },

        /**
         * Get connected edges to vertex
         *
         * @param {string} id
         * @param {object} [options]
         * @param {object} [options.offset]
         * @param {object} [options.size]
         * @param {object} [options.edgeLabel]
         * @param {object} [options.direction]
         */
        edges: function(vertexId, options) {
            var parameters = {
                graphVertexId: vertexId
            };
            if (options) {
                if (options.offset) parameters.offset = options.offset;
                if (options.size) parameters.size = options.size;
                if (options.edgeLabel) parameters.edgeLabel = options.edgeLabel;
                if (options.direction) parameters.direction = options.direction;
            }

            return ajax('GET', '/vertex/edges', parameters);
        },

        /**
         * Delete a vertex (sandboxed)
         *
         * @param {string} vertexId
         */
        'delete': function(vertexId) {
            return ajax('DELETE', '/vertex', {
                graphVertexId: vertexId
            })
        },

        /**
         * Check if the vertices exist (in current workspace)
         *
         * @param {Array.<string>} vertexIds
         */
        exists: function(vertexIds) {
            return ajax(vertexIds.length > 1 ? 'POST' : 'GET', '/vertex/exists', {
                vertexIds: vertexIds
            });
        },

        /**
         * Delete a property
         *
         * @param {string} vertexId
         * @param {object} property
         * @param {string} property.name
         * @param {string} property.key
         */
        deleteProperty: function(vertexId, property) {
            var url = storeHelper.vertexPropertyUrl(property);
            return ajax('DELETE', url, {
                graphVertexId: vertexId,
                propertyName: property.name,
                propertyKey: property.key
            })
        },

        /**
         * Get text property in HTML format
         *
         * @param {string} vertexId
         * @param {string} propertyKey
         * @param {string} propertyName
         */
        'highlighted-text': function(vertexId, propertyKey, propertyName) {
            return ajax('GET->HTML', '/vertex/highlighted-text', {
                graphVertexId: vertexId,
                propertyKey: propertyKey,
                propertyName: propertyName
            });
        },

        related: function(vertexIds, options) {
            return ajax('POST', '/vertex/find-related', {
                graphVertexIds: vertexIds,
                limitEdgeLabel: options.limitEdgeLabel,
                limitParentConceptId: options.limitParentConceptId
            });
        },

        /**
         * Get the vertexIds from the cache or request multiple vertices if they aren't yet cached.
         *
         * @function
         * @param {object} obj
         * @param {Array.<string>} obj.vertexIds
         * @return {Array.<object>} vertices
         * @example
         * dataRequest('vertex', 'store', {
         *    vertexIds: ['v1', 'v2']
         * }).then(function(vertices) {
         *     // ...
         * })
         */
        store: function(options) {
            return api.multiple(options);
        },

        uploadImage: function(vertexId, file) {
            return ajax('POST', '/vertex/upload-image?' +
                'graphVertexId=' + encodeURIComponent(vertexId), file);
        },

        /**
         * Create new vertex
         *
         * @param {object} justification
         * @param {string} [justification.justificationText]
         * @param {string} [justification.sourceInfo]
         * @param {string} conceptType
         * @param {string} visibilitySource
         */
        create: function(justification, conceptType, visibilitySource) {
            return ajax('POST', '/vertex/new', _.tap({
                conceptType: conceptType,
                visibilitySource: visibilitySource
            }, function(data) {
                if (justification.justificationText) {
                    data.justificationText = justification.justificationText;
                } else if (justification.sourceInfo) {
                    data.sourceInfo = JSON.stringify(justification.sourceInfo);
                }
            })).tap(storeHelper.updateElement);
        },

        cloudImport: function(cloudResource, cloudConfiguration) {
            return ajax('POST', '/vertex/cloudImport', {
                cloudResource, cloudConfiguration: JSON.stringify(cloudConfiguration)
            })
        },

        importFiles: function(files, conceptValue, visibilitySource) {
            var formData = new FormData();

            _.forEach(files, function(f) {
                formData.append('file', f);
                if (_.isString(visibilitySource)) {
                    formData.append('visibilitySource', visibilitySource);
                }
                if (_.isString(conceptValue)) {
                    formData.append('conceptId', conceptValue);
                }
            });

            if (_.isArray(conceptValue)) {
                _.forEach(conceptValue, function(v) {
                    formData.append('conceptId', v);
                });
            }

            return ajax('POST', '/vertex/import', formData);
        },

        importFileString: function(content, conceptValue, visibilitySource) {
            var formData = new FormData();

            formData.append('file', new Blob([content.string], {
                type: content.type
            }), 'untitled.' + (content.type === 'text/html' ? 'html' : 'txt'));
            if (_.isString(visibilitySource)) {
                formData.append('visibilitySource', visibilitySource);
            }
            if (_.isString(conceptValue)) {
                formData.append('conceptId', conceptValue);
            }

            return ajax('POST', '/vertex/import', formData);
        },

        /**
         * Set visibility on an vertex
         *
         * @param {string} vertexId
         * @param {string} visibilitySource
         */
        setVisibility: function(vertexId, visibilitySource) {
            return ajax('POST', '/vertex/visibility', {
                graphVertexId: vertexId,
                visibilitySource: visibilitySource
            }).tap(storeHelper.updateElement);
        },

        /**
         * Set visibility on a property
         *
         * @param {string} vertexId
         * @param {object} property
         * @param {string} property.visibilitySource
         * @param {string} property.oldVisibilitySource
         * @param {string} property.key
         * @param {string} property.name
         */
        setPropertyVisibility: function(vertexId, property) {
            return ajax('POST', '/vertex/property/visibility', {
                graphVertexId: vertexId,
                newVisibilitySource: property.visibilitySource,
                oldVisibilitySource: property.oldVisibilitySource,
                propertyKey: property.key,
                propertyName: property.name
            })
        },

        /**
         * Change/add property
         *
         * @param {string} vertexId
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
        setProperty: function(vertexId, property, optionalWorkspaceId) {
            var url = storeHelper.vertexPropertyUrl(property);
            return ajax('POST', url, _.tap({
                 graphVertexId: vertexId,
                 propertyName: property.name,
                 value: property.value,
                 visibilitySource: property.visibilitySource,
                 oldVisibilitySource: property.oldVisibilitySource,
                 justificationText: property.justificationText
            }, function(params) {
                if (property.sourceInfo) {
                    params.sourceInfo = JSON.stringify(property.sourceInfo);
                }
                if (!_.isUndefined(property.key)) {
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

        resolveTerm: function(params) {
            return ajax('POST', '/vertex/resolve-term', params);
        },

        unresolveTerm: function(params) {
            return ajax('POST', '/vertex/unresolve-term', params);
        },

        resolveDetectedObject: function(params) {
            return ajax('POST', '/vertex/resolve-detected-object', params);
        },

        unresolveDetectedObject: function(params) {
            return ajax('POST', '/vertex/unresolve-detected-object', params);
        }
    };

    return api;
});

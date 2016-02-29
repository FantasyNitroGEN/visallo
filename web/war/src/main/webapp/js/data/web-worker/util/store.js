
define([
    'require',
    'jscache',
    'configuration/plugins/registry',
    './cacheDecisions'
], function(require, Cache, registry, cacheDecisions) {
    'use strict';

    // Object cache per workspace
    var KIND_TO_CACHE = {
            vertex: 'vertices',
            edge: 'edges',
            workspaceEdges: 'workspaceEdges',
            workspace: 'workspace',
            filteredVertexIds: 'filteredVertexIds',
            cachedIds: 'cachedIds'
        },
        workspaceCaches = {},

        api = {

            logStatistics: function() {
                _.each(workspaceCaches, function(cache, workspaceId) {
                    var v = cache.vertices.getStats(),
                        e = cache.edges.getStats();
                    console.log(
                        'V(size:%s hits:%s miss:%s) E(size:%s hits:%s miss:%s)',
                        cache.vertices.size(),
                        v.hits,
                        v.misses,
                        cache.edges.size(),
                        e.hits,
                        e.misses,
                        workspaceId
                    );
                })
            },

            getObject: function(workspaceId, kind, objectId) {
                var result = api.getObjects(workspaceId, kind, objectId ? [objectId] : null);
                if (objectId) {
                    return result.length ? result[0] : null;
                }
                return result;
            },

            getObjects: function(workspaceId, kind, objectIds) {
                if (!(kind in KIND_TO_CACHE)) {
                    throw new Error('kind parameter not valid', kind);
                }

                var workspaceCache = cacheForWorkspace(workspaceId, { create: false }),
                    cache = workspaceCache && workspaceCache[KIND_TO_CACHE[kind]];

                if (objectIds) {
                    return objectIds.map(function(oId) {
                        return cache && cache.getItem(oId);
                    });
                }

                return cache;
            },

            removeObject: function(workspaceId, kind, objectId) {
                if (!(kind in KIND_TO_CACHE)) {
                    throw new Error('kind parameter not valid', kind);
                }

                var workspaceCache = cacheForWorkspace(workspaceId, { create: false }),
                    cache = workspaceCache && workspaceCache[KIND_TO_CACHE[kind]];

                if (cache && objectId) {
                    cache.removeItem(objectId);
                }
            },

            setWorkspace: function(workspace) {
                if (_.isArray(workspace.vertices)) {
                    workspace.vertices = _.indexBy(workspace.vertices, 'vertexId');
                }
                parseJsonLayoutForVertices(workspace.vertices);
                var workspaceCache = cacheForWorkspace(workspace.workspaceId);
                workspaceCache.workspace = workspace;
                return workspace;
            },

            setFilteredVertexIds: function(workspaceId, vertexIds) {
                if (arguments.length === 1) {
                    vertexIds = workspaceId;
                    workspaceId = null;
                }

                var workspaceCache = cacheForWorkspace(workspaceId);
                workspaceCache.filteredVertexIds = vertexIds;
            },

            removeWorkspaceVertexIds: function(workspaceId, vertexIds) {
                if (vertexIds.length) {
                    var workspace = api.getObject(workspaceId, 'workspace'),
                        vertexIdsAsArray = _.isArray(vertexIds) ? vertexIds : [vertexIds];

                    workspace.vertices = _.omit(workspace.vertices, vertexIdsAsArray);

                    vertexIdsAsArray.forEach(function(vertexId) {
                        api.removeObject(workspaceId, 'vertex', vertexId);
                    })
                }
            },

            updateWorkspace: function(workspaceId, changes) {
                var workspace = JSON.parse(JSON.stringify(api.getObject(workspaceId, 'workspace')));
                changes.entityUpdates.forEach(function(entityUpdate) {
                    var workspaceVertex = _.findWhere(workspace.vertices, { vertexId: entityUpdate.vertexId });
                    if (workspaceVertex) {
                        if ('graphPosition' in entityUpdate) {
                            workspaceVertex.graphPosition = entityUpdate.graphPosition;
                        } else {
                            delete workspaceVertex.graphPosition;
                        }
                        if ('graphLayoutJson' in entityUpdate) {
                            workspaceVertex.graphLayoutJson = entityUpdate.graphLayoutJson;
                        } else {
                            delete workspaceVertex.graphLayoutJson;
                        }
                    } else {
                        workspace.vertices[entityUpdate.vertexId] =
                            _.pick(entityUpdate, 'vertexId', 'graphPosition', 'graphLayoutJson');
                    }
                });
                workspace.vertices = _.omit(workspace.vertices, changes.entityDeletes);
                if (changes.title) {
                    workspace.title = changes.title
                }

                var updatedIds = _.pluck(changes.userUpdates, 'userId');
                updatedIds.concat(changes.userDeletes).forEach(function(userId) {
                    for (var i = 0; i < workspace.users.length; i++) {
                        if (workspace.users[i].userId === userId) {
                            workspace.users.splice(i, 1);
                            return;
                        }
                    }
                });
                workspace.users = workspace.users.concat(changes.userUpdates);

                api.workspaceWasChangedRemotely(workspace);
                return workspace;
            },

            removeWorkspace: function(workspaceId) {
                if (workspaceId in workspaceCaches) {
                    delete workspaceCaches[workspaceId];
                }
            },

            workspaceWasChangedRemotely: function(remoteWorkspace) {
                var user = _.findWhere(remoteWorkspace.users, { userId: publicData.currentUser.id });
                remoteWorkspace.editable = (/WRITE/i).test(user && user.access);
                remoteWorkspace.commentable = (/(COMMENT|WRITE)/i).test(user && user.access);
                remoteWorkspace.sharedToUser = remoteWorkspace.createdBy !== publicData.currentUser.id;
                if (('vertices' in remoteWorkspace)) {
                    remoteWorkspace.vertices = _.indexBy(remoteWorkspace.vertices, 'vertexId');
                    parseJsonLayoutForVertices(remoteWorkspace.vertices);
                }
                var workspace = api.getObject(remoteWorkspace.workspaceId, 'workspace');
                if (!workspace || !_.isEqual(remoteWorkspace, workspace)) {
                    console.groupCollapsed('Workspace Update');
                    console.debug('old', workspace);
                    console.debug('new', remoteWorkspace);
                    console.groupEnd('Workspace Update');

                    var vertexIds = _.keys(remoteWorkspace.vertices),
                        vertexIdsPrevious = workspace ? _.keys(workspace.vertices) : [],
                        addedIds = _.difference(vertexIds, vertexIdsPrevious),
                        removedIds = _.difference(vertexIdsPrevious, vertexIds),
                        updatedIds = _.without.apply(_, [vertexIds].concat(addedIds)),
                        added = _.values(_.pick(remoteWorkspace.vertices, addedIds)),
                        updated = _.compact(_.map(updatedIds, function(vId) {
                            return (workspace && _.isEqual(
                                remoteWorkspace.vertices[vId],
                                workspace.vertices[vId]
                            )) ? null : remoteWorkspace.vertices[vId];
                        }));

                    require(['../services/vertex'], function(vertex) {
                        vertex.store({ vertexIds: addedIds })
                            .done(function(newVertices) {
                                api.setWorkspace(remoteWorkspace);
                                dispatchMain('workspaceUpdated', {
                                    workspace: remoteWorkspace,
                                    newVertices: newVertices,
                                    entityUpdates: updated.concat(added),
                                    entityDeletes: removedIds,
                                    userUpdates: [],
                                    userDeletes: []
                                });
                            });
                    });
                }
            },

            workspaceShouldSave: function(workspace, changes) {
                var willChange = false;

                willChange = willChange || (changes.title && changes.title !== workspace.title);

                willChange = willChange || changes.userDeletes.length;
                willChange = willChange || changes.userUpdates.length;

                willChange = willChange || changes.entityDeletes.length;
                willChange = willChange || _.any(changes.entityUpdates, function(entityUpdate) {
                    var workspaceVertex = workspace.vertices[entityUpdate.vertexId];
                    if (workspaceVertex) {
                        return !_.isEqual(workspaceVertex, entityUpdate);
                    } else {
                        return true;
                    }
                });

                return willChange;
            },

            updateObject: function(data, options) {
                var onlyIfExists = options && options.onlyIfExists === true,
                    cached;

                if (!data.workspaceId) {
                    data.workspaceId = publicData.currentWorkspaceId;
                }

                var cachedIds = api.getObject(data.workspaceId, 'cachedIds');
                if (data.vertex && resemblesVertex(data.vertex)) {
                    cached = api.getObject(data.workspaceId, 'vertex', data.vertex.id);
                    if (cached || (cachedIds && (data.vertex.id in cachedIds)) || !onlyIfExists) {
                        cacheVertices(data.workspaceId, [data.vertex]);
                    }
                }

                if (data.edge) {
                    var toCache;

                    if (resemblesEdge(data.edge)) {
                        toCache = data.edge;
                    }

                    if (toCache) {
                        cacheEdges(data.workspaceId, [toCache]);
                    }
                }
            },

            checkAjaxForPossibleCaching: function(xhr, json, workspaceId, request) {
                var url = request.url,
                    cacheable;

                if (url === '/workspace/edges') {
                    cacheWorkspaceEdges(workspaceId, json.edges);
                    return;
                }

                if (cacheDecisions.shouldCacheObjectsAtUrl(url)) {
                    var list = json.elements || json.vertices || json.edges;
                    if (resemblesVertex(json)) {
                        if (cacheDecisions.shouldCacheVertexAtUrl(json, url)) {
                            console.debug(request.url, 'causing vertex to cache', json);
                            cacheVertices(workspaceId, [json], cachePriorityForUrl(request.url));
                        }
                    }
                    if (resemblesVertices(list)) {
                        cacheable = _.filter(list, function(v) {
                            return cacheDecisions.shouldCacheVertexAtUrl(v, url);
                        });
                        if (cacheable.length) {
                            console.debug(request.url, 'causing ' + cacheable.length + ' vertices to cache');
                            cacheVertices(workspaceId, cacheable, cachePriorityForUrl(request.url));
                        }
                    }
                    if (resemblesEdge(json)) {
                        if (cacheDecisions.shouldCacheEdgeAtUrl(json, url)) {
                            cacheEdges(workspaceId, [json]);
                            cacheWorkspaceEdgeIfVerticesInWorkspace(workspaceId, json);
                            var edgeVertices = _.compact([json.source, json.target].map(function(v) {
                                if (v && resemblesVertex(v)) {
                                    return v;
                                }
                            }));
                            if (edgeVertices.length) {
                                cacheVertices(workspaceId, edgeVertices);
                            }
                        }
                    }
                    if (resemblesEdges(list)) {
                        cacheable = _.filter(list, function(e) {
                            return cacheDecisions.shouldCacheEdgeAtUrl(e, url);
                        });
                        if (cacheable.length) {
                            cacheEdges(workspaceId, cacheable);
                        }
                    }
                    if (_.isArray(json.relationships) && json.relationships.length && 'vertex' in json.relationships[0]) {
                        var vertices = _.pluck(json.relationships, 'vertex');
                        if (resemblesVertices(vertices)) {
                            cacheable = _.filter(vertices, function(v) {
                                return cacheDecisions.shouldCacheVertexAtUrl(v, url);
                            })
                            console.debug(request.url, 'causing ' + cacheable.length + ' vertices to cache');
                            cacheVertices(workspaceId, cacheable, Cache.Priority.LOW);
                        }
                    }
                }
            }

        };

    return api;

    function cachePriorityForUrl(url) {
        var search = /^\/(vertex|edge|element)\/search$/;
        if (search.test(url)) {
            return Cache.Priority.LOW;
        }
    }

    function parseJsonLayoutForVertices(workspaceVertices) {
        if (workspaceVertices) {
            _.each(workspaceVertices, function(wv) {
                if (('graphLayoutJson' in wv) && _.isString(wv.graphLayoutJson)) {
                    wv.graphLayoutJson = JSON.parse(wv.graphLayoutJson);
                }
            })
        }
    }

    function cacheForWorkspace(workspaceId, options) {
        if (!workspaceId) {
            workspaceId = publicData.currentWorkspaceId;
        }

        if (workspaceId in workspaceCaches) {
            return workspaceCaches[workspaceId];
        }

        if (options && options.create === false) {
            return null;
        }

        workspaceCaches[workspaceId] = {
            vertices: new Cache(),
            edges: new Cache(),
            workspaceEdges: [],
            cachedIds: {}
        };

        return workspaceCaches[workspaceId];
    }

    function getCacheOptions(kind, callback) {
        require(['data/web-worker/services/config'], function(config) {
            config.properties()
                .then(function(properties) {
                    var lruSeconds = parseInt(properties['cache.' + kind + '.lru.expiration.seconds'], 10),
                        maxCacheSizeVertices = parseInt(properties['cache.' + kind + '.max_size'], 10);
                    return {
                        expirationSliding: lruSeconds === -1 ? null : lruSeconds,
                        maxSize: maxCacheSizeVertices
                    }
                })
                .then(callback)
                .done();
        });
    }

    function cacheWorkspaceEdges(workspaceId, workspaceEdges) {
        var workspaceCache = cacheForWorkspace(workspaceId);
        workspaceCache.workspaceEdges = workspaceEdges;
    }

    function cacheWorkspaceEdgeIfVerticesInWorkspace(workspaceId, edge) {
        var workspaceVertices = api.getObject(workspaceId, 'workspace').vertices,
            workspaceEdges = api.getObject(workspaceId, 'workspaceEdges');

        if (edge.outVertexId in workspaceVertices && edge.inVertexId in workspaceVertices) {
            var existingWorkspaceEdgeIndex = _.findIndex(workspaceEdges, function(workspaceEdge) {
                    return workspaceEdge.edgeId === edge.id
                }),
                minimalJson = {
                    edgeId: edge.id,
                    inVertexId: edge.inVertexId,
                    label: edge.label,
                    outVertexId: edge.outVertexId
                };
            if (existingWorkspaceEdgeIndex >= 0) {
                workspaceEdges.splice(existingWorkspaceEdgeIndex, 1, minimalJson);
            } else {
                workspaceEdges.push(minimalJson);
            }
        }
    }

    function cacheVertices(workspaceId, vertices, priority) {
        getCacheOptions('vertex', function(cacheOptions) {
            var workspaceCache = cacheForWorkspace(workspaceId),
                workspace = api.getObject(workspaceId, 'workspace'),
                vertexCache = workspaceCache.vertices,
                cachedIds = workspaceCache.cachedIds;

            vertexCache.resize(cacheOptions.maxSize);

            var added = [],
                updated = _.compact(vertices.map(function(v) {
                // Search puts a score, but we don't use it and it breaks
                // our cache update check
                if ('score' in v) {
                    delete v.score;
                }

                var previous = vertexCache.getItem(v.id);
                if (previous && _.isEqual(v, previous)) {
                    return;
                }

                cachedIds[v.id] = true;

                var inWorkspace = workspace && workspace.vertices && (v.id in workspace.vertices),
                    cachePriority = inWorkspace ? Cache.Priority.HIGH : (priority || Cache.Priority.NORMAL);

                vertexCache.setItem(v.id, v, {
                    expirationAbsolute: null,
                    expirationSliding: cacheOptions.expirationSliding,
                    priority: cachePriority
                });

                if (previous) {
                    //console.debug('Vertex updated previous:', previous, 'new:', v)
                    return v;
                } else {
                    added.push(v);
                }
            }));

            if (updated.length && workspaceId === publicData.currentWorkspaceId) {
                dispatchMain('storeObjectsUpdated', { vertices: updated });
            }

            if (updated.length || added.length) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'storeVerticesChanged',
                    data: {
                        updated: updated,
                        added: added,
                        workspaceId: workspaceId
                    }
                });
            }
        });
    }

    function cacheEdges(workspaceId, edges) {
        getCacheOptions('edge', function(cacheOptions) {
            var edgeCache = cacheForWorkspace(workspaceId).edges,
                workspace = api.getObject(workspaceId, 'workspace');

            edgeCache.resize(cacheOptions.maxSize);

            var updated = _.compact(edges.map(function(e) {
                    var previous = edgeCache.getItem(e.id);

                    if ((previous && _.isEqual(e, previous))) {
                        return;
                    }

                    edgeCache.setItem(e.id, e, {
                        expirationAbsolute: null,
                        expirationSliding: cacheOptions.expirationSliding,
                        priority: Cache.Priority.NORMAL
                    });

                    // console.debug('Edge updated previous:', previous, 'new:', e)
                    return e;
                }));

            if (updated.length && workspaceId === publicData.currentWorkspaceId) {
                dispatchMain('storeObjectsUpdated', { edges: updated });
            }
        });
    }

    function resemblesEdge(val) {
        return (
            _.isObject(val) &&
            val.type === 'edge' &&
            _.has(val, 'id') &&
            _.has(val, 'label')
        );
    }

    function resemblesEdges(val) {
        return _.isArray(val) && val.length && resemblesEdge(val[0]);
    }

    function resemblesVertex(val) {
        return (
            _.isObject(val) &&
            _.has(val, 'id') &&
            _.has(val, 'sandboxStatus') &&
            _.has(val, 'properties') &&
            _.isArray(val.properties) &&
            !_.has(val, 'outVertexId') &&
            !_.has(val, 'inVertexId')
        );
    }

    function resemblesVertices(val) {
        return _.isArray(val) && val.length && resemblesVertex(val[0]);
    }
});

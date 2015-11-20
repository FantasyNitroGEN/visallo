define([], function() {
    'use strict';

    return withObjectSelection;

    function defaultNoObjectsOrData(data) {
        var baseObj = {
                vertices: [],
                edges: [],
                vertexIds: {},
                edgeIds: {}
            },
            returnable = _.extend({}, baseObj, data || {});

        returnable.vertexIds = _.indexBy(returnable.vertices, 'id');
        returnable.edgeIds = _.indexBy(returnable.edges, 'id');

        return returnable;
    }

    function withObjectSelection() {

        var selectedObjects,
            selectedObjectsStackByWorkspace = {},
            previousSelectedObjects,
            edges;

        this.after('initialize', function() {
            this.setPublicApi('selectedObjects', defaultNoObjectsOrData());

            this.on('selectObjects', this.onSelectObjects);
            this.on('selectAll', this.onSelectAll);
            this.on('selectConnected', this.onSelectConnected);

            this.on('deleteSelected', this.onDeleteSelected);
            this.on('edgesLoaded', this.onEdgesLoaded);
            this.on('edgesDeleted', function(event, data) {
                if (selectedObjects && _.findWhere(selectedObjects.edges, { id: data.edgeId })) {
                    if (selectedObjects.edges.length === 1) {
                        this.trigger('selectObjects');
                    } else {
                        selectedObjects.edges = _.reject(selectedObjects.edges, function(e) {
                            return data.edgeId === e.id
                        })
                    }
                }
            });
            this.on('verticesDeleted', function(event, data) {
                if (selectedObjects) {
                    if (selectedObjects.vertices.length) {
                        this.trigger('selectObjects', {
                            vertices: _.reject(selectedObjects.vertices, function(v) {
                                return ~data.vertexIds.indexOf(v.id)
                            })
                        });
                    }
                }
            });

            this.on('searchTitle', this.onSearchTitle);
            this.on('searchRelated', this.onSearchRelated);
            this.on('addRelatedItems', this.onAddRelatedItems);
            this.on('objectsSelected', this.onObjectsSelected);
        });

        this.onEdgesLoaded = function(event, data) {
            edges = data.edges;
            this.setPublicApi('workspaceEdges', edges);
        };

        this.onSelectAll = function(event, data) {
            var self = this;

            this.dataRequestPromise.done(function(dataRequest) {
                dataRequest('workspace', 'store')
                    .done(function(vertices) {
                        self.trigger('selectObjects', { vertexIds: _.keys(vertices) });
                    })
            })
        };

        this.onSelectConnected = function(event, data) {
            var self = this,
                vertices = selectedObjects.vertices;

            if (vertices.length && edges && edges.length) {
                this.dataRequestPromise.done(function(dataRequest) {
                    dataRequest('workspace', 'store')
                        .done(function(workspaceVertices) {
                            var selected = _.pluck(vertices, 'id'),
                                toSelect = _.chain(edges)
                                    .map(function(e) {
                                        return selected.map(function(v) {
                                            return e.outVertexId === v ?
                                                e.inVertexId :
                                                e.inVertexId === v ?
                                                e.outVertexId :
                                                null
                                        })
                                    })
                                    .flatten()
                                    .compact()
                                    .unique()
                                    .filter(function(vId) {
                                        return vId in workspaceVertices;
                                    })
                                    .value();

                            self.trigger('selectObjects', { vertexIds: toSelect });
                        })
                })
            }
        };

        this.onDeleteSelected = function(event, data) {
            var self = this;

            require(['util/privileges'], function(Privileges) {
                if (!Privileges.canEDIT) {
                    return;
                }

                if (data && data.vertexId) {
                    self.trigger('updateWorkspace', {
                        entityDeletes: [data.vertexId]
                    });
                } else if (selectedObjects) {
                    if (selectedObjects.vertices.length) {
                        self.trigger('updateWorkspace', {
                            entityDeletes: _.pluck(selectedObjects.vertices, 'id')
                        });
                    }
                }
            });
        };

        this.onSelectObjects = function(event, data) {
            var self = this,
                hasItems = data &&
                    (
                        (data.vertexIds || data.vertices || []).length > 0 ||
                        (data.edgeIds || data.edges || []).length > 0
                    ),
                promises = [];

            this.dataRequestPromise.done(function(dataRequest) {
                var hasSomeVertices = false;

                if (data && data.vertexIds) {
                    if (!_.isArray(data.vertexIds)) {
                        data.vertexIds = [data.vertexIds];
                    }
                    promises.push(
                        dataRequest('vertex', 'store', { vertexIds: data.vertexIds })
                    );
                    hasSomeVertices = data.vertexIds.length > 0;
                } else if (data && data.vertices) {
                    promises.push(Promise.resolve(data.vertices));
                    hasSomeVertices = data.vertices.length > 0;
                } else {
                    promises.push(Promise.resolve([]));
                }

                if (hasSomeVertices) {
                    promises.push(Promise.resolve([]));
                } else if (data && data.edgeIds && data.edgeIds.length) {
                    promises.push(
                        dataRequest('edge', 'store', { edgeIds: data.edgeIds })
                    );
                } else if (data && data.edges) {
                    promises.push(Promise.resolve(data.edges));
                } else {
                    promises.push(Promise.resolve([]));
                }

                Promise.all(promises)
                    .done(function(result) {
                        var vertices = _.compact(result[0] || []),
                            edges = _.compact(result[1] || []);

                        if (!edges.length && !vertices.length && hasItems) {
                            return;
                        }

                        selectedObjects = {
                            vertices: vertices,
                            edges: vertices.length ? [] : edges
                        };

                        if (previousSelectedObjects &&
                            selectedObjects &&
                            _.isEqual(previousSelectedObjects, selectedObjects)) {
                            return;
                        }

                        self.appendToStack(selectedObjects)
                            .done(function() {
                                previousSelectedObjects = selectedObjects;

                                var transformedObjects = defaultNoObjectsOrData(selectedObjects);
                                self.setPublicApi('selectedObjects', transformedObjects);

                                var postData = _.clone(selectedObjects);
                                if (data && 'focus' in data) {
                                    postData.focus = data.focus;
                                }
                                if (data && 'options' in data) {
                                    postData.options = data.options;
                                }
                                self.trigger(event.target, 'objectsSelected', postData);
                            });
                    });
            });
        };

        this.onObjectsSelected = function(event, data) {
            var self = this;

            if (data.vertices.length) {
                require(['util/vertex/urlFormatters'], function(F) {
                    self.trigger('clipboardSet', {
                        text: F.vertexUrl.url(data.vertices, visalloData.currentWorkspaceId)
                    });
                })
            } else {
                this.trigger('clipboardClear');
            }

            if (window.DEBUG) {
                DEBUG.selectedObjects = data;
            }
        };

        this.onSearchTitle = function(event, data) {
            var self = this,
                vertexId = data.vertexId || (
                    selectedObjects &&
                    selectedObjects.vertices.length === 1 &&
                    selectedObjects.vertices[0].id
                );

            if (vertexId) {
                Promise.all([
                    Promise.require('util/vertex/formatters'),
                    this.dataRequestPromise.then(function(dataRequest) {
                        return dataRequest('vertex', 'store', { vertexIds: vertexId });
                    })
                ]).done(function(results) {
                    var F = results.shift(),
                        vertex = results.shift(),
                        title = F.vertex.title(vertex);

                    self.trigger('searchByParameters', { submit: true, parameters: { q: title }});
                })
            }
        };

        this.onSearchRelated = function(event, data) {
            var self = this;

            require(['util/vertex/formatters'], function(F) {
                var vertexIds = F.vertex.getVertexIdsFromDataEventOrCurrentSelection(data);
                if (vertexIds.length) {
                    self.trigger('searchByRelatedEntity', { vertexIds: vertexIds });
                }
            })
        };

        this.onAddRelatedItems = function(event, data) {
            var self = this;

            require(['util/vertex/formatters'], function(F) {
                var vertexIds = F.vertex.getVertexIdsFromDataEventOrCurrentSelection(data);

                if (vertexIds.length) {
                    Promise.all([
                        Promise.require('util/popovers/addRelated/addRelated'),
                        self.dataRequestPromise.then(function(dataRequest) {
                            return dataRequest('vertex', 'store', { vertexIds: vertexIds })
                        })
                    ]).done(function(results) {
                        var RP = results.shift(),
                            vertex = results.shift();

                        RP.teardownAll();

                        RP.attachTo(event.target, {
                            vertex: vertex,
                            relatedToVertexIds: vertexIds,
                            anchorTo: {
                                vertexId: vertexIds[0]
                            }
                        });
                    });
                }
            });
        };

        this.getConfigurationMaxHistory = function() {
            if (!this.configurationPropertiesPromise) {
                this.configurationPropertiesPromise =
                    this.dataRequestPromise
                        .then(function(dataRequest) {
                            return dataRequest('config', 'properties');
                        })
                        .then(function(properties) {
                            return parseInt(properties['detail.history.stack.max'], 10);
                        });
            }

            return this.configurationPropertiesPromise
        };

        this.appendToStack = function(selectedObjects) {
            var self = this,
                currentObjects = defaultNoObjectsOrData(selectedObjects),
                currentVertexIds = _.keys(currentObjects.vertexIds),
                workspaceId = visalloData.currentWorkspaceId,
                selectedObjectsStack = selectedObjectsStackByWorkspace[workspaceId] || (
                    selectedObjectsStackByWorkspace[workspaceId] = []
                ),
                currentEdgeIds = currentVertexIds.length === 0 && _.keys(currentObjects.edgeIds);

            if (!_.isEmpty(currentVertexIds) || !_.isEmpty(currentEdgeIds)) {

                var listedIndex = -1,
                    newSelectionIndex = -1;

                selectedObjectsStack.forEach(function(stack, index) {
                    var listed = function(v, e) {
                            return v.length ?
                                !_.isEmpty(stack.vertexIds) && !_.isEmpty(v) && _.isEqual(stack.vertexIds, v) :
                                !_.isEmpty(stack.edgeIds) && !_.isEmpty(e) && _.isEqual(stack.edgeIds, e);
                        },
                        currentListed = listed(currentVertexIds, currentEdgeIds);

                    stack.hide = currentListed;

                    if (currentListed) {
                        listedIndex = index
                    }
                });

                if (listedIndex >= 0) {
                    var extracted = selectedObjectsStack.splice(listedIndex, 1);
                    selectedObjectsStack.push(extracted[0]);
                } else {
                    selectedObjectsStack.push({
                        vertexIds: currentVertexIds || [],
                        edgeIds: currentEdgeIds || [],
                        hide: true
                    });
                }

                return Promise.all([
                    this.getConfigurationMaxHistory(),
                    Promise.require('util/vertex/formatters'),
                    this.dataRequestPromise.then(function(dataRequest) {
                        return dataRequest('ontology', 'relationships')
                    }),
                    this.dataRequestPromise
                ]).then(function(results) {
                    var max = results.shift(),
                        F = results.shift(),
                        relationships = results.shift(),
                        dataRequest = results.shift(),
                        notHidden = _.reject(selectedObjectsStack, function(s) {
                            return s.hide;
                        }),
                        vertexIdsToRequest = _.chain(notHidden)
                            .filter(function(s) {
                                return s.vertexIds.length === 1;
                            })
                            .map(function(s) {
                                return s.vertexIds[0];
                            })
                            .value(),
                        edgesToRequest = _.chain(notHidden)
                            .filter(function(s) {
                                return s.edgeIds.length;
                            })
                            .map(function(s) {
                                return s.edgeIds;
                            })
                            .flatten()
                            .value(),
                        edgesById;

                    self.setPublicApi('selectedObjectsStack', notHidden.slice(Math.max(0, notHidden.length - max)));

                    return (edgesToRequest.length ?
                            dataRequest('edge', 'store', { edgeIds: edgesToRequest }) :
                            Promise.resolve([])
                        ).then(function(edges) {
                            edgesById = _.indexBy(_.compact(edges), 'id');
                            edges.forEach(function(edge) {
                                vertexIdsToRequest.push(edge.outVertexId);
                                vertexIdsToRequest.push(edge.inVertexId);
                            });
                            if (vertexIdsToRequest.length) {
                                return dataRequest('vertex', 'store', { vertexIds: _.unique(vertexIdsToRequest) });
                            }
                            return Promise.resolve([]);
                        }).then(function(vertices) {
                            var verticesById = _.indexBy(_.compact(vertices), 'id');
                            notHidden.forEach(function(s) {
                                var hadVertexIds = s.vertexIds.length;
                                s.vertexIds = _.filter(s.vertexIds, function(vertexId) {
                                    return vertexId in verticesById;
                                });
                                s.edgeIds = _.filter(s.edgeIds, function(edgeId) {
                                    return edgeId in edgesById;
                                });
                                if (hadVertexIds) {
                                    if (s.vertexIds.length === 1) {
                                        s.title = F.vertex.title(verticesById[s.vertexIds[0]]);
                                    } else if (s.vertexIds.length) {
                                        s.title = F.number.pretty(s.vertexIds.length) + ' entities';
                                    } else {
                                        s.hide = true;
                                    }
                                } else if (s.edgeIds.length === 1) {
                                    var edge = edgesById[s.edgeIds[0]],
                                        source = edge && verticesById[edge.outVertexId],
                                        target = edge && verticesById[edge.inVertexId],
                                        ontologyEdge = edge && relationships.byTitle[edge.label],
                                        label = ' → ';
                                    if (source && target) {
                                        s.title = [
                                            F.string.truncate(F.vertex.title(source), 2),
                                            F.string.truncate(F.vertex.title(target), 2)
                                        ].join(label);
                                    } else {
                                        s.hide = true;
                                    }
                                } else if (s.edgeIds.length) {
                                    s.title = F.number.pretty(s.edgeIds.length) + ' relationships';
                                } else {
                                    s.hide = true;
                                }
                            });
                            notHidden = _.reject(selectedObjectsStack, function(s) {
                                return s.hide;
                            });
                            self.setPublicApi('selectedObjectsStack', notHidden.slice(Math.max(0, notHidden.length - max)));
                        });
                });
            } else {
                return Promise.resolve();
            }
        };
    }
});

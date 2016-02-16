
define([
    'flight/lib/component',
    'cytoscape',
    'arbor',
    'cytoscape-arbor',
    './stylesheet',
    './withControlDrag',
    './layouts/grid',
    'tpl!./graph',
    'tpl!./loading',
    'util/controls',
    'util/throttle',
    'util/vertex/formatters',
    'util/privileges',
    'util/retina',
    'util/withContextMenu',
    'util/withAsyncQueue',
    'util/withDataRequest',
    'configuration/plugins/registry',
    'colorjs'
], function(
    defineComponent,
    cytoscape,
    arbor,
    cyarbor,
    stylesheet,
    withControlDrag,
    BetterGrid,
    template,
    loadingTemplate,
    Controls,
    throttle,
    F,
    Privileges,
    retina,
    withContextMenu,
    withAsyncQueue,
    withDataRequest,
    registry,
    colorjs) {
    'use strict';

    cyarbor(cytoscape, arbor);

        // Delay before showing hover effect on graph
    var HOVER_FOCUS_DELAY_SECONDS = 0.25,
        MAX_TITLE_LENGTH = 15,
        SELECTION_THROTTLE = 100,
        GRAPH_PADDING_BORDER = 20,
        GRAPH_SNAP_TO_GRID = 175,
        GRAPH_SNAP_TO_GRID_Y = 75,
        GRID_LAYOUT_X_INCREMENT = GRAPH_SNAP_TO_GRID,
        GRID_LAYOUT_Y_INCREMENT = GRAPH_SNAP_TO_GRID_Y,
        GRAPH_EXPORTER_POINT = 'org.visallo.graph.export',
        CYTOSCAPE_ANIMATION = {
            duration: 400,
            easing: 'spring(250, 20)'
        };

    return defineComponent(Graph, withAsyncQueue, withContextMenu, withControlDrag, withDataRequest);

    function Graph() {

        var shiftKey = false,
            edgeIdToGroupedCyEdgeId = {},
            LAYOUT_OPTIONS = {
                // Customize layout options
                random: { padding: 10 },
                arbor: { friction: 0.6, repulsion: 5000 * retina.devicePixelRatio, targetFps: 60, stiffness: 300 },
                breadthfirst: {
                    roots: function(nodes, options) {
                        if (options && options.onlySelected) {
                            return [];
                        }
                        return nodes.roots().map(function(n) { return n.id(); })
                    },
                    directed: false,
                    circle: false,
                    maximalAdjustments: 10
                }
            },
            snapCoordinate = function(value, snap) {
                var rounded = Math.round(value),
                    diff = (rounded % snap),
                    which = snap / 2;

                if (rounded < 0 && Math.abs(diff) < which) return rounded - diff;
                if (rounded < 0) return rounded - (snap + diff);
                if (diff < which) return rounded - diff;

                return rounded + (snap - diff)
            },
            snapPosition = function(cyNode) {
                var p = retina.pixelsToPoints(cyNode.position()),
                    height = retina.pixelsToPoints({w: 0, h: cyNode.height()}).h,
                    xSnap = GRAPH_SNAP_TO_GRID,
                    ySnap = GRAPH_SNAP_TO_GRID_Y || xSnap,
                    copy = {
                        x: snapCoordinate(p.x, xSnap),
                        y: snapCoordinate(p.y, ySnap) + (ySnap - height) / 2
                    };

                return copy;
            },
            fromCyId = function(cyId) {
                return F.className.from(cyId);
            },
            toCyId = function(v) {
                var vId = _.isString(v) ? v : ('id' in v) ? v.id : v.edgeId;
                return F.className.to(vId);
            },
            fullToSummaryEdge = function(fullEdge) {
                return {
                    edgeId: fullEdge.id,
                    label: fullEdge.label,
                    inVertexId: fullEdge.inVertexId,
                    outVertexId: fullEdge.outVertexId
                };
            },
            generateCompoundEdgeId = function(edge) {
                return edge.outVertexId + edge.inVertexId + edge.label;
            },
            cyEdgeFromEdge = function(e, sourceNode, destNode, ontologyRelationships) {
                var source = e.sourceId || (e.source && e.source.id),
                    target = e.targetId || (e.target && e.target.id),
                    type = e.relationshipType || e.label || e.type,
                    ontology = ontologyRelationships.byTitle[type],
                    cyEdgeId = toCyId(e.id),
                    classes = [];

                e.edges.forEach(function(edge) {
                    edgeIdToGroupedCyEdgeId[edge.edgeId || edge.id] = cyEdgeId;
                });

                var data = {
                    id: cyEdgeId,
                    type: type,
                    source: sourceNode.id(),
                    target: destNode.id(),
                    label: (ontology && ontology.displayName || '') + (
                        (e.edges.length > 1) ?
                            (' (' + F.number.pretty(e.edges.length) + ')') :
                            ''
                    ),
                    edges: e.edges
                };

                registry.extensionsForPoint('org.visallo.graph.edge.transformer').forEach(function(extension) {
                    extension(data);
                });

                registry.extensionsForPoint('org.visallo.graph.edge.class').forEach(function(extension) {
                    extension(e.edges, type, classes);
                });

                return {
                    group: 'edges',
                    data: data,
                    classes: classes.join(' ')
                };
            };

        this.toCyId = toCyId;
        this.fromCyId = fromCyId;

        this.defaultAttrs({
            cytoscapeContainerSelector: '.cytoscape-container',
            emptyGraphSelector: '.empty-graph',
            graphToolsSelector: '.controls',
            graphViewsSelector: '.graph-views',
            contextMenuSelector: '.graph-context-menu',
            vertexContextMenuSelector: '.vertex-context-menu'
        });

        this.onVerticesHoveringEnded = function(evt, data) {
            this.cytoscapeReady(function(cy) {
                cy.$('.hover').remove();
            });
        };

        var vertices, idToCyNode;
        this.onVerticesHovering = function(evt, data) {
            if (!this.isWorkspaceEditable) {
                return this.trigger('displayInformation', { message: i18n('graph.workspace.readonly') })
            }
            this.cytoscapeReady(function(cy) {
                var self = this,
                    offset = this.$node.offset(),
                    renderedPosition = {
                        x: data.position.x - offset.left,
                        y: data.position.y - offset.top
                    },
                    start = {
                        x: renderedPosition.x,
                        y: renderedPosition.y
                    },
                    inc = GRID_LAYOUT_X_INCREMENT * cy.zoom() * retina.devicePixelRatio,
                    yinc = GRID_LAYOUT_Y_INCREMENT * cy.zoom() * retina.devicePixelRatio,
                    width = inc * 4;

                if (data.start) {
                    idToCyNode = {};
                    data.vertices.forEach(function(v) {
                        idToCyNode[v.id] = cy.getElementById(toCyId(v));
                    });

                    // Sort existing nodes to end, except leave the first
                    // dragging vertex
                    vertices = data.vertices.sort(function(a, b) {
                        var cyA = idToCyNode[a.id], cyB = idToCyNode[b.id];
                        if (data.vertices[0].id === a.id) return -1;
                        if (data.vertices[0].id === b.id) return 1;
                        if (cyA.length && !cyB.length) return 1;
                        if (cyB.length && !cyA.length) return -1;

                        var titleA = F.vertex.title(a).toLowerCase(),
                            titleB = F.vertex.title(b).toLowerCase();

                        return titleA < titleB ? -1 : titleB < titleA ? 1 : 0;
                    });
                }

                vertices.forEach(function(vertex, i) {
                    var tempId = 'NEW-' + toCyId(vertex),
                        node = cy.getElementById(tempId);

                    if (node.length) {
                        node.renderedPosition(renderedPosition);
                    } else {
                        var classes = self.classesForVertex(vertex) + ' hover',
                            cyNode = idToCyNode[vertex.id];

                        if (cyNode.length) {
                            classes += ' existing';
                        }

                        var cyNodeData = {
                            group: 'nodes',
                            classes: classes,
                            data: {
                                id: tempId
                            },
                            renderedPosition: renderedPosition,
                            selected: false
                        };
                        self.updateCyNodeData(cyNodeData.data, vertex);
                        cy.add(cyNodeData);
                    }

                    renderedPosition.x += inc;
                    if (renderedPosition.x > (start.x + width) || i === 0) {
                        renderedPosition.x = start.x;
                        renderedPosition.y += yinc;
                    }
                });
            });
        };

        this.onVerticesDropped = function(evt, data) {
            if (!this.isWorkspaceEditable) return;
            if (!this.$node.is(':visible')) return;
            this.cytoscapeReady(function(cy) {
                var self = this,
                    vertices = data.vertices,
                    position = data.dropPosition,
                    toFitTo = [],
                    toAnimateTo = [],
                    toRemove = [],
                    entityUpdates = [];

                vertices.forEach(function(vertex, i) {
                    var node = cy.getElementById('NEW-' + toCyId(vertex));
                    if (node.length === 0) return;
                    if (i === 0) {
                        position = node.position();
                    }
                    if (node.hasClass('existing')) {
                        var existingNode = cy.getElementById(node.id().replace(/^NEW-/, ''));
                        if (existingNode.length) toFitTo.push(existingNode);
                        toAnimateTo.push([node, existingNode]);
                        toFitTo.push(existingNode);
                    } else {
                        if (visalloData.currentUser.uiPreferences.snapToGrid === 'true') {
                            position = retina.pointsToPixels(snapPosition(node));
                            node.position(position);
                        }
                        entityUpdates.push({
                            vertexId: vertex.id,
                            graphPosition: retina.pixelsToPoints(node.position())
                        });
                        node.ungrabify();
                        node.unselectify();
                        toRemove.push(node);
                    }
                });

                self.cyNodesToRemoveOnWorkspaceUpdated = cy.collection(toRemove);

                if (toFitTo.length) {
                    Promise.resolve(self.fit(cy, null, { animate: true }))
                        .then(function() {
                            finished();
                            animateToExisting(0);
                        })
                } else {
                    animateToExisting(0);
                    finished();
                }

                function animateToExisting(delay) {
                    toAnimateTo.forEach(function(args) {
                        self.animateFromToNode.apply(self, args.concat([delay]));
                    });
                }

                function finished() {
                    self.trigger('updateWorkspace', {
                        entityUpdates: entityUpdates
                    });
                }
            });
        };

        this.addVertices = function(vertices, opts) {
            var self = this,
                options = $.extend({ fit: false, animate: false }, opts),
                addedVertices = [],
                updatedVertices = [],
                dragging = $('.ui-draggable-dragging:not(.clone-vertex)'),
                isVisible = this.$node.closest('.visible').length === 1,
                cloned = null;

            if (dragging.length && isVisible) {
                cloned = dragging.clone()
                    .css({width: 'auto'})
                    .addClass('clone-vertex')
                    .insertAfter(dragging);
            }

            this.cytoscapeReady(function(cy) {
                var container = $(cy.container()),
                    currentNodes = cy.nodes(),
                    rawBoundingBox = currentNodes.boundingBox(),
                    availableSpaceBox = retina.pixelsToPoints({
                        x: isFinite(rawBoundingBox.x1) ? rawBoundingBox.x1 : 0,
                        y: isFinite(rawBoundingBox.y2) ? rawBoundingBox.y2 : 0,
                        w: isFinite(rawBoundingBox.w) ? rawBoundingBox.w : 0
                    }),
                    xInc = GRID_LAYOUT_X_INCREMENT,
                    yInc = GRID_LAYOUT_Y_INCREMENT,
                    defaultAvailablePosition = _.pick(availableSpaceBox, 'x', 'y'),
                    nextAvailablePosition = null;

                if (options.animate) container.removeClass('animateinstart').addClass('animatein');

                defaultAvailablePosition.y += yInc;

                var maxWidth = Math.max(availableSpaceBox.w, xInc * 10),
                    startX = null,
                    vertexIds = _.pluck(vertices, 'id'),
                    existingNodes = currentNodes.filter(function(i, n) {
                        var nId = n.id();
                        if (/^(NEW|controlDragNodeId)/.test(nId)) {
                            return -1;
                        }
                        return vertexIds.indexOf(fromCyId(nId)) >= 0;
                    }),
                    customLayout = $.Deferred();

                if (options.layout) {
                    require(['graph/layouts/' + options.layout.type], function(doLayout) {
                        customLayout.resolve(
                            doLayout(cy, currentNodes, rawBoundingBox, vertexIds, options.layout)
                        );
                    });
                } else customLayout.resolve({});

                if (vertices.length && self.vertexIdsToSelect && self.vertexIdsToSelect.length) {
                    cy.$(':selected').unselect();
                }

                customLayout.done(function(layoutPositions) {

                    var cyNodes = [];
                    vertices.forEach(function(vertex) {

                        var cyNodeData = {
                                group: 'nodes',
                                classes: self.classesForVertex(vertex),
                                data: {
                                    id: toCyId(vertex)
                                },
                                grabbable: self.isWorkspaceEditable,
                                selected: self.vertexIdsToSelect && ~self.vertexIdsToSelect.indexOf(vertex.id)
                            },
                            workspaceVertex = self.workspaceVertices[vertex.id];

                        self.updateCyNodeData(cyNodeData.data, vertex);

                        var needsAdding = false,
                            needsUpdating = false,
                            hasPosition = workspaceVertex && (
                                workspaceVertex.graphPosition || workspaceVertex.graphLayoutJson
                            );

                        if (!hasPosition) {
                            console.debug('Vertex added without position info', vertex);
                            return;
                        }

                        if (workspaceVertex.graphPosition) {
                            cyNodeData.position = retina.pointsToPixels(
                                workspaceVertex.graphPosition
                            );
                        } else {
                            if (!nextAvailablePosition) {
                                var layout = workspaceVertex.graphLayoutJson;
                                if (layout && layout.pagePosition) {
                                    var projectedPosition = cy.renderer().projectIntoViewport(
                                        workspaceVertex.graphLayoutJson.pagePosition.x,
                                        workspaceVertex.graphLayoutJson.pagePosition.y
                                    );
                                    nextAvailablePosition = retina.pixelsToPoints({
                                        x: projectedPosition[0],
                                        y: projectedPosition[1]
                                    });
                                } else if (layout && layout.relatedToVertexId) {
                                    var relatedToCyNode = cy.getElementById(toCyId(layout.relatedToVertexId));
                                    if (relatedToCyNode.length) {
                                        var relatedToPosition = retina.pixelsToPoints(relatedToCyNode.position());
                                        nextAvailablePosition = {
                                            x: Math.max(relatedToPosition.x, defaultAvailablePosition.x),
                                            y: Math.max(relatedToPosition.y, defaultAvailablePosition.y)
                                        };
                                    }
                                }

                                if (!nextAvailablePosition) {
                                    nextAvailablePosition = defaultAvailablePosition;
                                }
                            }

                            if (startX === null) {
                                startX = nextAvailablePosition.x;
                            }

                            var position = retina.pointsToPixels(nextAvailablePosition);
                            cyNodeData.position = {
                                x: Math.round(position.x),
                                y: Math.round(position.y)
                            };

                            nextAvailablePosition.x += xInc;
                            if ((nextAvailablePosition.x - startX) > maxWidth) {
                                nextAvailablePosition.y += yInc;
                                nextAvailablePosition.x = startX;
                            }

                            if (dragging.length === 0 || !isVisible) {
                                needsUpdating = true;
                            } else {
                                needsAdding = true;
                            }
                        }

                        if (needsAdding || needsUpdating) {
                            (needsAdding ? addedVertices : updatedVertices).push({
                                vertexId: vertex.id
                            });
                        }

                        cyNodes.push(cyNodeData);
                    });

                    self.vertexIdsToSelect = null;

                    var addedCyNodes = cy.add(cyNodes);
                    addedVertices.concat(updatedVertices).forEach(function(v) {
                        v.graphPosition = retina.pixelsToPoints(cy.getElementById(toCyId(v.vertexId)).position());
                    });

                    if (options.fit && cy.nodes().length) {

                        _.defer(self.fit.bind(self));

                    } else if (isVisible && options.fitToVertexIds && options.fitToVertexIds.length) {
                        var zoomOutToNodes = cy.$(
                            options.fitToVertexIds.map(function(v) {
                            return '#' + toCyId(v);
                        }).join(','));

                        _.defer(function() {
                            self.fitToNodesIfOutsideViewport(cy, zoomOutToNodes);
                        })
                    }

                    if (options.animate) {
                        if (cy.nodes().length) {
                            _.delay(function again() {
                                container.on(TRANSITION_END, function(e) {
                                    container.off(TRANSITION_END);
                                    container.removeClass('animatein animatestart');
                                });
                                container.addClass('animateinstart');

                            }, 250);
                        } else container.removeClass('animatein animateinstart');
                    }

                    if (cloned && !(existingNodes.length && cloned && cloned.length)) {
                        cloned.remove();
                    }

                    if (updatedVertices.length || addedVertices.length) {
                        self.trigger('updateWorkspace', {
                            entityUpdates: updatedVertices.concat(addedVertices)
                        });
                    }

                    self.hideLoading();

                    self.setWorkspaceDirty();
                });
            });
        };

        this.classesForVertex = function(vertex) {
            var cls = [],
                displayType = F.vertex.displayType(vertex);

            if (F.vertex.imageIsFromConcept(vertex) === false) {
                cls.push('hasCustomGlyph');
            }
            if (~['video', 'image'].indexOf(displayType)) {
                cls.push(displayType);
            }

            registry.extensionsForPoint('org.visallo.graph.node.class')
                .forEach(function(modifier) {
                    modifier(vertex, cls);
                });

            return cls.join(' ');
        };

        this.updateCyNodeData = function(data, vertex) {
            var truncatedTitle = F.string.truncate(F.vertex.title(vertex), 3),
                merged = data;

            merged.previousTruncated = truncatedTitle;
            merged.truncatedTitle = truncatedTitle;
            merged.conceptType = F.vertex.prop(vertex, 'conceptType');
            merged.imageSrc = F.vertex.image(vertex, null, 150);
            merged.selectedImageSrc = F.vertex.selectedImage(vertex, null, 150);

            registry.extensionsForPoint('org.visallo.graph.node.transformer')
                .forEach(function(dataTransform) {
                    dataTransform(vertex, merged);
                });

            return merged;
        };

        this.onVerticesDeleted = function(event, data) {
            this.cytoscapeReady(function(cy) {

                if (data.vertexIds.length) {
                    cy.$(
                        data.vertexIds.map(function(v) {
                            return '#' + toCyId(v);
                        }).join(',')
                    ).remove();

                    this.setWorkspaceDirty();
                    this.updateVertexSelections(cy);
                }
            });
        };

        this.onObjectsSelected = function(evt, data) {
            if ($(evt.target).is('.graph-pane')) {
                return;
            }

            this.cytoscapeReady(function(cy) {
                this.ignoreCySelectionEvents = true;

                cy.$(':selected').unselect();

                var vertices = data.vertices,
                    edges = data.edges,
                    cyNodes;
                if (vertices.length || edges.length) {
                    cyNodes = cy.$(
                        _.chain(
                            _.map(vertices, function(v) {
                                return '#' + toCyId(v);
                            })
                            .concat(
                                _.map(edges, function(e) {
                                    var edge = fullToSummaryEdge(e);
                                    return [
                                        '#' + toCyId(edge.edgeId),
                                        '#' + toCyId(
                                            edge.outVertexId +
                                            edge.inVertexId +
                                            edge.label
                                        )
                                    ];
                                })
                            )
                        )
                        .flatten()
                        .value()
                        .join(',')
                    ).select();
                }

                setTimeout(function() {
                    this.ignoreCySelectionEvents = false;
                }.bind(this), SELECTION_THROTTLE * 1.5);

                if (cyNodes && cyNodes.length) {
                    this.nodesToFitAfterGraphPadding = cyNodes;
                }
            });
        };

        this.onVerticesUpdated = function(evt, data) {
            var self = this;
            this.cytoscapeReady(function(cy) {
                // TODO: consider using batchData
                data.vertices
                    .forEach(function(updatedVertex) {
                        var cyNode = cy.nodes().filter('#' + toCyId(updatedVertex));
                        if (cyNode.length) {
                            cyNode = cyNode[0];
                            var newData = self.updateCyNodeData(cyNode.data(), updatedVertex);
                            cyNode.data(newData);

                            _.each(cyNode._private.classes, function(enabled, name) {
                                cyNode.removeClass(name);
                            });
                            var classes = self.classesForVertex(updatedVertex);
                            if (classes.length) {
                                cyNode.addClass(classes);
                            }
                        }
                    });
            });

            this.setWorkspaceDirty();
        };

        this.animateFromToNode = function(cyFromNode, cyToNode, delay) {
            var self = this,
                cy = cyFromNode.cy();

            if (cyToNode && cyToNode.length) {
                cyFromNode
                    .css('opacity', 1.0)
                    .stop(true)
                    .delay(delay)
                    .animate(
                        {
                            position: cyToNode.position()
                        },
                        _.extend({}, CYTOSCAPE_ANIMATION, {
                            complete: function() {
                                cyFromNode.remove();
                            }
                        })
                    );
            } else {
                cyFromNode.remove();
            }
        };

        this.onContextMenuExportWorkspace = function(exporterId) {
            var exporter = _.findWhere(
                    registry.extensionsForPoint(GRAPH_EXPORTER_POINT),
                    { _identifier: exporterId }
                ),
                $node = this.$node,
                workspaceId = this.previousWorkspace;

            if (exporter) {
                require(['util/popovers/exportWorkspace/exportWorkspace'], function(ExportWorkspace) {
                    ExportWorkspace.attachTo($node, {
                        exporter: exporter,
                        workspaceId: workspaceId,
                        anchorTo: {
                            page: {
                                x: window.lastMousePositionX,
                                y: window.lastMousePositionY
                            }
                        }
                    });
                });
            }
        };

        this.onContextMenuZoom = function(level) {
            this.cytoscapeReady(function(cy) {
                cy.zoom(level);
            });
        };

        this.onEdgesLoaded = function(evt, relationshipData) {
            this.cytoscapeReady(function(cy) {
                var self = this;

                if (relationshipData.edges) {
                    var relationshipEdges = [],
                        collapsedEdges = _.chain(relationshipData.edges)
                            .groupBy(generateCompoundEdgeId)
                            .values()
                            .map(function(e) {
                                return {
                                    id: generateCompoundEdgeId(e[0]),
                                    type: e[0].label,
                                    sourceId: e[0].outVertexId,
                                    targetId: e[0].inVertexId,
                                    edges: e
                                }
                            })
                            .value();

                    collapsedEdges.forEach(function(edge) {
                        var sourceNode = cy.getElementById(toCyId(edge.sourceId)),
                            destNode = cy.getElementById(toCyId(edge.targetId));

                        if (sourceNode.length && destNode.length) {
                            relationshipEdges.push(
                                cyEdgeFromEdge(edge, sourceNode, destNode, self.ontologyRelationships)
                            );
                        }
                    });

                    if (relationshipEdges.length) {
                        cy.edges().remove();
                        cy.add(relationshipEdges);
                    }
                }
            });
        };

        this.onEdgesUpdated = function(event, data) {
            this.cytoscapeReady(function(cy) {
                var self = this,
                    newEdges = _.compact(data.edges.map(function(fullEdge) {
                        var edge = fullToSummaryEdge(fullEdge),
                            cyEdge = cy.getElementById(toCyId(edge.outVertexId + edge.inVertexId + edge.label))
                        if (cyEdge.length) {
                            var edges = cyEdge.data('edges'),
                                ontology = self.ontologyRelationships.byTitle[cyEdge.data('type')];

                            edges.push(edge);
                            edges = _.unique(edges, false, _.property('edgeId'));
                            cyEdge.data('edges', edges);
                            cyEdge.data('label',
                                (ontology && ontology.displayName || '') + (
                                    (edges.length > 1) ?
                                        (' (' + F.number.pretty(edges.length) + ')') :
                                        ''
                                )
                            );
                        } else {
                            var sourceNode = cy.getElementById(toCyId(edge.outVertexId)),
                                destNode = cy.getElementById(toCyId(edge.inVertexId));

                            if (sourceNode.length && destNode.length) {
                                return cyEdgeFromEdge({
                                    id: generateCompoundEdgeId(edge),
                                    type: edge.label,
                                    sourceId: edge.outVertexId,
                                    targetId: edge.inVertexId,
                                    edges: [edge]
                                }, sourceNode, destNode, self.ontologyRelationships);
                            }
                        }
                    }));

                if (newEdges.length) {
                    cy.add(newEdges);
                }
            });
        };

        this.onEdgesDeleted = function(event, data) {
            this.cytoscapeReady(function(cy) {
                var self = this;
                _.each(cy.edges(), function(cyEdge) {
                    var edges = _.reject(cyEdge.data('edges'), function(e) {
                            return e.edgeId === data.edgeId
                        }),
                        ontology = self.ontologyRelationships.byTitle[cyEdge.data('type')];

                    if (edges.length) {
                        cyEdge.data('edges', edges);
                        cyEdge.data('label',
                            (ontology && ontology.displayName || '') + (
                                (edges.length > 1) ?
                                    (' (' + F.number.pretty(edges.length) + ')') :
                                    ''
                            )
                        );
                    } else {
                        cyEdge.remove();
                    }
                });
            });
        };

        this.onContextMenuFitToWindow = function() {
            this.fit(null, null, { animate: true });
        };

        this.onContextMenuCreateVertex = function() {
            var menu = this.select('contextMenuSelector');
            this.createVertex(menu.offset());
        }

        this.onDevicePixelRatioChanged = function() {
            this.cytoscapeReady(function(cy) {
                cy.renderer().updatePixelRatio();
                this.fit(cy);
            });
        };

        this.fit = function(cy, nodes, options) {
            var self = this;
            return Promise.resolve(cy || this.cytoscapeReady())

                .then(function(cy) {
                    if (!nodes || !nodes.length) {
                        nodes = cy.elements();
                    }
                    if (nodes.size() === 0) {
                        cy.reset();
                    } else if (self.graphPadding) {
                        var $$ = cytoscape,
                            elements = nodes,
                            bb = elements.boundingBox(),
                            style = cy.style(),
                            padding = _.extend({}, self.graphPadding),
                            pixelScale = cy.renderer().options.pixelRatio,
                            w = parseFloat(style.containerCss('width')),
                            h = parseFloat(style.containerCss('height')),
                            zoom;

                        if (!_.isObject(padding)) {
                            if (!_.isNumber(padding)) padding = 0;
                            padding = { t: padding, r: padding, b: padding, l: padding};
                        }
                        padding.t = (padding.t || 0);
                        padding.r = (padding.r || 0);
                        padding.b = (padding.b || 0);
                        padding.l = (padding.l || 0);

                        if (!isNaN(w) && !isNaN(h)) {
                            zoom = Math.min(
                                (w - (padding.l + padding.r)) / bb.w,
                                (h - (padding.t + padding.b)) / bb.h
                            );

                            // Set min and max zoom to fit all items
                            if (zoom < cy._private.minZoom) {
                                cy._private.minZoom = zoom;
                                cy._private.maxZoom = 1 / zoom;
                            } else {
                                cy._private.minZoom = cy._private.originalMinZoom;
                                cy._private.maxZoom = cy._private.originalMaxZoom;
                            }

                            if (zoom > cy._private.maxZoom) zoom = cy._private.maxZoom;

                            var position = {
                                    x: (w + padding.l - padding.r - zoom * (bb.x1 + bb.x2)) / 2,
                                    y: (h + padding.t - padding.b - zoom * (bb.y1 + bb.y2)) / 2
                                },
                                _p = cy._private;

                            if (options && options.animate) {
                                return new Promise(function(f) {
                                    cy.animate({
                                        zoom: zoom,
                                        pan: position
                                    }, _.extend({}, CYTOSCAPE_ANIMATION, {
                                        queue: false,
                                        complete: function() {
                                            f();
                                        }
                                    }));
                                })
                            } else {
                                _p.zoom = zoom;
                                _p.pan = position;
                                cy.trigger('pan zoom viewport');
                                cy.notify({ // notify the renderer that the viewport changed
                                    type: 'viewport'
                                });
                            }
                        }
                    }
                });
        };

        this.cyNodesForVertexIds = function(cy, vertexIds) {
            if (_.isEmpty(vertexIds)) return cy.collection();
            var selector = vertexIds.map(function(vId) {
                return '#' + toCyId(vId);
            }).join(',');

            return cy.nodes(selector);
        };

        this.cyEdgesForEdgeIds = function(cy, edgeIds) {
            if (_.isEmpty(edgeIds)) return cy.collection();
            var selector = _.compact(edgeIds.map(function(eId) {
                var cyEdgeId = edgeIdToGroupedCyEdgeId[eId];
                if (cyEdgeId) {
                    return '#' + cyEdgeId;
                }
            }));

            if (selector.length) {
                return cy.edges(selector.join(','));
            }

            return cy.collection();
        };

        this.onFocusElements = function(e, data) {
            this.cytoscapeReady(function(cy) {
                this.hoverDelay = _.delay(function() {
                    cy.elements('.focus').removeClass('focus');
                    this.cyNodesForVertexIds(cy, data.vertexIds || []).addClass('focus');
                    this.cyEdgesForEdgeIds(cy, data.edgeIds || []).addClass('focus');
                }.bind(this), HOVER_FOCUS_DELAY_SECONDS * 1000);
            });
        };

        this.onDefocusElements = function(e, data) {
            clearTimeout(this.hoverDelay);
            this.cytoscapeReady(function(cy) {
                cy.elements('.focus').removeClass('focus');
            });
        };

        this.onFocusPaths = function(e, data) {
            this.cytoscapeReady(function(cy) {
                var paths = data.paths,
                    sourceId = data.sourceId,
                    targetId = data.targetId;

                cy.$('.path-edge').removeClass('path-edge path-hidden-verts');
                cy.$('.path-temp').remove();

                paths.forEach(function(path, i) {
                    var vertexIds = _.chain(path)
                                .filter(function(v) {
                                    return v !== sourceId && v !== targetId;
                                })
                                .value(),
                        end = colorjs('#0088cc').shiftHue(i * (360 / paths.length)).toCSSHex(),
                        lastNode = cy.getElementById(toCyId(sourceId)),
                        count = 0,
                        existingOrNewEdgeBetween = function(node1, node2, count) {
                            var edge = node1.edgesWith(node2);
                            if (!edge.length || edge.removed() || edge.hasClass('path-edge')) {
                                edge = cy.add({
                                    group: 'edges',
                                    classes: 'path-edge path-temp' + (count ? ' path-hidden-verts' : ''),
                                    id: node1.id() + '-' + node2.id() + 'path=' + i,
                                    data: {
                                        source: node1.id(),
                                        target: node2.id(),
                                        pathColor: end,
                                        label: count === 0 ? '' :
                                            i18n('graph.path.edge.label.' + (
                                                count === 1 ? 'one' : 'some'
                                            ), F.number.pretty(count))
                                    }
                                });
                            } else {
                                edge.addClass('path-edge');
                                edge.data('pathColor', end);
                            }
                            return edge;
                        };

                    vertexIds.forEach(function(vId, i) {
                        var thisNode = cy.getElementById(toCyId(vId));
                        if (thisNode.length && !thisNode.removed()) {
                            existingOrNewEdgeBetween(lastNode, thisNode, count);
                            lastNode = thisNode;
                            count = 0;
                        } else count++;
                    });

                    existingOrNewEdgeBetween(lastNode, cy.getElementById(toCyId(targetId)), count);
                });
            });
        };

        this.onDefocusPaths = function(e, data) {
            this.cytoscapeReady(function(cy) {
                cy.$('.path-edge').removeClass('path-edge path-hidden-verts');
                cy.$('.path-temp').remove();
            });
        };

        this.onGraphPaddingUpdated = function(e, data) {
            var self = this;

            this.graphPaddingRight = data.padding.r;

            var padding = $.extend({}, data.padding);

            padding.r += this.select('graphToolsSelector').outerWidth(true) || 65;
            padding.l += GRAPH_PADDING_BORDER;
            padding.t += GRAPH_PADDING_BORDER;
            padding.b += GRAPH_PADDING_BORDER;
            this.graphPadding = padding;

            this.updateGraphViewsPosition();

            if (this.nodesToFitAfterGraphPadding) {
                this.cytoscapeReady().done(function(cy) {
                    self.fitToNodesIfOutsideViewport(cy, self.nodesToFitAfterGraphPadding);

                    self.nodesToFitAfterGraphPadding = null;
                });
            }
        };

        this.fitToNodesIfOutsideViewport = function(cy, nodes) {
            var bb = nodes.boundingBox(),
                e = cy.extent(),
                overlap = 100,
                zoom = cy._private.zoom;

            e.x1 += (this.graphPadding.l - overlap) / zoom;
            e.x2 -= (this.graphPadding.r - overlap) / zoom;
            e.y1 += (this.graphPadding.t - overlap) / zoom;
            e.y2 -= (this.graphPadding.b - overlap) / zoom;

            var left = bb.x1 > e.x1,
                right = bb.x2 < e.x2,
                top = bb.y1 > e.y1,
                bottom = bb.y2 < e.y2;

            if (!(left && right && top && bottom)) {
                this.fit(cy, null, { animate: true });
            }
        };

        this.updateGraphViewsPosition = function() {
            this.select('graphViewsSelector').css({
                left: (this.graphPadding.l - GRAPH_PADDING_BORDER) + 'px',
                right: (this.graphPaddingRight) + 'px'
            });
        };

        this.onContextMenuLayout = function(layout, opts) {
            this.trigger('toggleSnapToGrid', {
                snapToGrid: false
            });
            this.runLayout(layout, undefined, opts);
        };

        this.runLayout = function(layout, elements, options) {
            var self = this;

            this.cytoscapeReady(function(cy) {
                if (options && options.onlySelected) {
                    elements = cy.collection(cy.nodes().filter(':selected'));
                }
                var opts = $.extend({
                    name: layout,
                    fit: false,
                    stop: function() {
                        var updates = $.map(elements || cy.nodes(), function(vertex) {
                            return {
                                vertexId: fromCyId(vertex.id()),
                                graphPosition: retina.pixelsToPoints(vertex.position())
                            };
                        });
                        self.trigger('updateWorkspace', {
                            entityUpdates: updates
                        });
                        if (!elements) {
                            self.fit(cy, null, { animate: true });
                        }
                    }
                }, _.each(LAYOUT_OPTIONS[layout] || {}, function(optionValue, optionName) {
                    if (_.isFunction(optionValue)) {
                        LAYOUT_OPTIONS[layout][optionName] = optionValue(elements || cy.nodes(), options);
                    }
                }), options);

                if (elements) {
                    elements.layout(opts);
                } else {
                    cy.layout(opts);
                }
            });
        };

        this.onContextMenuSelect = function(select) {
            this.cytoscapeReady(function(cy) {
                if (select === 'all') {
                    cy.nodes().filter(':unselected').select();
                } else if (select === 'none') {
                    cy.nodes().filter(':selected').unselect();
                } else if (select === 'invert') {
                    var selected = cy.nodes().filter(':selected'),
                        unselected = cy.nodes().filter(':unselected');
                    selected.unselect();
                    unselected.select();
                } else {
                    var selector = _.findWhere(
                        registry.extensionsForPoint('org.visallo.graph.selection'),
                        { identifier: select }
                    );
                    if (selector) {
                        selector(cy);
                    }
                }
            });
        };

        this.graphTap = throttle('selection', SELECTION_THROTTLE, function(event) {
            this.trigger('defocusPaths');
            this.trigger('defocusVertices');

            if (event.cyTarget === event.cy) {
                this.trigger('selectObjects');
            }
        });

        this.graphContextTap = function(event) {
            var self = this,
                selectedObjects = visalloData.selectedObjects,
                selectionHasVertices = selectedObjects.vertices.length,
                selectionHasEdges = selectedObjects.edges.length,
                cyTargetIsElement = event.cyTarget !== event.cy,
                menu;

            if (event.cyTarget === event.cy) {
                menu = this.select('contextMenuSelector');
                this.select('vertexContextMenuSelector').blur().parent().removeClass('open');
                this.trigger('closeVertexMenu');
            } else if (selectionHasVertices || (
                cyTargetIsElement && event.cyTarget.group() === 'nodes'
            )) {
                this.select('contextMenuSelector').blur().parent().removeClass('open');

                var originalEvent = event.originalEvent;
                if (!cyTargetIsElement || !(/^(NEW|controlDragNodeId)/).test(event.cyTarget.id())) {
                    this.trigger(this.select('cytoscapeContainerSelector')[0], 'showVertexContextMenu', {
                        vertexId: cyTargetIsElement ?
                            fromCyId(event.cyTarget.id()) :
                            selectedObjects.vertices[0].id,
                        position: {
                            x: originalEvent.pageX,
                            y: originalEvent.pageY
                        }
                    });
                }

                return;
            } else {
                this.select('vertexContextMenuSelector').blur().parent().removeClass('open');
                this.select('contextMenuSelector').blur().parent().removeClass('open');
            }

            if (menu) {
                // Show/Hide the layout selection menu item
                if (event.cy.nodes().filter(':selected').length) {
                    menu.find('.layouts-multi').show();
                } else {
                    menu.find('.layouts-multi').hide();
                }

                if (menu.is(self.attr.contextMenuSelector)) {
                    var graphExporters = registry.extensionsForPoint(GRAPH_EXPORTER_POINT);

                    if (graphExporters.length) {
                        var $exporters = menu.find('.exporters');

                        if ($exporters.length === 0) {
                            $exporters = $('<li class="dropdown-submenu"><a>' +
                                i18n('graph.contextmenu.export_workspace') +
                                '</a>' +
                                '<ul class="dropdown-menu exporters"></ul></li>'
                            ).appendTo(menu).before('<li class="divider"></li>').find('ul');
                        }

                        $exporters.empty();
                        graphExporters.forEach(function(exporter, i) {
                            exporter._identifier = 'EXPORTER_' + i;
                            $exporters.append(
                                $('<li><a href="#"></a></li>')
                                    .find('a')
                                    .text(exporter.menuItem)
                                    .attr('data-func', 'exportWorkspace')
                                    .attr('data-args', JSON.stringify([exporter._identifier]))
                                    .end()
                            );
                        });
                    }

                    var graphSelections = registry.extensionsForPoint('org.visallo.graph.selection');
                    if (graphSelections.length) {
                        var $selectorMenu = menu.find('.selectors .dropdown-menu');
                        $selectorMenu.find('.plugin').remove();
                        var selected = event.cy.nodes().filter(':selected').length > 0;
                        graphSelections.forEach(function(selector) {
                            if ((selected && _.contains(['always', 'selected'], selector.visibility)) ||
                                (!selected && _.contains(['always', 'none-selected'], selector.visibility))) {

                                $selectorMenu.append(
                                    $('<li class="plugin"><a href="#" tabindex="-1"></a></li>')
                                        .find('a')
                                        .text(i18n('graph.selector.' + selector.identifier + '.displayName'))
                                        .attr('data-func', 'select')
                                        .attr('data-args', '["' + selector.identifier + '"]')
                                        .end()
                                );
                            }
                        });
                    }

                    var graphLayouts = registry.extensionsForPoint('org.visallo.graph.layout');
                    if (graphLayouts.length) {
                        var appendLayoutMenuItems = function($layoutMenu, onlySelected) {
                            var onlySelectedArg = onlySelected ? ',{"onlySelected":true}' : '';

                            $layoutMenu.find('.plugin').remove();

                            graphLayouts.forEach(function(layout) {
                                $layoutMenu.append(
                                    $('<li class="plugin"><a href="#" tabindex="-1"></a></li>')
                                        .find('a')
                                        .text(i18n('graph.layout.' + layout.identifier + '.displayName'))
                                        .attr('data-func', 'layout')
                                        .attr('data-args', '["' + layout.identifier + '"' + onlySelectedArg + ']')
                                        .end()
                                );
                            });
                        };

                        appendLayoutMenuItems(menu.find('.layouts .dropdown-menu'), false);
                        appendLayoutMenuItems(menu.find('.layouts-multi .dropdown-menu'), true);
                    }
                }
                menu.find('.layouts, .layouts-multi, .layouts li, .layouts-multi li').toggleClass('disabled', visalloData.currentWorkspaceEditable === false);

                this.toggleMenu({positionUsingEvent: event}, menu);
            }
        };

        this.graphSelect = throttle('selection', SELECTION_THROTTLE, function(event) {
            if (this.ignoreCySelectionEvents) return;
            this.updateVertexSelections(event.cy, event.cyTarget);
        });

        this.graphUnselect = throttle('selection', SELECTION_THROTTLE, function(event) {
            if (this.ignoreCySelectionEvents) return;
            this.updateVertexSelections(event.cy, event.cyTarget);
        });

        this.updateVertexSelections = function(cy, cyTarget) {
            var self = this,
                nodes = cy.nodes().filter(':selected').not('.temp'),
                edges = cy.edges().filter(':selected').not('.temp'),
                edgeIds = [],
                vertexIds = cyTarget.vertexIds || [];

            // TODO: shift selection breaks this
            //if (shiftKey) {
                //nodes = nodes.add(self.previousSelectionNodes)
                //if (cyTarget !== cy && cyTarget.length && self.previousSelectionNodes &&
                   //self.previousSelectionNodes.anySame(cyTarget)) {

                    //nodes = nodes.not(cyTarget)
                //}
            //}

            self.previousSelectionNodes = nodes;
            self.previousSelectionEdges = edges;

            nodes.each(function(index, cyNode) {
                if (!cyNode.hasClass('temp') && !cyNode.hasClass('path-edge')) {
                    vertexIds.push(fromCyId(cyNode.id()));
                }
            });

            edges.each(function(index, cyEdge) {
                if (!cyEdge.hasClass('temp') && !cyEdge.hasClass('path-edge')) {
                    edgeIds = edgeIds.concat(_.pluck(cyEdge.data('edges'), 'edgeId'));
                }
            });

            this.trigger('selectObjects', {
                vertexIds: vertexIds,
                edgeIds: edgeIds
            });

        };

        this.graphGrab = function(event) {
            var self = this;
            this.trigger('defocusPaths');
            this.cytoscapeReady(function(cy) {
                var vertices = event.cyTarget.selected() ? cy.nodes().filter(':selected') : event.cyTarget;
                this.grabbedVertices = vertices.not('.temp,#controlDragNodeId').each(function() {
                    var p = retina.pixelsToPoints(this.position());
                    this.data('originalPosition', { x: p.x, y: p.y });
                    this.data('freed', false);
                });
            });
        };

        this.graphFree = function(event) {
            if (!this.isWorkspaceEditable) return;
            var self = this,
                dup = true, // CY is sending multiple "free" events, prevent that...
                vertices = this.grabbedVertices;

            if (!vertices || vertices.length === 0) return;

            var cy = vertices[0].cy(),
                verticesMoved = [],
                targetPosition,
                snapToGrid = visalloData.currentUser.uiPreferences.snapToGrid === 'true',
                calcPosition = function(cyNode) {
                    var p = retina.pixelsToPoints(cyNode.position());
                    return {
                        x: Math.round(p.x),
                        y: Math.round(p.y)
                    };
                };

            cy.startBatch()
            vertices.each(function(i, vertex) {
                var cyId = vertex.id(),
                    pCopy;

                if (i === 0) {
                    pCopy = calcPosition(vertex);
                    targetPosition = pCopy;
                }
                if (snapToGrid) {
                    pCopy = snapPosition(vertex);
                } else if (!pCopy) {
                    pCopy = calcPosition(vertex);
                }

                if (!vertex.data('freed')) {
                    dup = false;
                }

                // Rounding can cause vertex to jump 1/2 pixel
                // Jump immediately instead of after save
                if (snapToGrid) {
                    vertex.animate({
                        position: retina.pointsToPixels(pCopy)
                    }, CYTOSCAPE_ANIMATION)
                } else {
                    vertex.position(retina.pointsToPixels(pCopy));
                }
                vertex.data('freed', true)

                verticesMoved.push({
                    vertexId: fromCyId(cyId),
                    graphPosition: pCopy
                });
            });

            cy.endBatch();

            if (dup) {
                return;
            }

            // If the user didn't drag more than a few pixels, select the
            // object, it could be an accidental mouse move
            var target = vertices[0],
                originalPosition = target.data('originalPosition'),
                dx = targetPosition.x - originalPosition.x,
                dy = targetPosition.y - originalPosition.y,
                distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 5) {
                event.cyTarget.select();
            }

            if (distance > 0) {
                this.trigger('updateWorkspace', {
                    entityUpdates: verticesMoved
                });
                this.setWorkspaceDirty();
            }
        };

        this.graphMouseOver = function(event) {
            var self = this,
                cyNode = event.cyTarget;

            clearTimeout(this.mouseoverTimeout);

            if (cyNode !== event.cy && cyNode.group() === 'nodes') {
                this.mouseoverTimeout = _.delay(function() {
                    var nId = cyNode.id();
                    if (/^(NEW|controlDragNodeId)/.test(nId)) {
                        return;
                    }

                    self.dataRequest('vertex', 'store', { vertexIds: fromCyId(nId) })
                        .done(function(vertex) {
                            var truncatedTitle = cyNode.data('truncatedTitle');

                            if (vertex) {
                                cyNode.data('previousTruncated', truncatedTitle);
                                cyNode.data('truncatedTitle', F.vertex.title(vertex));
                            }
                        })
                }, 500);
            }
        };

        this.graphMouseOut = function(event) {
            clearTimeout(this.mouseoverTimeout);
            if (event.cyTarget !== event.cy) {
                event.cyTarget.data('truncatedTitle', event.cyTarget.data('previousTruncated'));
            }
        };

        this.setWorkspaceDirty = function() {
            this.checkEmptyGraph();
        };

        this.updateCytoscapeControlBehavior = function() {
            this.cytoscapeReady(function(cy) {
                var noVertices = cy.nodes().length === 0;

                if (noVertices) {
                    cy.boxSelectionEnabled(false)
                        .userPanningEnabled(false)
                        .userZoomingEnabled(false);
                } else {
                    cy.boxSelectionEnabled(true)
                        .userPanningEnabled(true)
                        .userZoomingEnabled(true);
                }
            });
        };

        this.checkEmptyGraph = function() {
            this.cytoscapeReady(function(cy) {
                var noVertices = cy.nodes().length === 0;

                this.select('emptyGraphSelector').toggle(noVertices);
                this.updateCytoscapeControlBehavior();
                this.select('graphToolsSelector').toggle(!noVertices);
                if (noVertices) {
                    cy.reset();
                }
            });
        };

        this.resetGraph = function() {
            this.cytoscapeReady(function(cy) {
                cy.elements().remove();
                this.setWorkspaceDirty();
            });
        };

        this.hideLoading = function() {
            var loading = this.$node.find('.loading-graph');
            if (loading.length) {
                loading.on(TRANSITION_END, function(e) {
                    loading.remove();
                });
                loading.addClass('hidden');
                _.delay(function() {
                    loading.remove();
                }, 2000);
            }
        };

        this.getNodesByVertexIds = function(cy, list, optionalVertexIdAccessor) {
            if (list.length === 0) {
                return cy.collection();
            }

            return cy.$(
                list.map(function(obj) {
                    return '#' + toCyId(optionalVertexIdAccessor ? obj[optionalVertexIdAccessor] : obj);
                }).join(',')
            );
        };

        // Delete entities before saving (faster feeling interface)
        this.onUpdateWorkspace = function(cy, event, data) {
            if (data.options && data.options.selectAll && data.entityUpdates) {
                this.vertexIdsToSelect = _.pluck(data.entityUpdates, 'vertexId');
            }
            if (data && data.entityDeletes && data.entityDeletes.length) {
                cy.$(
                    data.entityDeletes.map(function(vertexId) {
                    return '#' + toCyId(vertexId);
                }).join(',')).remove();

                this.setWorkspaceDirty();
                this.updateVertexSelections(cy);
            }
            if (data && data.entityUpdates && data.entityUpdates.length && this.$node.closest('.visible').length) {
                data.entityUpdates.forEach(function(entityUpdate) {
                    if ('graphLayoutJson' in entityUpdate && 'pagePosition' in entityUpdate.graphLayoutJson) {
                        var projectedPosition = cy.renderer().projectIntoViewport(
                            entityUpdate.graphLayoutJson.pagePosition.x,
                            entityUpdate.graphLayoutJson.pagePosition.y
                        );

                        entityUpdate.graphPosition = retina.pixelsToPoints({
                            x: projectedPosition[0],
                            y: projectedPosition[1]
                        });
                        delete entityUpdate.graphLayoutJson;
                    }
                });
            }
        }

        this.onWorkspaceUpdated = function(event, data) {
            if (visalloData.currentWorkspaceId === data.workspace.workspaceId) {
                this.isWorkspaceEditable = data.workspace.editable;
                this.cytoscapeReady(function(cy) {
                    var self = this,
                        fitToIds = [],
                        allNodes = cy.nodes();

                    allNodes[data.workspace.editable ? 'grabify' : 'ungrabify']();

                    data.entityUpdates.forEach(function(entityUpdate) {
                        var cyNode = cy.getElementById(toCyId(entityUpdate.vertexId)),
                            previousWorkspaceVertex = self.workspaceVertices[entityUpdate.vertexId];

                        if (cyNode.length && !cyNode.grabbed() && ('graphPosition' in entityUpdate)) {
                            cyNode
                                .stop(true)
                                .animate(
                                    {
                                        position: retina.pointsToPixels(entityUpdate.graphPosition)
                                    },
                                    CYTOSCAPE_ANIMATION
                                );
                        }

                        var noPreviousGraphPosition = (
                                !previousWorkspaceVertex ||
                                !('graphPosition' in previousWorkspaceVertex)
                            ),
                            nowHasGraphPosition = 'graphPosition' in entityUpdate,
                            newVertex = !!(noPreviousGraphPosition && nowHasGraphPosition);

                        if (previousWorkspaceVertex &&
                            previousWorkspaceVertex.graphLayoutJson &&
                            previousWorkspaceVertex.graphLayoutJson.relatedToVertexId) {
                            fitToIds.push(previousWorkspaceVertex.graphLayoutJson.relatedToVertexId)
                        }
                        if (newVertex) {
                            fitToIds.push(entityUpdate.vertexId);
                        }

                        self.workspaceVertices[entityUpdate.vertexId] = entityUpdate;
                    });
                    self.workspaceVertices = _.omit(self.workspaceVertices, data.entityDeletes);

                    this.getNodesByVertexIds(cy, data.entityDeletes).remove();
                    if (data.newVertices) {
                        if (this.cyNodesToRemoveOnWorkspaceUpdated) {
                            this.cyNodesToRemoveOnWorkspaceUpdated.remove();
                            this.cyNodesToRemoveOnWorkspaceUpdated = null;
                        }
                        this.addVertices(data.newVertices, {
                            fitToVertexIds: _.unique(fitToIds)
                        })
                    }
                    this.setWorkspaceDirty();
                    this.updateVertexSelections(cy, {
                        vertexIds: _.unique(fitToIds)
                    });
                });
            }
        }

        this.onWorkspaceLoaded = function(evt, workspace) {
            this.resetGraph();
            this.isWorkspaceEditable = workspace.editable;
            this.workspaceVertices = workspace.vertices;
            if (workspace.data.vertices.length) {
                var newWorkspace = !this.previousWorkspace || this.previousWorkspace !== workspace.workspaceId;
                this.addVertices(workspace.data.vertices, {
                    fit: newWorkspace,
                    animate: false
                });
            } else {
                this.hideLoading();
                this.checkEmptyGraph();
            }

            this.previousWorkspace = workspace.workspaceId;
        };

        this.onMenubarToggleDisplay = function(e, data) {
            if (data.name === 'graph' && this.$node.is(':visible')) {
                this.cytoscapeReady(function(cy) {
                    cy.renderer().notify({type: 'viewport'});

                    if (this.fitOnMenubarToggle) {
                        this.fit(cy);
                        this.fitOnMenubarToggle = false;
                    }
                });
            }
        };

        this.onRegisterForPositionChanges = function(event, data) {
            var self = this,
                anchorTo = data && data.anchorTo;

            if (!anchorTo || (!anchorTo.vertexId && !anchorTo.page)) {
                return console.error('Registering for position events requires a vertexId');
            }

            this.cytoscapeReady().done(function(cy) {

                event.stopPropagation();

                var cyNode = anchorTo.vertexId && cy.getElementById(toCyId(anchorTo.vertexId)),
                    offset = self.$node.offset(),
                    cyPosition = anchorTo.page && cy.renderer().projectIntoViewport(
                        anchorTo.page.x + offset.left,
                        anchorTo.page.y + offset.top
                    );

                if (!self.onViewportChangesForPositionChanges) {
                    self.onViewportChangesForPositionChanges = function() {
                        var position;

                        if (anchorTo.vertexId) {
                            var positionInNode = cyNode.renderedPosition();

                            position = {
                                x: positionInNode.x + offset.left,
                                y: positionInNode.y + offset.top
                            };

                        } else if (anchorTo.page) {
                            position = {
                                x: cyPosition[0] * cy.zoom() + cy.pan().x,
                                y: cyPosition[1] * cy.zoom() + cy.pan().y
                            };
                        }

                        self.trigger(event.target, 'positionChanged', { position: position });
                    };
                }

                cy.on('pan zoom position', self.onViewportChangesForPositionChanges);
                self.onViewportChangesForPositionChanges();
            })
        };

        this.onUnregisterForPositionChanges = function(event, data) {
            var self = this;
            this.cytoscapeReady().done(function(cy) {
                if (self.onViewportChangesForPositionChanges) {
                    cy.off('pan zoom position', self.onViewportChangesForPositionChanges);
                    self.onViewportChangesForPositionChanges = null;
                }
            });
        };

        this.paddingForZoomOut = function() {
            return _.tap(_.extend({}, this.graphPadding), function(p) {
                var extra = 50;

                p.t += extra;
                p.b += extra;
                p.l += extra;
                // Right has the zoom controls overlap with it
            });
        }

        this.onShowPanel = function() {
            this.pauseAnimation(false);
        };

        this.onHidePanel = function() {
            this.pauseAnimation(true);
        };

        this.onShowMenu = function(event, data) {
            var self = this;

            this.cytoscapeReady()
                .done(function(cy) {
                    var offset = self.$node.offset(),
                        r = cy.renderer(),
                        pos = r.projectIntoViewport(
                            data.pageX,// - offset.left,
                            data.pageY// - offset.top
                        ),
                        near = r.findNearestElement(pos[0], pos[1], true);

                    self.graphContextTap({
                        cyTarget: near || cy,
                        cy: cy,
                        originalEvent: _.pick(data, 'pageX', 'pageY')
                    })
                });
        };

        this.onHideMenu = function(event) {
            this.trigger(document, 'closeVertexMenu');
            this.select('contextMenuSelector').blur().parent().removeClass('open');
        };

        this.createVertex = function(offset) {
            var self = this;
            if (Privileges.canEDIT) {
                require(['util/popovers/fileImport/fileImport'], function(CreateVertex) {
                    CreateVertex.attachTo(self.$node, {
                        anchorTo: {
                            page: {
                                x: offset.left,
                                y: offset.top
                            }
                        }
                    });
                });
            }
        };

        this.onCreateVertex = function(e, data) {
            this.createVertex({
                left: data && data.pageX || 0,
                top: data && data.pageY || 0
            })
        };

        this.pauseAnimation = function(enable) {
            var self = this;

            if (!enable) {
                this.cytoscapeReady().done(function(cy) {
                    self.fit(cy);
                    cy.resize();
                });
            }
            //var self = this;
            //if (enable) {
                //this.paused = true;
            //} else {
                //this.paused = false;
                //this.cytoscapeReady().done(function(cy) {
                    //cy.startAnimationLoop();
                    //self.fit(cy);
                //});
            //}
        };

        this.onDidToggleDisplay = function(event, data) {
            if (data.name === 'graph') {
                this.pauseAnimation(!data.visible);
            }
        };

        this.onToggleClickAndDrag = function(event, data) {
            this.updateCytoscapeControlBehavior();
        };

        this.onToggleSnapToGrid = function(event, data) {
            var self = this;

            if (!data || !data.snapToGrid) return;

            this.cytoscapeReady(function(cy) {
                cy.batch(function() {
                    var verticesMoved = [];
                    cy.nodes().forEach(function(cyNode) {
                        var points = snapPosition(cyNode),
                            p = retina.pointsToPixels(points),
                            cyId = cyNode.id();

                        cyNode.position(p);

                        if (!(/^(NEW|controlDragNodeId)/.test(cyId))) {
                            verticesMoved.push({
                                vertexId: fromCyId(cyId),
                                graphPosition: points
                            });
                        }
                    });

                    self.trigger('updateWorkspace', {
                        entityUpdates: verticesMoved
                    });
                });
            })
        };

        this.after('teardown', function() {
            this.$node.empty();
        });

        this.after('initialize', function() {
            var self = this;

            this.setupAsyncQueue('cytoscape');

            this.$node.html(loadingTemplate({}));

            registry.documentExtensionPoint(GRAPH_EXPORTER_POINT,
                'Add menu options to export graph / workspace',
                function(e) {
                    return ('menuItem' in e) && ('componentPath' in e);
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.selection',
                'Add custom graph selection menu items',
                function(e) {
                    return ('identifier' in e) &&
                        _.contains(
                            ['selected', 'none-selected', 'always'],
                            e.visibility
                        );
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.layout',
                'Add new cytoscape layouts to graph menu',
                function(e) {
                    return ('identifier' in e);
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.view',
                'Add components to graph container',
                function(e) {
                    return ('componentPath' in e);
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.node.class',
                'Function that can change cytoscape classes of nodes',
                function(e) {
                    return _.isFunction(e);
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.edge.class',
                'Function that can change cytoscape classes of edges',
                function(e) {
                    return _.isFunction(e);
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.node.transformer',
                'Function that can change cytoscape node structure',
                function(e) {
                    return _.isFunction(e);
                }
            );
            registry.documentExtensionPoint('org.visallo.graph.edge.transformer',
                'Function that can change cytoscape edge structure',
                function(e) {
                    return _.isFunction(e);
                }
            );

            this.cytoscapeReady(function(cy) {
                this.on(document, 'updateWorkspace', this.onUpdateWorkspace.bind(this, cy));
            });
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'verticesHovering', this.onVerticesHovering);
            this.on(document, 'verticesHoveringEnded', this.onVerticesHoveringEnded);
            this.on(document, 'verticesDropped', this.onVerticesDropped);
            this.on(document, 'verticesDeleted', this.onVerticesDeleted);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'devicePixelRatioChanged', this.onDevicePixelRatioChanged);
            this.on(document, 'menubarToggleDisplay', this.onMenubarToggleDisplay);
            this.on(document, 'focusVertices', this.onFocusElements);
            this.on(document, 'defocusVertices', this.onDefocusElements);
            this.on(document, 'focusElements', this.onFocusElements);
            this.on(document, 'defocusElements', this.onDefocusElements);
            this.on(document, 'focusPaths', this.onFocusPaths);
            this.on(document, 'defocusPaths', this.onDefocusPaths);
            this.on(document, 'edgesLoaded', this.onEdgesLoaded);
            this.on(document, 'edgesDeleted', this.onEdgesDeleted);
            this.on(document, 'edgesUpdated', this.onEdgesUpdated);
            this.on(document, 'didToggleDisplay', this.onDidToggleDisplay);

            this.on('registerForPositionChanges', this.onRegisterForPositionChanges);
            this.on('unregisterForPositionChanges', this.onUnregisterForPositionChanges);
            this.on('showPanel', this.onShowPanel);
            this.on('hidePanel', this.onHidePanel);
            this.on('showMenu', this.onShowMenu);
            this.on('hideMenu', this.onHideMenu);
            this.on('createVertex', this.onCreateVertex);
            this.on('toggleSnapToGrid', this.onToggleSnapToGrid);
            this.on('toggleClickAndDrag', this.onToggleClickAndDrag);
            this.on('contextmenu', function(e) {
                e.preventDefault();
            });
            this.on(window, 'keydown keyup', function(e) {
                var $target = $(e.target),
                    gKey = 71;

                if ($target.is('input,select,textarea:not(.clipboardManager)')) {
                    return;
                }

                if (e.type === 'keydown') {
                    if (self.gKeyPressed) {
                        var cKey = 67
                        if (e.which === cKey || e.which === gKey) {
                            self.cytoscapeReady(function(cy) {
                                var vertices = visalloData.selectedObjects.vertices, elements, options;
                                if (vertices.length) {
                                    elements = self.cyNodesForVertexIds(cy, _.pluck(vertices, 'id'));
                                    options = {
                                        boundingBox: elements.boundingBox()
                                    };
                                }
                                self.runLayout(e.which === gKey ? 'bettergrid' : 'circle', elements, options);
                            });
                        }
                    }
                    if (e.which === gKey && !e.shiftKey && !e.metaKey && !e.altKey) {
                        self.gKeyPressed = true;
                        _.delay(function() {
                            self.gKeyPressed = false;
                        }, 500)
                    }
                }
                if (e.type === 'keyup' && e.shiftKey) {
                    return;
                }

                this.updateCytoscapeControlBehavior()
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: i18n('graph.help.scope'),
                shortcuts: {
                    '-': { fire: 'zoomOut', desc: i18n('graph.help.zoom_out') },
                    '=': { fire: 'zoomIn', desc: i18n('graph.help.zoom_in') },
                    'alt-f': { fire: 'fit', desc: i18n('graph.help.fit') },
                    'alt-n': { fire: 'createVertex', desc: i18n('graph.help.create_vertex') }
                }
            });

            if (self.attr.vertices && self.attr.vertices.length) {
                this.select('emptyGraphSelector').hide();
                this.addVertices(self.attr.vertices);
            }

            this.dataRequest('ontology', 'ontology')
                .done(function(ontology) {
                    var concepts = ontology.concepts,
                        relationships = ontology.relationships,
                        templateData = {
                            firstLevelConcepts: concepts.entityConcept.children || [],
                            pathHopOptions: ['2', '3', '4']
                        };

                    self.$node.append(template(templateData)).find('.shortcut').each(function() {
                        var $this = $(this), command = $this.text();
                        $this.text(F.string.shortcut($this.text()));
                    });

                    self.bindContextMenuClickEvent();

                    Controls.attachTo(self.select('graphToolsSelector'), {
                        optionsComponentPath: 'graph/options/container',
                        optionsAttributes: {
                            cy: self.cytoscapeReady()
                        }
                    });

                    var $views = $();
                    self.updateGraphViewsPosition();
                    registry.extensionsForPoint('org.visallo.graph.view')
                        .forEach(function(view) {
                            var $view = $('<div>');
                            if (view.className) {
                                $view.addClass(view.className);
                            }
                            require([view.componentPath], function(View) {
                                View.attachTo($view);
                            })
                            $views = $views.add($view);
                        });
                    self.select('graphViewsSelector').append($views);

                    self.ontologyRelationships = relationships;
                    stylesheet(null, function(style) {
                        self.initializeGraph(style);
                    });
                    self.on(document, 'reapplyGraphStylesheet', function() {
                        this.cytoscapeReady(function(cy) {
                            stylesheet(cy.style().resetToDefault(), function(style) {
                                style.update();
                            });
                        })
                    })
                });
        });

        this.initializeGraph = function(style) {
            var self = this;

            cytoscape('layout', 'bettergrid', BetterGrid);
            registry.extensionsForPoint('org.visallo.graph.layout').forEach(function(layout) {
                cytoscape('layout', layout.identifier, layout);
            });

            cytoscape({
                minZoom: 1 / 16,
                maxZoom: 6,
                hideEdgesOnViewport: false,
                hideLabelsOnViewport: false,
                textureOnViewport: true,
                boxSelectionEnabled: true,
                panningEnabled: true,
                userPanningEnabled: true,
                zoomingEnabled: true,
                userZoomingEnabled: true,
                pixelRatio: retina.devicePixelRatio,
                motionBlur: false,
                container: this.select('cytoscapeContainerSelector').css({height: '100%'})[0],
                renderer: {
                    showFps: (function() {
                        try {
                            if (localStorage.getItem('graphfps')) {
                                return true;
                            }
                        } catch(e) { /*eslint no-empty:0*/ }

                        return false;
                    })()
                },
                style: style,

                ready: function() {
                    /*eslint consistent-this:0*/
                    var cy = this,
                        container = cy.container(),
                        options = cy.options();

                    cy.on({
                        tap: self.graphTap.bind(self),
                        select: self.graphSelect.bind(self),
                        unselect: self.graphUnselect.bind(self),
                        grab: self.graphGrab.bind(self),
                        free: self.graphFree.bind(self),
                        mouseover: self.graphMouseOver.bind(self),
                        mouseout: self.graphMouseOut.bind(self)
                    });

                    self.on('pan', function(e, data) {
                        e.stopPropagation();
                        cy.panBy(data.pan);
                    });
                    self.on('fit', function(e) {
                        e.stopPropagation();
                        self.fit(cy, null, { animate: true });
                    });

                    cy._private.originalMinZoom = cy._private.minZoom;
                    cy._private.originalMaxZoom = cy._private.maxZoom;

                    var zoomFactor = 0,
                        zoomAcceleration = 5.0,
                        zoomDamping = 0.8,
                        zoom = function(factor, dt) {
                            var zoom = cy._private.zoom,
                                w = self.$node.width(),
                                h = self.$node.height(),
                                pos = cy.renderer().projectIntoViewport(w / 2, h / 2);

                            cy.zoom({
                                level: zoom + factor * dt,
                                position: { x: pos[0], y: pos[1] }
                            })
                        },
                        lastTimeStamp = 0;

                    self.on('zoomOut zoomIn', function(e) {
                        var dt = e.timeStamp - lastTimeStamp;
                        lastTimeStamp = e.timeStamp;
                        if (dt < 30) {
                            dt /= 1000;
                            zoomFactor += zoomAcceleration * dt * zoomDamping;
                        } else {
                            dt = 1;
                            zoomFactor = 0.01;
                        }
                        zoom(zoomFactor * (e.type === 'zoomOut' ? -1 : 1), dt);
                    });
                },
                done: function() {
                    self.paused = false;
                    self.updateCytoscapeControlBehavior();
                    self.cytoscapeMarkReady(this);
                    self.trigger('cytoscapeReady', {
                        cy: this
                    });

                    if (self.$node.is('.visible')) {
                        setTimeout(function() {
                            self.fit();
                        }, 100);
                    } else self.fitOnMenubarToggle = true;
                }
            });
        };
    }

});

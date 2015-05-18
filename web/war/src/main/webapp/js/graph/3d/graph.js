
define([
    'flight/lib/component',
    './3djs/3djs',
    'util/vertex/formatters',
    'tpl!util/alert',
    'util/withDataRequest'
], function(defineComponent, $3djs, F, alertTemplate, withDataRequest) {
    'use strict';

    var MAX_TITLE_LENGTH = 15,
        imageCache = {};

    return defineComponent(Graph3D, withDataRequest);

    function loadImage(src) {
        if (imageCache[src]) {
            return imageCache[src];
        }

        var deferred = $.Deferred(),
            image = new Image();

        image.onload = function() {
            deferred.resolve(this);
        };
        image.onerror = function() {
            deferred.reject(arguments);
        };
        image.src = src;
        imageCache[src] = deferred.promise();
        return imageCache[src];
    }

    function Graph3D() {
        this.defaultAttrs({ });

        this.after('teardown', function() {
            imageCache = {};
            this.graphRenderer.teardown();
            this.$node.empty();
        });

        this.after('initialize', function() {
            var self = this;

            this.graph = new $3djs.Graph();
            var loadSuccess = this.load3djs();
            if (loadSuccess) {
                this.dataRequest('workspace', 'store')
                    .then(function(verticesById) {
                        var vertexIds = _.keys(verticesById);
                        self.dataRequest('vertex', 'store', { vertexIds: vertexIds })
                            .done(function(vertices) {
                                if (vertices.length) {
                                    self.addVertices(vertices);
                                }
                            })
                    });

                this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
                this.on(document, 'verticesDropped', this.onVerticesDropped);
                this.on(document, 'verticesDeleted', this.onVerticesDeleted);
                this.on(document, 'verticesUpdated', this.onVerticesUpdated);
                this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
                this.on(document, 'edgesLoaded', this.onEdgesLoaded);

                this.on(document, 'keyup', this.onKeyUp);

                this.on('showPanel', this.onShowPanel);
                this.on('hidePanel', this.onHidePanel);
            } else {
                this.$node.html(alertTemplate({error: i18n('graph.3d.webgl.error')}));
            }
        });

        this.onKeyUp = function(event) {
            var renderer = this.graphRenderer,
                repulsion = renderer._layout.repulsionMultiplier;

            if (event.which === 187) {
                // expand
                repulsion *= 1.5;
            } else if (event.which === 189) {
                repulsion *= 0.75;
            }

            renderer.updateLayoutOptions({repulsion: repulsion});
        };

        this.onWorkspaceUpdated = function(event, data) {
            var self = this;

            if (data.entityDeletes.length) {
                data.entityDeletes.forEach(function(v) {
                    self.graph.removeNode(v);
                });
                self.graph.needsUpdate = true;
            }

            if (data.newVertices.length) {
                self.addVertices(data.newVertices, data.workspace);
                self.graph.needsUpdate = true;
            }
        };

        this.onShowPanel = function() {
            if (this.graphRenderer) {
                this.graphRenderer.running = true;
                this.graphRenderer.continueAnimation();
            }
        };

        this.onHidePanel = function() {
            if (this.graphRenderer) {
                this.graphRenderer.running = false;
            }
        };

        this.onVerticesDropped = function(event, data) {
           if (!this.$node.is(':visible')) return;

           this.trigger('updateWorkspace', {
               entityUpdates: data.vertices.map(function(vertex) {
                   return {
                       vertexId: vertex.id,
                       graphLayoutJson: {
                           position3d: {
                               screen: data.dropPosition
                           }
                       }
                   };
               })
           });
        };

        this.addVertices = function(vertices, workspace) {
            var self = this,
                graph = this.graph,
                deferredImages = [];

            vertices.forEach(function(vertex) {
                var node = new $3djs.Graph.Node(vertex.id);

                node.data.vertex = vertex;
                node.data.icon = F.vertex.image(vertex, null, 256);

                if (node.data.icon) {
                    deferredImages.push(
                        loadImage(node.data.icon)
                            .done(function(image) {
                                var ratio = image.naturalWidth / image.naturalHeight,
                                    height = 150;

                                node.data.icon = image.src;
                                addToGraph(height * ratio, height, node);
                            })
                    );
                } else {
                    console.warn('No icon set for vertex: ', vertex);
                }
            });

            $.when.apply(null, deferredImages).done(function() {
                if (self.relationships && self.relationships.length) {
                    self.addEdges(self.relationships);
                }
                graph.needsUpdate = true;
            });
            function addToGraph(width, height, node) {
                node.data.iconWidth = width;
                node.data.iconHeight = height;
                node.data.label = F.string.truncate(F.vertex.title(node.data.vertex), 3);

                node.needsUpdate = true;
                node.layout = {};
                var workspaceVertex = workspace && workspace.vertices[node.data.vertex.id];
                if (vertices.length === 1 &&
                    workspaceVertex &&
                    workspaceVertex.graphLayoutJson &&
                    workspaceVertex.graphLayoutJson.position3d) {
                    var p3d = workspaceVertex.graphLayoutJson.position3d;
                    if (p3d.screen) {
                        node.layout = {};
                        node.position = self.graphRenderer.screenToWorld(p3d.screen.x, p3d.screen.y);
                    }
                }
                graph.addNode(node);
            }
        };

        this.onWorkspaceLoaded = function(evt, workspace) {
            var self = this,
                graph = this.graph;

            this.isWorkspaceEditable = workspace.editable;

            graph.reset();
            self.graphRenderer.reset();
            graph.needsUpdate = true;
            _.defer(function() {
                if (workspace.data && workspace.data.vertices) {
                    self.addVertices(workspace.data.vertices, workspace);
                }
            })
        };

        this.onVerticesDeleted = function(event, data) {
            var self = this;

            data.vertexIds.forEach(function(v) {
                self.graph.removeNode(v);
            });

            self.graph.needsUpdate = true;
        };

        this.onVerticesUpdated = function() {
        };

        this.onEdgesLoaded = function(event, data) {
            var graph = this.graph;

            if (data.edges) {
                this.relationships = data.edges;
                this.graph.edges.length = 0;
                this.addEdges(data.edges);
                this.graph.needsUpdate = true;
            }
        };

        this.addEdges = function(relationships) {
            var graph = this.graph,
                edges = graph.edges;

            edges.length = 0;
            relationships.forEach(function(r) {
                var source = graph.node(r.sourceVertexId),
                    target = graph.node(r.destVertexId);

                if (source && target) {
                    edges.push({
                        source: source,
                        target: target
                    });
                }
            });
        };

        this.load3djs = function() {
            var graph = this.graph,
                self = this,
                graphRenderer = new $3djs.GraphRenderer(this.node);

            if (graphRenderer.browserSupported) {
                this.graphRenderer = graphRenderer;
                graphRenderer.renderGraph(this.graph);
                graphRenderer.addToRenderLoop();
                //graphRenderer.showStats();

                graphRenderer.addEventListener('node_click', function(event) {
                    var selected = [];
                    if (event.content) {
                        var data = graph.node(event.content).data.vertex;
                        selected.push(data);
                    }
                    self.trigger('selectObjects', { vertices: selected });
                }, false);

                return true;
            }

            return false;
        };
    }
});

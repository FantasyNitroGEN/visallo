
define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'tpl!./list',
    './detail-relationship-item',
    './vertex-item',
    './edge-item',
    'tpl!util/alert',
    'util/requirejs/promise!util/service/ontologyPromise',
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/popovers/withElementScrollingPositionUpdates',
    'util/jquery.withinScrollable',
    'util/jquery.ui.draggable.multiselect'
], function(
    defineComponent,
    registry,
    template,
    DetailRelationshipItem,
    VertexItem,
    EdgeItem,
    alertTemplate,
    ontologyPromise,
    F,
    withDataRequest,
    withPositionUpdates) {
    'use strict';

    var EXTENSION_POINT_NAME = 'org.visallo.entity.listItemRenderer';
    registry.documentExtensionPoint(EXTENSION_POINT_NAME,
        'Implement custom implementations for rendering items into element lists. ' +
        'This allows plugins to adjust how list items are displayed in search results, details panels, ' +
        'or anywhere else the lists are used.',
        function(e) {
            return _.isFunction(e.canHandle) && (e.component || e.componentPath);
        }
    );

    return defineComponent(List, withPositionUpdates, withDataRequest);

    function List() {

        this.defaultAttrs({
            itemSelector: 'ul > li.element-item',
            infiniteScrolling: false,
            usageContext: 'search'
        });

        this.stateForItem = function($item) {
            var $a = $item.children('a'),
                vertexId = $a.data('vertexId'),
                edgeId = $a.data('edgeId'),
                inWorkspace = (vertexId && vertexId in this.workspaceVertices) ||
                              (edgeId && edgeId in this.workspaceEdges),
                inMap = false;

            if (vertexId && inWorkspace) {
                return this.dataRequest('vertex', 'store', { vertexIds: [vertexId] }).then(function(vertices) {
                    inMap = vertices.length && _.some(vertices[0].properties, function(p) {
                        var ontologyProperty = ontologyPromise.properties.byTitle[p.name];
                        return ontologyProperty && ontologyProperty.dataType === 'geoLocation';
                    });
                    return { inGraph: inWorkspace, inMap: inMap };
                });
            }

            return Promise.resolve({ inGraph: inWorkspace, inMap: inMap });
        };

        this.after('initialize', function() {
            var self = this;

            // Do this to support code that arrives here via the
            // deprecated vertex/list and edge/list components
            this.attr.items = this.attr.items || this.attr.edges || this.attr.vertices;

            this.workspaceEdges = _.indexBy(visalloData.workspaceEdges, 'edgeId');
            this.renderers = registry.extensionsForPoint(EXTENSION_POINT_NAME).concat([
                { canHandle: function(item, usageContext) {
                        return usageContext === 'detail/relationships' &&
                                item && item.relationship && item.vertex &&
                                F.vertex.isEdge(item.relationship) &&
                                F.vertex.isVertex(item.vertex);
                    }, component: DetailRelationshipItem },
                { canHandle: function(item) { return F.vertex.isEdge(item); }, component: EdgeItem },
                { canHandle: function(item) { return F.vertex.isVertex(item); }, component: VertexItem }
            ])

            var rendererPromises = _.map(this.renderers, function(extension) {
                    if (extension.componentPath && !extension.component) {
                        return Promise.require(extension.componentPath).then(function(component) {
                            extension.component = component;
                        });
                    }
                    return Promise.resolve();
                });

            Promise.all(
                    [this.dataRequest('workspace', 'store')].concat(rendererPromises)
                ).done(function(promiseResults) {
                    self.workspaceVertices = promiseResults[0];
                    self.$node
                        .addClass('element-list')
                        .html(template({
                            infiniteScrolling: self.attr.infiniteScrolling &&
                                               self.attr.total !== self.attr.items.length
                        }));

                    self.attachEvents();

                    self.addItems(self.attr.items);

                    self.loadVisibleResultPreviews();
                    self.loadVisibleResultPreviews = _.debounce(self.loadVisibleResultPreviews.bind(self), 1000);

                    self.triggerInfiniteScrollRequest = _.debounce(self.triggerInfiniteScrollRequest.bind(self), 1000);
                    self.triggerInfiniteScrollRequest();

                    self.$node.droppable({ accept: '*', tolerance: 'pointer' });

                    self.onObjectsSelected(null, visalloData.selectedObjects);

                    self.on('selectAll', self.onSelectAll);
                    self.on('downUp', self.move);
                    self.on('upUp', self.move);
                    self.on('contextmenu', self.onContextMenu);
                    self.on(document, 'workspaceUpdated', self.onWorkspaceUpdated);

                    _.defer(function() {
                        self.$node.scrollTop(0);
                    })
            });
        });

        this.onWorkspaceUpdated = function(event, data) {
            var self = this;
            this.dataRequest('workspace', 'store')
                .done(function(workspaceVertices) {
                    self.workspaceVertices = workspaceVertices;

                    var addedVertices = _.indexBy(data.newVertices, 'id'),
                        removedVertices = _.indexBy(data.entityDeletes);

                    self.select('itemSelector').each(function(idx, item) {
                        var $item = $(item),
                            vertexId = $item.children('a').data('vertexId');
                        if (vertexId in addedVertices) {
                            self.stateForItem($item).then(function(itemState) {
                                $item.addClass('graph-displayed').toggleClass('map-displayed', itemState.inMap);
                            });
                        } else if (vertexId in removedVertices) {
                            $item.removeClass('graph-displayed map-displayed');
                        }
                    });
                });
        };

        this.onContextMenu = function(evt) {
            evt.preventDefault();
            evt.stopPropagation();

            var vertexId = $(evt.target).closest('.element-item').children('a').data('vertexId');
            if (vertexId) {
                this.trigger(this.$node, 'showVertexContextMenu', {
                    vertexId: vertexId,
                    position: {
                        x: event.pageX,
                        y: event.pageY
                    }
                });
            }
        };

        this.move = function(e, data) {
            var previousSelected = this.select('itemSelector').filter('.active')[e.type === 'upUp' ? 'first' : 'last'](),
                moveTo = previousSelected[e.type === 'upUp' ? 'prev' : 'next']('.element-item');

            if (moveTo.length) {
                var selectedVertexIds = data.shiftKey ? _.keys(visalloData.selectedObjects.vertexIds) : [],
                    selectedEdgeIds = data.shiftKey ? _.keys(visalloData.selectedObjects.edgeIds) : [],
                    vertexId = moveTo.children('a').data('vertexId'),
                    edgeId = moveTo.children('a').data('edgeId');

                if (vertexId) selectedVertexIds.push(vertexId);
                if (edgeId) selectedEdgeIds.push(edgeId);

                this.trigger(document, 'defocusVertices');
                this.trigger('selectObjects', { vertexIds: selectedVertexIds, edgeIds: selectedEdgeIds });
            }
        };

        this.onSelectAll = function(e) {
            e.stopPropagation();

            var items = this.select('itemSelector').addClass('active');
            this.selectItems(items);
        };

        this.after('teardown', function() {
            this.select('itemSelector').children('a').teardownAllComponents();
            this.$node.off('mouseenter mouseleave');
            this.scrollNode.off('scroll.elementList');
            this.$node.empty();
        });

        this.attachEvents = function() {
            this.scrollNode = this.$node;
            while (this.scrollNode.length && this.scrollNode.css('overflow') !== 'auto') {
                this.scrollNode = this.scrollNode.parent();
            }
            this.scrollNode.on('scroll.elementList', this.onResultsScroll.bind(this));

            this.$node.on('mouseenter mouseleave', '.element-item', this.onHoverItem.bind(this));

            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'verticesDeleted', this.onVerticesDeleted);
            this.on(document, 'edgesDeleted', this.onEdgesDeleted);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'switchWorkspace', this.onWorkspaceClear);
            this.on(document, 'workspaceDeleted', this.onWorkspaceClear);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on('addInfiniteItems', this.onAddInfiniteItems);
        };

        this.onHoverItem = function(evt) {
            if (this.disableHover === 'defocused') {
                return;
            } else if (this.disableHover) {
                this.disableHover = 'defocused';
                return this.trigger(document, 'defocusVertices');
            }

            var id = $(evt.target).closest('.element-item').children('a').data('vertexId');
            if (evt.type === 'mouseenter' && id) {
                this.trigger(document, 'focusVertices', { vertexIds: [id] });
            } else {
                this.trigger(document, 'defocusVertices');
            }
        };

        this.onResultsScroll = function(e) {
            if (!this.disableHover) {
                this.disableHover = true;
            }

            this.loadVisibleResultPreviews();

            if (this.attr.infiniteScrolling) {
                this.triggerInfiniteScrollRequest();
            }
        };

        this.triggerInfiniteScrollRequest = function() {
            if (!this.attr.infiniteScrolling) return;

            var loadingListElement = this.$node.find('.infinite-loading');

            if (this.scrollNode.length) {
                loadingListElement = loadingListElement.withinScrollable(this.scrollNode);
            }

            if (loadingListElement.length) {
                /** TODO: is the concept type attr needed here? */
                var data = { conceptType: this.attr.verticesConceptId };
                if (!this.offset) this.offset = this.attr.nextOffset;
                data.paging = {
                    offset: this.offset
                };
                this.trigger('infiniteScrollRequest', data);
            }
        };

        this.attachItemRenderer = function(el, item, relationship) {
            var self = this,
                itemRenderer = _.find(this.renderers, function(renderer) {
                    return renderer.component && renderer.canHandle(item, self.attr.usageContext);
                }).component;

            el.children('a').teardownAllComponents();
            el.empty();
            itemRenderer.attachTo($('<a class="draggable" />').appendTo(el), { item: item });

            this.stateForItem(el).then(function(itemState) {
                if (itemState.inGraph) el.addClass('graph-displayed');
                if (itemState.inMap) el.addClass('map-displayed');
            });

            return el;
        }

        this.addItems = function(items) {
            var self = this,
                loading = this.$node.find('.infinite-loading'),
                added = _.reduce(items, function(selection, item) {
                    var $li = self.attachItemRenderer($('<li class="element-item" />'), item);
                    return selection.add($li);
                }, $());

            if (loading && loading.length) {
                loading.before(added);
            } else {
                this.$node.children('ul').append(added);
            }

            this.applyDraggable(added.children('a.draggable'));
            this.loadVisibleResultPreviews();
        }

        this.onAddInfiniteItems = function(evt, data) {
            var loading = this.$node.find('.infinite-loading');

            if (!data.success) {
                loading.html(alertTemplate({
                    error: i18n('element.list.infinite_scroll.error')
                }));
                this.attr.infiniteScrolling = false;
            } else if (data.items.length === 0) {
                loading.remove();
                this.attr.infiniteScrolling = false;
            } else {
                this.addItems(data.items);

                this.offset = data.nextOffset;

                var total = data.total || this.attr.total || 0;
                if (total === this.select('itemSelector').length) {
                    loading.remove();
                    this.attr.infiniteScrolling = false;
                } else {
                    this.triggerInfiniteScrollRequest();
                }
            }
        };

        this.loadVisibleResultPreviews = function() {
            this.disableHover = false;

            var lisVisible = this.select('itemSelector');
            if (this.scrollNode.length) {
                lisVisible = lisVisible.withinScrollable(this.scrollNode);
            }

            lisVisible.children('a').trigger('loadPreview');
        };

        this.applyDraggable = function(el) {
            var self = this;

            el.draggable({
                helper: 'clone',
                appendTo: 'body',
                revert: 'invalid',
                revertDuration: 250,
                scroll: false,
                zIndex: 100,
                distance: 10,
                multi: true,
                start: function(ev, ui) {
                    $(ui.helper).addClass('vertex-dragging');
                },
                selection: function(ev, ui) {
                    self.selectItems(ui.selected);
                }
            });
        };

        this.selectItems = function(items) {
            var selection = _.reduce(items.children('a'), function(memo, item) {
                    var $item = $(item),
                        vertexId = $item.data('vertexId'),
                        edgeId = $item.data('edgeId');

                    if (vertexId) memo.vertexIds.push(vertexId);
                    if (edgeId) memo.edgeIds.push(edgeId);

                    return memo;
                }, { vertexIds: [], edgeIds: [] });

            if (selection.vertexIds.length === 0 && selection.edgeIds.length === 0) {
                return;
            }

            this.trigger(document, 'defocusVertices');
            this.trigger('selectObjects', selection);
        };

        this.onWorkspaceLoaded = function(evt, workspace) {
            this.onVerticesUpdated(evt, workspace.data || {});
        };

        // Switching workspaces should clear the icon state and vertices
        this.onWorkspaceClear = function(event, data) {
            if (event.type !== 'workspaceDeleted' || visalloData.currentWorkspaceId === data.workspaceId) {
                this.select('itemSelector').filter('.graph-displayed').removeClass('graph-displayed');
                this.select('itemSelector').filter('.map-displayed').removeClass('map-displayed');
            }
        };

        this.onVerticesUpdated = function(event, data) {
            var self = this,
                updatedVertices = _.indexBy(data.vertices || [], 'id');

            this.select('itemSelector').each(function(idx, item) {
                var $item = $(item),
                    vertexId = $item.children('a').data('vertexId');
                if (vertexId && vertexId in updatedVertices) {
                    self.attachItemRenderer($item, updatedVertices[vertexId]);
                }
            });
            this.loadVisibleResultPreviews();
        };

        this.onVerticesDeleted = function(event, data) {
            var self = this,
                deletedVertices = _.indexBy(data.vertexIds || []);
            this.select('itemSelector').each(function(idx, item) {
                var $item = $(item),
                    $a = $item.children('a'),
                    vertexId = $a.data('vertexId');
                if (vertexId && vertexId in deletedVertices) {
                    $a.teardownAllComponents();
                    $item.remove();
                }
            });
            this.loadVisibleResultPreviews();
        };

        this.onEdgesDeleted = function(event, data) {
            var self = this;
            this.select('itemSelector').each(function(idx, item) {
                var $item = $(item),
                    $a = $item.children('a'),
                    edgeId = $a.data('edgeId');
                if (edgeId && edgeId === data.edgeId) {
                    $a.teardownAllComponents();
                    $item.remove();
                }
            });
            this.loadVisibleResultPreviews();
        };

        this.onObjectsSelected = function(event, data) {
            var self = this,
                vertexIds = _.pluck(data.vertices, 'id'),
                edgeIds = _.pluck(data.edges, 'id');

            this.$node.children('ul').children('.active').removeClass('active');

            if (vertexIds.length === 0 && edgeIds.length === 0) {
                return;
            }

            this.$node.addClass('active');
            this.select('itemSelector').each(function(idx, item) {
                var $item = $(item),
                    itemVertexId = $item.children('a').data('vertexId'),
                    itemEdgeId = $item.children('a').data('edgeId');
                if ((itemVertexId && _.contains(vertexIds, itemVertexId)) ||
                    (itemEdgeId && _.contains(edgeIds, itemEdgeId))) {
                    $item.addClass('active');
                }
            });
        };
    }
});

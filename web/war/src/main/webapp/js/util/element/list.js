
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
    'util/dnd',
    'util/popovers/withElementScrollingPositionUpdates',
    'util/jquery.withinScrollable'
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
    dnd,
    withPositionUpdates) {
    'use strict';

    var MAX_ITEMS_BEFORE_FORCE_LOADING = 10;
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
            draggableSelector: '.element-item a.draggable',
            infiniteScrolling: false,
            usageContext: 'search'
        });

        this.after('initialize', function() {
            var self = this;

            // Do this to support code that arrives here via the
            // deprecated vertex/list and edge/list components
            this.attr.items = this.attr.items || this.attr.edges || this.attr.vertices;

            this.renderers = registry.extensionsForPoint(EXTENSION_POINT_NAME).concat([
                { canHandle: function(item, usageContext) {
                        return usageContext === 'detail/relationships' &&
                                item && item.relationship &&
                                F.vertex.isEdge(item.relationship);
                    }, component: DetailRelationshipItem },
                { canHandle: function(item) { return F.vertex.isEdge(item); }, component: EdgeItem },
                { canHandle: function(item) { return F.vertex.isVertex(item); }, component: VertexItem }
            ])

            this.on('click', {
                draggableSelector: this.onClick
            });

            var rendererPromises = _.map(this.renderers, function(extension) {
                    if (extension.componentPath && !extension.component) {
                        return Promise.require(extension.componentPath).then(function(component) {
                            extension.component = component;
                        });
                    }
                    return Promise.resolve();
                });

            Promise.all(rendererPromises).done(function(promiseResults) {
                    self.localScrolling = !self.attr.infiniteScrolling && self.attr.items.length > MAX_ITEMS_BEFORE_FORCE_LOADING;
                    self.$node
                        .addClass('element-list')
                        .html(template({
                            localScrolling: self.localScrolling,
                            infiniteScrolling: self.attr.infiniteScrolling &&
                                               self.attr.total !== self.attr.items.length
                        }));

                    self.attachEvents();

                    if (self.localScrolling) {
                        self.offset = 0;
                        self.allItems = self.attr.items;
                        self.addItems(self.allItems.slice(0, MAX_ITEMS_BEFORE_FORCE_LOADING));
                    } else {
                        self.addItems(self.attr.items);
                    }

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
                    self.trigger('renderFinished');

                    _.defer(function() {
                        self.$node.scrollTop(0);
                    })
                    self.trigger('listRendered');
            });
        });

        this.onClick = function(event) {
            event.preventDefault();
            event.stopPropagation();

            const {vertexIds, edgeIds} = visalloData.selectedObjects;
            const $target = $(event.target).parents('li');
            const pushData = (data) => {
                if (data.vertexId) selectVertexIds.push(data.vertexId)
                if (data.edgeId) selectEdgeIds.push(data.edgeId)
            };

            var data = $(event.target).closest('a.draggable').data();
            var [selectVertexIds, selectEdgeIds] = [[], []];

            if (!this.attr.singleSelection) {
                const targetIndex = $target.index();

                if (event.shiftKey) {
                    const index = this.lastClickedIndex || 0;
                    const min = Math.min(index, targetIndex);
                    const max = Math.max(index, targetIndex);
                    const $items = $target.parent().children();
                    for (let i = min; i <= max; i++) {
                        pushData($items.eq(i).find('a.draggable').data());
                    }
                } else if (event.metaKey || event.ctrlKey) {
                    selectVertexIds = Object.keys(vertexIds);
                    selectEdgeIds = Object.keys(edgeIds);
                } else {
                    if (data.vertexId && data.vertexId in vertexIds &&
                        Object.keys(vertexIds).length === 1) {
                        data = {};
                    }
                    if (data.edgeId && data.edgeId in edgeIds &&
                        Object.keys(edgeIds).length === 1) {
                        data = {};
                    }
                }

                if (!event.shiftKey) {
                    this.lastClickedIndex = targetIndex;
                }
            }

            pushData(data);

            this.trigger('selectObjects', {
                vertexIds: selectVertexIds,
                edgeIds: selectEdgeIds
            })
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

                this.trigger(document, 'defocusElements');
                this.trigger('selectObjects', { vertexIds: selectedVertexIds, edgeIds: selectedEdgeIds });
            }
        };

        this.onSelectAll = function(e) {
            e.stopPropagation();

            var items = this.select('itemSelector').addClass('active');
            this.selectItems(items);
        };

        this.after('teardown', function() {
            this.trigger(document, 'defocusElements');
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
            this.on('objectsSelected', this.onObjectsSelected);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on('addInfiniteItems', this.onAddInfiniteItems);
        };

        this.onHoverItem = function(evt) {
            if (this.disableHover === 'defocused') {
                return;
            } else if (this.disableHover) {
                this.disableHover = 'defocused';
                return this.trigger(document, 'defocusElements');
            }

            var $anchor = $(evt.target).closest('.element-item').children('a'),
                vertexId = $anchor.data('vertexId'),
                edgeId = $anchor.data('edgeId');

            if (evt.type === 'mouseenter' && vertexId) {
                this.trigger(document, 'focusElements', { vertexIds: [vertexId] });
            } else if (evt.type === 'mouseenter' && edgeId) {
                this.trigger(document, 'focusElements', { edgeIds: [edgeId] });
            } else {
                this.trigger(document, 'defocusElements');
            }
        };

        this.onResultsScroll = function(e) {
            if (!this.disableHover) {
                this.disableHover = true;
            }

            this.loadVisibleResultPreviews();

            if (this.localScrolling || this.attr.infiniteScrolling) {
                this.triggerInfiniteScrollRequest();
            }
        };

        this.triggerInfiniteScrollRequest = function() {
            if (!this.attr.infiniteScrolling && !this.localScrolling) return;

            var loadingListElement = this.$node.find('.infinite-loading');

            if (this.scrollNode.length) {
                loadingListElement = loadingListElement.withinScrollable(this.scrollNode);
            }

            if (loadingListElement.length) {

                if (this.localScrolling) {
                    this.offset += MAX_ITEMS_BEFORE_FORCE_LOADING;
                    this.addItems(this.attr.items.slice(this.offset, this.offset + MAX_ITEMS_BEFORE_FORCE_LOADING));
                } else {
                    /** TODO: is the concept type attr needed here? */
                    var data = { conceptType: this.attr.verticesConceptId };
                    if (!this.offset) this.offset = this.attr.nextOffset;
                    data.paging = {
                        offset: this.offset
                    };
                    this.trigger('infiniteScrollRequest', data);
                }
            }
        };

        this.attachItemRenderer = function(el, item, relationship) {
            var self = this,
                usageContext = self.attr.usageContext,
                itemRenderer = _.find(this.renderers, function(renderer) {
                    return renderer.component && renderer.canHandle(item, usageContext);
                }).component;

            el.children('a').teardownAllComponents();
            el.empty();
            itemRenderer.attachTo($('<a class="draggable"/>').appendTo(el), { item: item, usageContext: usageContext });

            this.applyDraggable(el[0]);

            return el;
        };

        this.addItems = function(items) {
            if (this.localScrolling && (this.offset + MAX_ITEMS_BEFORE_FORCE_LOADING) >= this.allItems.length) {
                this.$node.find('.infinite-loading').remove();
            }
            if (items.length && 'vertex' in items[0]) {
                this._items = {
                    ...(this._items || {}),
                    ...(_.indexBy(_.pluck(items, 'vertex'), 'id')),
                    ...(_.indexBy(_.pluck(items, 'relationship'), 'id'))
                };
            } else {
                this._items = { ...(this._items || {}), ...(_.indexBy(items, 'id')) };
            }

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

            this.loadVisibleResultPreviews();
        };

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

            el.setAttribute('draggable', true)
            el.addEventListener('dragstart', function(e) {
                const $target = $(e.target);
                const elements = [];

                $target.siblings('.active').andSelf().each(function() {
                    var data = $(this).find('a.draggable').data();
                    if (data.vertexId) {
                        elements.push(self._items[data.vertexId])
                    }
                    if (data.edgeId) {
                        elements.push(self._items[data.edgeId])
                    }
                })
                const dt = e.dataTransfer;
                dt.effectAllowed = 'all';
                dnd.setDataTransferWithElements(dt, { elements })
            }, false)
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

            this.trigger(document, 'defocusElements');
            this.trigger('selectObjects', selection);
        };

        this.onWorkspaceLoaded = function(evt, workspace) {
            this.onVerticesUpdated(evt, workspace.data || {});
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
                edgeIds = _.pluck(data.edges, 'id'),
                total = vertexIds.length + edgeIds.length;

            this.$node.children('ul').children('.active').removeClass('active');

            if (vertexIds.length === 0 && edgeIds.length === 0) {
                return;
            }
            if (this.attr.showSelected === false && total > 1) return;

            this.$node.addClass('active');
            this.select('itemSelector').each(function(idx, item) {
                var $item = $(item),
                    itemVertexId = $item.children('a').data('vertexId'),
                    itemEdgeId = $item.children('a').data('edgeId');
                if ((itemVertexId && _.contains(vertexIds, itemVertexId)) ||
                    (itemEdgeId && _.contains(edgeIds, itemEdgeId))) {
                    $item.addClass('active');
                    self.trigger($item.children('a'), 'itemActivated');
                } else {
                    self.trigger($item.children('a'), 'itemDeactivated');
                }
            });
        };
    }
});

define([
    'react',
    'react-redux',
    'react-dom',
    'data/web-worker/store/selection/actions',
    'data/web-worker/store/product/actions',
    'data/web-worker/store/product/selectors',
    'components/DroppableHOC',
    'configuration/plugins/registry',
    'util/retina',
    'util/dnd',
    './worker/actions',
    './Graph'
], function(
    React,
    redux,
    ReactDom,
    selectionActions,
    productActions,
    productSelectors,
    DroppableHOC,
    registry,
    retina,
    dnd,
    graphActions,
    Graph) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.graph.options',
        'Add components to graph options dropdown',
        function(e) {
            return ('identifier' in e) && ('optionComponentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphOptions'
    );
    registry.documentExtensionPoint('org.visallo.graph.style',
        'Apply additional cytoscape styles',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphStyle'
    );
    registry.documentExtensionPoint('org.visallo.graph.node.decoration',
        'Description',
        function(e) {
            if (e.applyTo && !_.isFunction(e.applyTo)) return false;
            if (!_.isObject(e.alignment)) return false;
            if (!_.contains(['left', 'center', 'right'], e.alignment.h)) return false;
            if (!_.contains(['top', 'center', 'bottom'], e.alignment.v)) return false;
            return true;
        },
        'http://docs.visallo.org/extension-points/front-end/graphDecorations'
    );
    registry.documentExtensionPoint('org.visallo.graph.export',
        'Add menu options to export graph / workspace',
        function(e) {
            return ('menuItem' in e) && ('componentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphExport'
    );
    registry.documentExtensionPoint('org.visallo.graph.selection',
        'Add custom graph selection menu items',
        function(e) {
            return ('identifier' in e) &&
                _.contains(
                    ['selected', 'none-selected', 'always'],
                    e.visibility
                );
        },
        'http://docs.visallo.org/extension-points/front-end/graphSelector'
    );
    registry.documentExtensionPoint('org.visallo.graph.layout',
        'Add new cytoscape layouts to graph menu',
        function(e) {
            return ('identifier' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphLayout'
    );
    registry.documentExtensionPoint('org.visallo.graph.view',
        'Add components to graph container',
        function(e) {
            return ('componentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphView'
    );
    registry.documentExtensionPoint('org.visallo.graph.node.class',
        'Function that can change cytoscape classes of nodes',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphNode/class.html'
    );
    registry.documentExtensionPoint('org.visallo.graph.edge.class',
        'Function that can change cytoscape classes of edges',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphEdge/class.html'
    );
    registry.documentExtensionPoint('org.visallo.graph.node.transformer',
        'Function that can change cytoscape node structure',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphNode/transformer.html'
    );
    registry.documentExtensionPoint('org.visallo.graph.edge.transformer',
        'Function that can change cytoscape edge structure',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphEdge/transformer.html'
    );
    registry.registerExtension('org.visallo.graph.options', {
        identifier: 'toggleEdgeLabel',
        optionComponentPath: 'org/visallo/web/product/graph/dist/EdgeLabel'
    });
    registry.registerExtension('org.visallo.graph.options', {
        identifier: 'toggleSnapToGrid',
        optionComponentPath: 'org/visallo/web/product/graph/dist/SnapToGrid'
    });
    registry.registerExtension('org.visallo.vertex.menu', {
        label: i18n('vertex.contextmenu.add_related'),
        event: 'addRelatedItems',
        shortcut: 'alt+r',
        shouldDisable: function(selection, vertexId, target) {
            var graph = document.querySelector('.org-visallo-graph');
            if (graph) {
                return !graph.contains(target)
            }
            return true;
        },
        options: {
            insertIntoMenuItems: function(item, items) {
                const index = _.findIndex(items, { label: i18n('vertex.contextmenu.search') });
                if (index >= 0) {
                    items.splice(index + 1, 0, item);
                } else {
                    items.push(item);
                }
            }
        }
    });
    registry.registerExtension('org.visallo.vertex.menu', {
        cls: 'requires-EDIT',
        label: i18n('vertex.contextmenu.connect'),
        shortcut: 'CTRL+drag',
        event: 'startVertexConnection',
        selection: 1,
        args: {
            connectionType: 'CreateConnection'
        },
        shouldDisable: function(selection, vertexId, target) {
            return $(target).closest('.org-visallo-graph').length === 0;
        },
        options: {
            insertIntoMenuItems: function(item, items) {
                items.splice(0, 0, item);
            }
        }
    })
    registry.registerExtension('org.visallo.vertex.menu', {
        label: i18n('vertex.contextmenu.find_path'),
        shortcut: 'CTRL+drag',
        event: 'startVertexConnection',
        selection: 1,
        args: {
            connectionType: 'FindPath'
        },
        shouldDisable: function(selection, vertexId, target) {
            return $(target).closest('.org-visallo-graph').length === 0;
        },
        options: {
            insertIntoMenuItems: function(item, items) {
                items.splice(1, 0, item, 'DIVIDER');
            }
        }
    });
    registry.registerExtension('org.visallo.vertex.menu', {
        label: i18n('vertex.contextmenu.open.preview'),
        subtitle: i18n('vertex.contextmenu.open.preview.subtitle'),
        event: 'previewVertex',
        shortcut: 'alt+p',
        shouldDisable: function(selection, vertexId, target) {
            return $(target).closest('.org-visallo-graph').length === 0;
        },
        options: {
            insertIntoMenuItems: function(item, items) {
                var openItem = _.findWhere(items, { label: i18n('vertex.contextmenu.open') });
                if (openItem) {
                    openItem.submenu.splice(0, 0, item);
                }
            }
        }
    });

    $(() => {
        $(document).trigger('registerKeyboardShortcuts', {
            scope: i18n('graph.help.scope'),
            shortcuts: {
                '-': { fire: 'zoomOut', desc: i18n('graph.help.zoom_out') },
                '=': { fire: 'zoomIn', desc: i18n('graph.help.zoom_in') },
                'alt-f': { fire: 'fit', desc: i18n('graph.help.fit') },
                'alt-n': { fire: 'createVertex', desc: i18n('graph.help.create_vertex') },
                'alt-p': { fire: 'previewVertex', desc: i18n('graph.help.preview_vertex') }
            }
        });
    });

    const mimeTypes = [VISALLO_MIMETYPES.ELEMENTS];
    const style = { height: '100%' };

    const GraphContainer = redux.connect(

        (state, props) => {
            var pixelRatio = state.screen.pixelRatio,
                ontology = state.ontology,
                panelPadding = state.panel.padding,
                ghosts = state['org-visallo-graph'].animatingGhosts,
                uiPreferences = state.user.current.uiPreferences,
                focusing = productSelectors.getFocusedElementsInProduct(state);

            return {
                ...props,
                selection: productSelectors.getSelectedElementsInProduct(state),
                viewport: productSelectors.getViewport(state),
                focusing,
                ghosts,
                pixelRatio,
                uiPreferences,
                ontology,
                panelPadding,
                productElementIds: productSelectors.getElementIdsInProduct(state),
                elements: productSelectors.getElementsInProduct(state),
                workspace: state.workspace.byId[state.workspace.currentId],
                mimeTypes,
                style
            }
        },

        function(dispatch, props) {
            return {
                onAddSelection: (selection) => dispatch(selectionActions.add(selection)),
                onRemoveSelection: (selection) => dispatch(selectionActions.remove(selection)),
                onSetSelection: (selection) => dispatch(selectionActions.set(selection)),
                onClearSelection: () => dispatch(selectionActions.clear()),

                onUpdatePreview: (id, dataUrl) => dispatch(productActions.updatePreview(id, dataUrl)),

                onAddRelated: (id, vertices) => dispatch(graphActions.addRelated(id, vertices)),
                onUpdatePositions: (id, positions) => dispatch(graphActions.updatePositions(id, positions)),
                onSaveViewport: (id, { pan, zoom }) => dispatch(productActions.updateViewport(id, { pan, zoom })),
                onSearch(event) {
                    event.preventDefault();
                    if (!$('.search-pane.visible').length) {
                        $(document).trigger('menubarToggleDisplay', { name: 'search' })
                    }
                },
                onGhostFinished(id) {
                    dispatch(graphActions.removeGhost(id))
                },
                onDrop: (event, position) => {
                    const { dataTransfer } = event;
                    const elements = dnd.getElementsFromDataTransfer(dataTransfer);
                    if (elements) {
                        event.preventDefault();
                        event.stopPropagation();
                        dispatch(graphActions.dropElements(props.product.id, elements, position))
                    }
                },
                onDropElementIds: (elementIds, position) => {
                    dispatch(graphActions.dropElements(props.product.id, elementIds, position))
                },
                onRemoveElementIds: (elementIds) => {
                    dispatch(productActions.removeElements(props.product.id, elementIds))
                },
                onVertexMenu: (element, vertexId, position) => {
                    $(element).trigger('showVertexContextMenu', { vertexId, position });
                },
                onEdgeMenu: (element, edgeIds, position) => {
                    $(element).trigger('showEdgeContextMenu', { edgeIds, position });
                }
            }
        },
        null,
        { withRef: true }
    )(DroppableHOC(Graph, '.org-visallo-graph'));

    return GraphContainer;
});

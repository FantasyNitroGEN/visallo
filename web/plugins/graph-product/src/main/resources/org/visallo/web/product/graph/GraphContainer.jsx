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

    /**
     * Plugin to add custom options components (Flight or React) which display in the graph options menu (next to Fit)
     * when its opened.
     *
     * @param {string} identifier Unique id for this option item
     * @param {string} optionComponentPath Path to {@link org.visallo.graph.options~Component} to render
     */
    registry.documentExtensionPoint('org.visallo.graph.options',
        'Add components to graph options dropdown',
        function(e) {
            return ('identifier' in e) && ('optionComponentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphOptions'
    );

    /**
     * Add to the cytoscape stylesheet. [Cytoscape docs](http://js.cytoscape.org/#function-format)
     *
     * This is used to adjust the styling of all graph elements: Nodes, Edges,
     * Decorations, etc.
     *
     * The default stylesheet is defined in [styles.js](https://github.com/visallo/visallo/blob/master/web/plugins/graph-product/src/main/resources/org/visallo/web/product/graph/styles.js#L46)
     *
     * @param {org.visallo.graph.style~StyleFn} config Cytoscape style function
     * @example
     * registry.registerExtension('org.visallo.graph.style', function(cytoscapeStylesheet) {
     *     // Changes selected nodes color to red
     *     cytoscapeStylesheet.selector('node:selected')
     *         .style({
     *             color: '#FF0000'
     *         })
     * });
     */
    registry.documentExtensionPoint('org.visallo.graph.style',
        'Apply additional cytoscape styles',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphStyle'
    );

    /**
     * Graph decorations are additional detail to display around a vertex
     * when displayed in a graph. These decorations are implemented as
     * [cytoscape.js](http://js.cytoscape.org) nodes inside of compound nodes.
     * That allows them to be styled just like vertices using {@link org.visallo.graph.style} extensions.
     *
     * @param {object} alignment Where the decoration is attached
     * @param {string} alignment.h Where the decoration is attached on the
     * horizontal axis: `left`, `center`, `right`
     * @param {string} alignment.v Where the decoration is attached on the vertical axis: `top`, `center`, `bottom`
     * @param {object|org.visallo.graph.node.decoration~data} data The cytoscape data object for the decoration node
     * @param {string|org.visallo.graph.node.decoration~classes} [classes]
     * Classes to add to the cytoscape node, useful for styling with {@link org.visallo.graph.style}
     *
     * Include multiple `classes` using space-separated string
     * @param {org.visallo.graph.node.decoration~applyTo} [applyTo] Whether the
     * decoration should be added to certain nodes.
     * @param {object} [padding] Offset the decoration from the bounds of the
     * node.
     *
     * Useful when the decoration is styled with known width/height that won't
     * work with defaults `8x8`.
     * @param {number} [padding.x=8] X offset
     * @param {number} [padding.y=8] Y offset
     * @param {org.visallo.graph.node.decoration~onClick} [onClick] This function is called on click events
     * @param {org.visallo.graph.node.decoration~onMouseOver} [onMouseOver] This function is called on mouseover events
     * @param {org.visallo.graph.node.decoration~onMouseOut} [onMouseOut] This function is called on mouseout events
     * @example
     * registry.registerExtension('org.visallo.graph.node.decoration', {
     *     applyTo: function(v) { return true; },
     *     alignment: { h: 'left', v: 'top' },
     *     classes: 'custom',
     *     data: function(vertex) {
     *         return {
     *             label: vertex.properties.length
     *         }
     *     }
     * });
     */
    registry.documentExtensionPoint('org.visallo.graph.node.decoration',
        'Add decoration text/images around the node',
        function(e) {
            if (e.applyTo && !_.isFunction(e.applyTo)) return false;
            if (!_.isObject(e.alignment)) return false;
            if (!_.contains(['left', 'center', 'right'], e.alignment.h)) return false;
            if (!_.contains(['top', 'center', 'bottom'], e.alignment.v)) return false;
            return true;
        },
        'http://docs.visallo.org/extension-points/front-end/graphDecorations'
    );

    /**
     * Allows a custom component to render to configure how to export.
     *
     * @param {string} menuItem The string to display in the context menu
     * @param {string} componentPath The path to {@link org.visallo.graph.export~Exporter|exporter component}
     * to render when user selects the menu option.
     * @param {boolean} [showPopoverTitle=true] If the popover should display a title
     * @param {boolean} [showPopoverCancel=true] If the popover displays cancel button
     * @param {function} [attributes] Function that can transform the properties that the component receives.
     */
    registry.documentExtensionPoint('org.visallo.graph.export',
        'Add menu options to export graph / workspace',
        function(e) {
            return ('menuItem' in e) && ('componentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphExport'
    );

    /**
     * Add custom cytoscape selection menu items. Graph provides select all, none, and invert by default.
     *
     * The text displayed to the user uses the message bundle key:
     *
     *      graph.selector.[identifier].displayName=Selection Text to Display
     *
     * @param {function} config Function that does the custom selection
     * @param {string} config.identifier Unique id for selection
     * @param {string} [config.visibility] When should the item be available based on
     * the current selection
     *
     * * `selected` When there is currently something selected
     * * `none-selected` When nothing is selected
     * * `always` Always show this option regardless of selection state
     * @example
     * var doRandomSelection = function(cy) {
     *     var nodes = cy.nodes().unselect(),
     *         randomIndex = Math.floor(Math.random() * nodes.length);
     *     nodes[randomIndex].select();
     * }
     * doRandomSelection.identifier = 'myRandomSelector';
     * // optionally: doRandomSelection.visibility = 'always';
     * registry.registerExtension('org.visallo.graph.selection', doRandomSelection);
     */
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

    /**
     * Extension to add new graph [layouts](http://js.cytoscape.org/#layouts) that are accesible from the layout
     * context menu. 
     *
     * the `identifier` is used for the menu option text and should be in the
     * plugins message bundle:
     *
     *      graph.layout.myLayout.displayName=My Layout
     *
     * @param {function} config A cytoscape layout object constructor.
     * @param {string} config.identifier The layout identifier
     * @param {function} config.run Instance method to run the layout.
     * @example
     * MyLayout.identifier = 'myLayout';
     * function MyLayout(options) {
     *     this.options = options;
     * }
     *
     * MyLayout.prototype.run = function() {
     *     var cy = this.options.cy;
     *
     *     // Layout nodes
     *     // Note: Use util/retina to convert from points to pixels (Hi-DPI displays)
     *     cy.nodes()[0].renderedPosition({x:100,y:100})
     *
     *     // Must call ready and stop callbacks
     *     cy.one("layoutready", options.ready);
     *     cy.trigger("layoutready");
     *
     *     cy.one("layoutstop", options.stop);
     *     cy.trigger("layoutstop");
     *
     *     return this;
     * };
     * registry.registerExtension('org.visallo.graph.layout', MyLayout);
     */
    registry.documentExtensionPoint('org.visallo.graph.layout',
        'Add new cytoscape layouts to graph menu',
        function(e) {
            return ('identifier' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphLayout'
    );

    /**
     * Plugin to add custom view components which overlay the graph. Used for toolbars, etc., that interact with the graph.
     *
     * Views can be Flight or React components and should be styled to be
     * absolutely positioned. The absolute position given is relative to the
     * graph. `0,0` is top-left corner of graph     *
     *
     * @param {string} componentPath Path to component to render
     * component
     */
    registry.documentExtensionPoint('org.visallo.graph.view',
        'Add components to graph container',
        function(e) {
            return ('componentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphView'
    );

    /**
     * Register a function that can add or remove classes from cytoscape nodes for custom styling.
     *
     * @param {org.visallo.graph.node.class~classFn} config
     */
    registry.documentExtensionPoint('org.visallo.graph.node.class',
        'Function that can change cytoscape classes of nodes',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphNode/class.html'
    );

    /**
     * Register a function that can add or remove classes from cytoscape edges for custom styling.
     *
     * @param {org.visallo.graph.edge.class~classFn} config
     */
    registry.documentExtensionPoint('org.visallo.graph.edge.class',
        'Function that can change cytoscape classes of edges',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphEdge/class.html'
    );

    /**
     * Allows extensions to adjust the `data` attribute of cytoscape nodes.
     * @param {org.visallo.graph.node.transformer~transformerFn} config
     */
    registry.documentExtensionPoint('org.visallo.graph.node.transformer',
        'Function that can change cytoscape node structure',
        function(e) {
            return _.isFunction(e);
        },
        'http://docs.visallo.org/extension-points/front-end/graphNode/transformer.html'
    );

    /**
     * Allows extensions to adjust the `data` attribute of cytoscape edges.
     * @param {org.visallo.graph.edge.transformer~transformerFn} config
     */
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
                onUpdatePositions: (id, positions) => dispatch(graphActions.updatePositions(id, positions, { undoable: true })),
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
                    dispatch(productActions.removeElements(props.product.id, elementIds, { undoable: true }));
                },
                onVertexMenu: (element, vertexId, position) => {
                    $(element).trigger('showVertexContextMenu', { vertexId, position });
                }
            }
        },
        null,
        { withRef: true }
    )(DroppableHOC(Graph, '.org-visallo-graph'));

    return GraphContainer;
});

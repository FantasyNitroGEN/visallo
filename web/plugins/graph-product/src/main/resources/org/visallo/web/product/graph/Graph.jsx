define([
    'react',
    './Cytoscape',
    './popoverHelper',
    './styles',
    './GraphEmpty',
    './GraphExtensionViews',
    './popovers/index',
    './collapsedNodeImageHelpers',
    'util/vertex/formatters',
    'util/retina',
    'components/RegistryInjectorHOC'
], function(
    React,
    Cytoscape,
    PopoverHelper,
    styles,
    GraphEmpty,
    GraphExtensionViews,
    Popovers,
    CollapsedNodeImageHelpers,
    F,
    retina,
    RegistryInjectorHOC) {
    'use strict';

    const MaxPathsToFocus = 100;
    const MaxPreviewPopovers = 5;

    const PropTypes = React.PropTypes;
    const noop = function() {};
    const generateCompoundEdgeId = edge => edge.outVertexId + edge.inVertexId + edge.label;
    const isGhost = cyElement => cyElement && cyElement._private && cyElement._private.data && cyElement._private.data.animateTo;
    const isValidElement = cyElement => cyElement && cyElement.is('.c,.v,.e,.partial') && !isGhost(cyElement);
    const isValidNode = cyElement => cyElement && cyElement.is('node.c,node.v,node.partial') && !isGhost(cyElement);
    const edgeDisplay = (label, ontology, edges) => {
        const display = label in ontology.relationships ? ontology.relationships[label].displayName : '';
        const showNum = edges.length > 1;
        const num = showNum ? ` (${F.number.pretty(edges.length)})` : '';
        return display + num;
    };
    const propTypesElementArrays = { vertices: PropTypes.array, edges: PropTypes.array };

    let memoizeForStorage = {};
    const memoizeClear = () => { memoizeForStorage = {}; }
    const memoizeFor = function(key, elements, fn, idFn) {
        if (!key) throw new Error('Cache key must be specified');
        if (!elements) throw new Error('Valid elements should be provided');
        if (!_.isFunction(fn)) throw new Error('Cache creation method should be provided');
        const fullKey = `${key}-${idFn ? idFn() : elements.id}`;
        const cache = memoizeForStorage[fullKey];
        const vertexChanged = cache && (_.isArray(cache.elements) ?
            (
                cache.elements.length !== elements.length ||
                _.any(cache.elements, (ce, i) => ce !== elements[i])
            ) : cache.elements !== elements
        ); 
        if (cache && !vertexChanged) {
            return cache.value
        }

        memoizeForStorage[fullKey] = { elements, value: fn() };
        return memoizeForStorage[fullKey].value
    }

    const Graph = React.createClass({

        propTypes: {
            workspace: PropTypes.shape({
                editable: PropTypes.bool
            }).isRequired,
            product: PropTypes.shape({
                previewMD5: PropTypes.string,
                extendedData: PropTypes.shape(propTypesElementArrays).isRequired
            }).isRequired,
            uiPreferences: PropTypes.shape({
                edgeLabels: PropTypes.bool
            }).isRequired,
            productElementIds: PropTypes.shape(propTypesElementArrays).isRequired,
            elements: PropTypes.shape({
                vertices: PropTypes.object,
                edges: PropTypes.object
            }).isRequired,
            selection: PropTypes.shape(propTypesElementArrays).isRequired,
            focusing: PropTypes.shape(propTypesElementArrays).isRequired,
            registry: PropTypes.object.isRequired,
            onUpdatePreview: PropTypes.func.isRequired,
            onVertexMenu: PropTypes.func,
            onEdgeMenu: PropTypes.func
        },

        getDefaultProps() {
            return {
                onVertexMenu: noop,
                onEdgeMenu: noop
            }
        },

        getInitialState() {
            return {
                viewport: this.props.viewport || {},
                animatingGhosts: {},
                initialProductDisplay: true,
                draw: null,
                paths: null,
                hovering: null,
                collapsedImageDataUris: {}
            }
        },

        saveViewport(props) {
            var productId = this.props.product.id;
            if (this.currentViewport && productId in this.currentViewport) {
                var viewport = this.currentViewport[productId];
                props.onSaveViewport(productId, viewport);
            }
        },

        componentDidMount() {
            memoizeClear();
            this.cyNodeIdsWithPositionChanges = {};

            this.popoverHelper = new PopoverHelper(this.node, this.cy);
            this.legacyListeners({
                addRelatedDoAdd: (event, data) => {
                    this.props.onAddRelated(this.props.product.id, data.addVertices)
                },
                selectAll: (event, data) => {
                    this.cytoscape.state.cy.elements().select();
                },
                selectConnected: (event, data) => {
                    event.stopPropagation();
                    const cy = this.cytoscape.state.cy;
                    const selected = cy.elements().filter(':selected');
                    selected.neighborhood('node').select();
                    selected.connectedNodes().select();

                    selected.unselect();
                },
                startVertexConnection: (event, { vertexId, connectionType }) => {
                    this.setState({
                        draw: {
                            vertexId,
                            connectionType
                        }
                    });
                },
                uncollapse: (event, { collapsedNodeIds }) => {
                    this.props.onUncollapseNodes(this.props.product.id, collapsedNodeIds);
                },
                menubarToggleDisplay: { node: document, handler: (event, data) => {
                    if (data.name === 'products-full') {
                        this.teardownPreviews();
                    }
                }},
                finishedVertexConnection: this.cancelDraw,
                'zoomOut zoomIn fit': this.onKeyboard,
                createVertex: event => this.createVertex(),
                fileImportSuccess: this.onFileImportSuccess,
                previewVertex: this.previewVertex,
                closePreviewVertex: (event, { vertexId }) => {
                    delete this.detailPopoversMap[vertexId];
                },
                elementsCut: { node: document, handler: (event, { vertexIds }) => {
                    this.props.onRemoveElementIds({ vertexIds, edgeIds: [] });
                }},
                elementsPasted: { node: document, handler: (event, elementIds) => {
                    this.props.onDropElementIds(elementIds)
                }},
                focusPaths: { node: document, handler: this.onFocusPaths },
                defocusPaths: { node: document, handler: this.onDefocusPaths },
                focusPathsAddVertexIds: { node: document, handler: this.onFocusPathsAdd },
                reapplyGraphStylesheet: { node: document, handler: this.reapplyGraphStylesheet }
            });
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.selection !== this.props.selection) {
                this.resetQueuedSelection(nextProps.selection);
            }
            if (nextProps.registry !== this.props.registry) {
                memoizeClear();
            }
            if (nextProps.product.id === this.props.product.id) {
                this.setState({ viewport: {}, initialProductDisplay: false })
            } else {
                this.teardownPreviews();
                this.saveViewport(nextProps)
                this.setState({ viewport: nextProps.viewport || {}, initialProductDisplay: true })
            }
        },

        componentWillUnmount() {
            this.removeEvents.forEach(({ node, func, events }) => {
                $(node).off(events, func);
            })

            this.teardownPreviews();
            this.popoverHelper.destroy();
            this.popoverHelper = null;
            this.saveViewport(this.props)
        },

        teardownPreviews() {
            if (this.detailPopoversMap) {
                _.each(this.detailPopoversMap, e => $(e).teardownAllComponents())
                this.detailPopoversMap = {};
            }
        },

        render() {
            var { viewport, initialProductDisplay, draw, paths } = this.state,
                { panelPadding, registry, workspace, product } = this.props,
                { editable } = workspace,
                { previewMD5 } = product,
                config = {...CONFIGURATION(this.props), ...viewport},
                events = {
                    onSelect: this.onSelect,
                    onRemove: this.onRemove,
                    onUnselect: this.onUnselect,
                    onFree: this.onFree,
                    onLayoutStop: this.onLayoutStop,
                    onPosition: this.onPosition,
                    onReady: this.onReady,
                    onDecorationEvent: this.onDecorationEvent,
                    onMouseOver: this.onMouseOver,
                    onMouseOut: this.onMouseOut,
                    onTap: this.onTap,
                    onTapHold: this.onTapHold,
                    onTapStart: this.onTapStart,
                    onCxtTapStart: this.onTapStart,
                    onCxtTapEnd: this.onCxtTapEnd,
                    onContextTap: this.onContextTap,
                    onPan: this.onViewport,
                    onZoom: this.onViewport
                },
                menuHandlers = {
                    onMenuCreateVertex: this.onMenuCreateVertex,
                    onMenuSelect: this.onMenuSelect,
                    onMenuExport: this.onMenuExport,
                    onCollapseSelectedNodes: this.onCollapseSelectedNodes
                },
                cyElements = this.mapPropsToElements(editable),
                extensionViews = registry['org.visallo.graph.view'];

            return (
                <div ref={r => {this.node = r}} className="org-visallo-graph" style={{ height: '100%' }}>
                    <Cytoscape
                        ref={r => { this.cytoscape = r}}
                        {...events}
                        {...menuHandlers}
                        tools={this.getTools()}
                        initialProductDisplay={initialProductDisplay}
                        hasPreview={Boolean(previewMD5)}
                        config={config}
                        panelPadding={panelPadding}
                        elements={cyElements}
                        drawEdgeToMouseFrom={draw ? _.pick(draw, 'vertexId', 'toVertexId') : null }
                        drawPaths={paths ? _.pick(paths, 'paths', 'sourceId', 'targetId') : null }
                        onGhostFinished={this.props.onGhostFinished}
                        onUpdatePreview={this.onUpdatePreview}
                        editable={editable}
                    ></Cytoscape>

                    {cyElements.nodes.length === 0 ? (
                        <GraphEmpty editable={editable} panelPadding={panelPadding} onSearch={this.props.onSearch} onCreate={this.onCreate} />
                    ) : null}

                    { extensionViews.length ? (
                        <GraphExtensionViews views={extensionViews} panelPadding={panelPadding} />
                    ) : null }
                </div>
            )
        },

        onFocusPaths(event, data) {
            if (data.paths.length > MaxPathsToFocus) {
                data.paths = data.paths.slice(0, MaxPathsToFocus);
                $(document).trigger('displayInformation', { message: 'Too many paths to show, will display the first ' + MaxPathsToFocus })
            }
            this.setState({
                paths: data
            })
        },

        onFocusPathsAdd(event) {
            const { paths } = this.state;
            if (paths) {
                const limitedPaths = paths.paths.slice(0, MaxPathsToFocus);
                const vertexIds = _.chain(limitedPaths).flatten().uniq().value();
                this.props.onDropElementIds({ vertexIds });
            }
        },

        onDefocusPaths(event, data) {
            if (this.state.paths) {
                this.setState({ paths: null });
            }
        },

        onCreate() {
            this.createVertex();
        },

        reapplyGraphStylesheet() {
            this.forceUpdate();
        },

        getTools() {
            /**
             * @typedef org.visallo.graph.options~Component
             * @property {object} cy The cytoscape instance
             * @property {object} product The graph product
             */
            return this.props.registry['org.visallo.graph.options'].map(e => ({
                identifier: e.identifier,
                componentPath: e.optionComponentPath,
                product: this.props.product
            }));
        },

        onReady({ cy }) {
            this.cy = cy;
        },

        onDecorationEvent(event) {
            const { cy, cyTarget } = event;
            const decoration = decorationForId(cyTarget.id());
            if (decoration) {
                const handlerName = {
                    /**
                     * @callback org.visallo.graph.node.decoration~onClick
                     * @this The decoration cytoscape node
                     * @param {object} event The {@link http://js.cytoscape.org/#events/event-object|Cytoscape event} object
                     * @param {object} data
                     * @param {object} data.vertex The vertex this decoration
                     * is attached
                     * @param {object} data.cy The cytoscape instance
                     */
                    tap: 'onClick',
                    /**
                     * @callback org.visallo.graph.node.decoration~onMouseOver
                     * @this The decoration cytoscape node
                     * @param {object} event The {@link http://js.cytoscape.org/#events/event-object|Cytoscape event} object
                     * @param {object} data
                     * @param {object} data.vertex The vertex this decoration
                     * is attached
                     * @param {object} data.cy The cytoscape instance
                     */
                    mouseover: 'onMouseOver',
                    /**
                     * @callback org.visallo.graph.node.decoration~onMouseOut
                     * @this The decoration cytoscape node
                     * @param {object} event The {@link http://js.cytoscape.org/#events/event-object|Cytoscape event} object
                     * @param {object} data
                     * @param {object} data.vertex The vertex this decoration
                     * is attached
                     * @param {object} data.cy The cytoscape instance
                     */
                    mouseout: 'onMouseOut'
                }[event.type];
                if (_.isFunction(decoration.onClick)) {
                    if (handlerName === 'onMouseOver') {
                        this.node.style.cursor = 'pointer';
                    } else if (handlerName === 'onMouseOut' || handlerName === 'onClick') {
                        this.node.style.cursor = null;
                    }
                }
                if (_.isFunction(decoration[handlerName])) {
                    decoration[handlerName].call(cyTarget, event, {
                        cy,
                        vertex: cyTarget.data('vertex')
                    });
                }
            }
        },

        onMouseOver({ cy, cyTarget }) {
            clearTimeout(this.hoverMouseOverTimeout);

            if (cyTarget !== cy && cyTarget.is('node.v')) {
                this.hoverMouseOverTimeout = _.delay(() => {
                    if (cyTarget.data('isTruncated')) {
                        var nId = cyTarget.id();
                        this.setState({ hovering: nId })
                    }
                }, 500);
            }
        },

        onMouseOut({ cy, cyTarget }) {
            clearTimeout(this.hoverMouseOverTimeout);
            if (cyTarget !== cy && cyTarget.is('node.v')) {
                if (this.state.hovering) {
                    this.setState({ hovering: null })
                }
            }
        },

        onFileImportSuccess(event, { vertexIds, position }) {
            const { x, y } = position;
            const { left, top } = this.node.getBoundingClientRect();
            const pos = this.droppableTransformPosition({
                x: x - left,
                y: y - top
            });
            this.props.onDropElementIds({vertexIds}, pos);
        },

        onKeyboard(event) {
            const { type } = event;
            const cytoscape = this.cytoscape;

            switch (type) {
                case 'fit': cytoscape.fit();
                    break;
                case 'zoomIn': cytoscape.onControlsZoom('in')
                    break;
                case 'zoomOut': cytoscape.onControlsZoom('out')
                    break;
                default:
                    console.warn(type);
            }
        },

        onMenuSelect(identifier) {
            const cy = this.cytoscape.state.cy;
            const selector = _.findWhere(
                this.props.registry['org.visallo.graph.selection'],
                { identifier }
            );
            if (selector) {
                selector(cy);
            }
        },

        onMenuExport(componentPath) {
            var exporter = _.findWhere(
                    this.props.registry['org.visallo.graph.export'],
                    { componentPath }
                );

            if (exporter) {
                const cy = this.cytoscape.state.cy;
                const { product } = this.props;
                Promise.require('util/popovers/exportWorkspace/exportWorkspace').then(ExportWorkspace => {
                    ExportWorkspace.attachTo(cy.container(), {
                        exporter: exporter,
                        workspaceId: product.workspaceId,
                        productId: product.id,
                        cy: cy,
                        anchorTo: {
                            page: {
                                x: window.lastMousePositionX,
                                y: window.lastMousePositionY
                            }
                        }
                    });
                });
            }
        },

        onCollapseSelectedNodes(nodes) {
            const { product, productElementIds, rootId } = this.props;
            const collapsedNodes = product.extendedData.compoundNodes;
            let vertexIds = [];

            if (nodes.length < 2) return;

            nodes.forEach(node => {
                if (node.data.vertexIds) {
                    vertexIds = vertexIds.concat(node.data.vertexIds);
                } else {
                    vertexIds.push(node.id());
                }
            });

            const positions = nodes.map(node => retina.pixelsToPoints(node.position()));
            const pos = {
                x: positions.reduce((total, pos) => total + pos.x, 0) / positions.length,
                y: positions.reduce((total, pos) => total + pos.y, 0) / positions.length
            };

            this.props.onCollapseNodes(product.id, {
                children: vertexIds,
                pos,
                parent: rootId
             });
        },

        onMenuCreateVertex({pageX, pageY }) {
            const position = { x: pageX, y: pageY };
            this.createVertex(position);
        },

        previewVertex(event, data) {
            const cy = this.cytoscape.state.cy;

            Promise.all([
                Promise.require('util/popovers/detail/detail'),
                F.vertex.getVertexIdsFromDataEventOrCurrentSelection(data, { async: true })
            ]).spread((DetailPopover, ids) => {
                if (!this.detailPopoversMap) {
                    this.detailPopoversMap = {};
                }
                const currentPopovers = Object.keys(this.detailPopoversMap);
                const remove = _.intersection(ids, currentPopovers);
                var add = _.difference(ids, currentPopovers)

                remove.forEach(id => {
                    const cyNode = cy.getElementById(id);
                    if (cyNode.length) {
                        $(this.detailPopoversMap[id]).teardownAllComponents().remove();
                        delete this.detailPopoversMap[id];
                    }
                })
                const availableToOpen = MaxPreviewPopovers - (currentPopovers.length - remove.length);
                if (add.length && add.length > availableToOpen) {
                    $(this.node).trigger('displayInformation', { message: i18n('popovers.preview_vertex.too_many', MaxPreviewPopovers) });
                    add = add.slice(0, Math.max(0, availableToOpen));
                }

                add.forEach(id => {
                    var $popover = $('<div>').addClass('graphDetailPanePopover').appendTo(this.node);
                    this.detailPopoversMap[id] = $popover[0];
                    DetailPopover.attachTo($popover[0], {
                        vertexId: id,
                        anchorTo: {
                            vertexId: id
                        }
                    });
                })
            });
        },

        createVertex(position) {
            if (!position) {
                position = { x: window.lastMousePositionX, y: window.lastMousePositionY };
            }


            if (this.props.workspace.editable) {
                Promise.require('util/popovers/fileImport/fileImport')
                    .then(CreateVertex => {
                        CreateVertex.attachTo(this.node, {
                            anchorTo: { page: position }
                        });
                    });
            }
        },

        onUpdatePreview(data) {
            this.props.onUpdatePreview(this.props.product.id, data)
        },

        cancelDraw() {
            const cy = this.cytoscape.state.cy;
            cy.autoungrabify(false);
            this.setState({ draw: null })
        },

        onTapHold({ cy, cyTarget }) {
            if (cy !== cyTarget) {
                this.previewVertex(null, { vertexId: cyTarget.id() })
            }
        },

        onTapStart(event) {
            const { cy, cyTarget } = event;
            if (cy !== cyTarget && event.originalEvent.ctrlKey) {
                cy.autoungrabify(true);
                this.setState({
                    draw: {
                        vertexId: cyTarget.id()
                    }
                });
            }
        },

        onTap(event) {
            const { cy, cyTarget, cyPosition } = event;
            const { x, y } = cyPosition;
            const { ctrlKey, shiftKey } = event.originalEvent;
            const { draw, paths } = this.state;

            if (paths) {
                if (cy === cyTarget && _.isEmpty(this.props.selection.vertices) && _.isEmpty(this.props.selection.edges)) {
                    $(document).trigger('defocusPaths');
                    this.setState({ paths: null })
                }
            }
            if (draw) {
                const upElement = cy.renderer().findNearestElement(x, y, true, false);
                if (!upElement || draw.vertexId === upElement.id()) {
                    this.cancelDraw();
                    if (ctrlKey && upElement) {
                        this.onContextTap(event);
                    }
                } else if (!upElement.isNode()) {
                    this.cancelDraw();
                } else {
                    this.setState({ draw: {...draw, toVertexId: upElement.id() } });
                    this.showConnectionPopover();
                }
            } else {
                if (ctrlKey) {
                    this.onContextTap(event);
                } else if (!shiftKey && cy === cyTarget) {
                    this.coalesceSelection('clear');
                    this.props.onClearSelection();
                }
            }
        },

        onCxtTapEnd(event) {
            const { cy, cyTarget } = event;
            if (cy !== cyTarget && event.originalEvent.ctrlKey) {
                this.onTap(event);
            }
        },

        onContextTap(event) {
            const { cyTarget, cy, originalEvent } = event;
            // TODO: show all selected objects if not on item
            if (cyTarget !== cy) {
                const { pageX, pageY } = originalEvent;
                if (cyTarget.is('node.c')) {
                    this.props.onCollapsedItemMenu(originalEvent.target, cyTarget.id(), { x: pageX, y: pageY });
                } else if (cyTarget.isNode()) {
                    this.props.onVertexMenu(originalEvent.target, cyTarget.id(), { x: pageX, y: pageY });
                } else {
                    const edgeIds = _.pluck(cyTarget.data('edgeInfos'), 'edgeId');
                    this.props.onEdgeMenu(originalEvent.target, edgeIds, { x: pageX, y: pageY });
                }
            }
        },

        onRemove({ cyTarget }) {
            if (isValidElement(cyTarget)) {
                this.coalesceSelection('remove', getCyItemTypeAsString(cyTarget), cyTarget);
            }
        },

        onSelect({ cyTarget }) {
            if (isValidElement(cyTarget)) {
                this.coalesceSelection('add', getCyItemTypeAsString(cyTarget), cyTarget);
            }
        },

        onUnselect({ cyTarget }) {
            if (isValidElement(cyTarget)) {
                this.coalesceSelection('remove', getCyItemTypeAsString(cyTarget), cyTarget);
            }
        },

        onLayoutStop() {
            this.sendPositionUpdates();
        },

        onFree() {
            this.sendPositionUpdates();
        },

        sendPositionUpdates() {
            const { vertices, compoundNodes: collapsedNodes } = this.props.product.extendedData;

            if (!_.isEmpty(this.cyNodeIdsWithPositionChanges)) {
                const positionUpdates = _.mapObject(this.cyNodeIdsWithPositionChanges, (cyNode, id) => {
                    const update = vertices[id] || collapsedNodes[id];
                    update.pos = retina.pixelsToPoints(cyNode.position());
                    return update;
                });

                this.props.onUpdatePositions(
                    this.props.product.id,
                    positionUpdates
                );
                this.cyNodeIdsWithPositionChanges = {};
            }
        },

        onPosition({ cyTarget }) {
            if (isValidNode(cyTarget)) {
                var id = cyTarget.id();
                this.cyNodeIdsWithPositionChanges[id] = cyTarget;
            }
        },

        onViewport({ cy }) {
            var zoom = cy.zoom(), pan = cy.pan();
            if (!this.currentViewport) this.currentViewport = {};
            const viewport = { zoom, pan: {...pan}};
            this.currentViewport[this.props.product.id] = viewport;
        },

        droppableTransformPosition(rpos) {
            const cy = this.cytoscape.state.cy;
            const pan = cy.pan();
            const zoom = cy.zoom();
            return retina.pixelsToPoints({
                x: (rpos.x - pan.x) / zoom,
                y: (rpos.y - pan.y) / zoom
            });
        },
//
//        getCollapseDataContainingVertexId(vertexId) {
//            const collapseData = this.props.product.extendedData[COLLAPSED_EXTENDED_DATA_KEY];
//            if (!collapseData) {
//                return null;
//            }
//            const matchingCollapsedItems = Object.keys(collapseData).filter(collapseItemId => {
//                return collapseData[collapseItemId].vertexIds.includes(vertexId);
//            });
//            return matchingCollapsedItems.length > 0 ? matchingCollapsedItems[0] : null;
//        },

        getRootNode() {
            const { product, productElementIds, rootId } = this.props;
            const productVertices = productElementIds.vertices;
            const collapsedNodes = product.extendedData.compoundNodes;

            if (collapsedNodes[rootId] && collapsedNodes[rootId].visible) {
                return collapsedNodes[id];
            } else {
                const children = [];

                [productVertices, collapsedNodes].forEach((type) => {
                    _.mapObject(type, (item, id) => {
                        if (item.parent === 'root') {
                           children.push(id);
                        }
                    })
                });

                return { id: 'root', children }
            }
        },

        mapPropsToElements(editable) {
            const { selection, ghosts, productElementIds, elements, ontology, registry, focusing, product } = this.props;
            const { hovering } = this.state;
            const { vertices: productVertices, edges: productEdges } = productElementIds;
            const { vertices, edges } = elements;
            const { vertices: verticesSelectedById, edges: edgesSelectedById } = selection;
            const collapsedNodes = _.pick(product.extendedData.compoundNodes, ({ visible }) => visible);

            const rootNode = this.getRootNode();
            const filterByRoot = (items) => _.values(_.pick(items, rootNode.children));

            const cyNodeConfig = (node) => {
                const { id, type, pos, children, parent, title } = node;
                let selected, classes, data;

                if (type === 'vertex') {
                   selected = id in verticesSelectedById;
                   classes = mapVertexToClasses(id, vertices, focusing, registry['org.visallo.graph.node.class']);
                   data = mapVertexToData(id, vertices, registry['org.visallo.graph.node.transformer'], hovering);

                   if (data) {
                       renderedNodeIds[id] = true;
                   }
                } else {
                   const vertexIds = getVertexIdsFromCollapsedNode(collapsedNodes, id);
                   selected = vertexIds.some(id => id in verticesSelectedById)
                   classes = mapCollapsedNodeToClasses(id, collapsedNodes, focusing, vertexIds, registry['org.visallo.graph.collapsed.class']);
                   data = {
                       ...node,
                       vertexIds,
                       truncatedTitle: title || F.string.truncate(generateCollapsedNodeTitle(node, vertices), 3),
                       imageSrc: this.state.collapsedImageDataUris[id] && this.state.collapsedImageDataUris[id].imageDataUri || 'img/loading-large@2x.png'
                   }
                }

                return {
                    group: 'nodes',
                    data,
                    classes,
                    position: retina.pointsToPixels(pos),
                    selected,
                    grabbable: editable
                }
            }

            const renderedNodeIds = {};

            const cyVertices = filterByRoot(productVertices).reduce((nodes, nodeData) => {
                const { type, id, pos, parent } = nodeData;
                const cyNode = cyNodeConfig(nodeData);

                if (ghosts && id in ghosts) {
                    const ghostData = {
                        ...cyNode.data,
                        id: `${cyNode.data.id}-ANIMATING`,
                        animateTo: {
                            id: nodeData.id,
                            pos: { ...cyNode.position }
                        }
                    };
                    delete ghostData.parent;
                    nodes.push({
                        ...cyNode,
                        data: ghostData,
                        position: retina.pointsToPixels(ghosts[id]),
                        grabbable: false,
                        selectable: false
                    });
                }

                if (id in vertices) {
                    const markedAsDeleted = vertices[id] === null;
                    if (markedAsDeleted) {
                        return nodes;
                    }
                    const vertex = vertices[id];
                    const applyDecorations = memoizeFor('org.visallo.graph.node.decoration#applyTo', vertex, () => {
                        return _.filter(registry['org.visallo.graph.node.decoration'], function(e) {
                            /**
                             * @callback org.visallo.graph.node.decoration~applyTo
                             * @param {object} vertex
                             * @returns {boolean} Whether the decoration should be
                             * added to the node representing the vertex
                             */
                            return !_.isFunction(e.applyTo) || e.applyTo(vertex);
                        });
                    });
                    if (applyDecorations.length) {
                        const parentId = 'decP' + id;
                        cyNode.data.parent = parentId;
                        const decorations = memoizeFor('org.visallo.graph.node.decoration#data', vertex, () => {
                            return applyDecorations.map(dec => {
                                const data = mapDecorationToData(dec, vertex, () => this.forceUpdate());
                                if (!data) {
                                    return;
                                }
                                var { padding } = dec;
                                return {
                                    group: 'nodes',
                                    classes: mapDecorationToClasses(dec, vertex),
                                    data: {
                                        ...data,
                                        id: idForDecoration(dec, vertex.id),
                                        alignment: dec.alignment,
                                        padding,
                                        parent: parentId,
                                        vertex
                                    },
                                    position: { x: -1, y: -1 },
                                    grabbable: false,
                                    selectable: false
                                }
                            })
                        });

                        nodes.push({
                            group: 'nodes',
                            data: { id: parentId },
                            classes: 'decorationParent',
                            selectable: false,
                            grabbable: false
                        });
                        nodes.push(cyNode);
                        decorations.forEach(d => {
                            if (d) nodes.push(d);
                        });
                    } else if (cyNode) {
                        nodes.push(cyNode);
                    }
                } else if (cyNode) {
                    nodes.push(cyNode);
                }

                return nodes
            }, []);

            _.defer(() => {
                CollapsedNodeImageHelpers.updateImageDataUrisForCollapsedNodes(
                    _.pick(collapsedNodes, ({ id }) => rootNode.children.includes(id)),
                    this.props.elements.vertices,
                    this.state.collapsedImageDataUris,
                    (newCollapsedImageDataUris) => {
                        this.setState({
                            collapsedImageDataUris: newCollapsedImageDataUris
                        });
                    }
                );
            });

            const cyCollapsedNodes = filterByRoot(collapsedNodes).reduce((nodes, nodeData) => {
                const { type, id, pos, parent, children } = nodeData;
                const cyNode = cyNodeConfig(nodeData);

                renderedNodeIds[id] = true;

                if (ghosts) {
                    _.mapObject(ghosts, (ghost => {
                        if (ghost.id in cyNode.vertexIds) {
                            const ghostData = {
                                ...cyNode.data,
                                id: `${cyNode.data.id}-ANIMATING`,
                                animateTo: {
                                    id,
                                    pos: {...cyNode.position}
                                }
                            };
                            delete ghostData.parent;
                            nodes.push({
                                ...cyNode,
                                data: ghostData,
                                position: retina.pointsToPixels(ghosts[id]),
                                grabbable: false,
                                selectable: false
                            });
                        }
                    }));
                }

                nodes.push(cyNode);
                return nodes;
            }, []);

            const cyNodes = cyVertices.concat(cyCollapsedNodes);

            const cyEdges = _.chain(productEdges)
                .filter(edgeInfo => {
                    const elementMarkedAsDeletedInStore =
                        edgeInfo.edgeId in edges &&
                        edges[edgeInfo.edgeId] === null;
                    const edgeNodesExist = edgeInfo.inVertexId in renderedNodeIds && edgeInfo.outVertexId in renderedNodeIds;

                    return !elementMarkedAsDeletedInStore && edgeNodesExist;
                })
                .groupBy(generateCompoundEdgeId)
                .map((edgeInfos, id) => {
                    const {inVertexId, outVertexId} = edgeInfos[0];
                    return {
                        inCollapsedNodeId: getRenderedParentNodeFromVertexId(inVertexId),
                        outCollapsedNodeId: getRenderedParentNodeFromVertexId(outVertexId),
                        edgeInfos,
                        id
                    };

                    function getRenderedParentNodeFromVertexId(vertexId) {
                        let parentId = productVertices[vertexId].parent;
                        while (parentId !== rootNode.id && !(parentId in renderedNodeIds)) {
                            parentId = productVertices[vertexId].parent;
                            if (!collapsedNodes[parentId]) return null;
                        }
                        return parentId === rootNode.id ? null : parentId;
                    }
                })
                .filter(({inCollapsedNodeId, outCollapsedNodeId}) => {
                    // exclude collapsed node self-referencing edges
                    return !outCollapsedNodeId || !inCollapsedNodeId
                        || (outCollapsedNodeId !== inCollapsedNodeId)
                })
                .map(data => {
                    const edgesForInfos = Object.values(_.pick(edges, _.pluck(data.edgeInfos, 'edgeId')));
                    return {
                        data: mapEdgeToData(data, edgesForInfos, ontology, registry['org.visallo.graph.edge.transformer']),
                        classes: mapEdgeToClasses(data.edgeInfos, edgesForInfos, focusing, registry['org.visallo.graph.edge.class']),
                        selected: _.any(data.edgeInfos, e => e.edgeId in edgesSelectedById)
                    }
                })
                .value();

            return { nodes: cyNodes, edges: cyEdges };

        },

        resetQueuedSelection(sel) {
            this._queuedSelection = sel ? {
                add: { vertices: sel.vertices, edges: sel.edges },
                remove: {vertices: {}, edges: {}}
            } : { add: {vertices: {}, edges: {}}, remove: {vertices: {}, edges: {}} };

            if (!this._queuedSelectionTrigger) {
                this._queuedSelectionTrigger = _.debounce(() => {
                    const vertices = Object.keys(this._queuedSelection.add.vertices);
                    const edges = Object.keys(this._queuedSelection.add.edges);
                    if (vertices.length || edges.length) {
                        this.props.onSetSelection({ vertices, edges })
                    } else {
                        this.props.onClearSelection();
                    }
                }, 100);
            }
        },

        coalesceSelection(action, type, cyElementOrId) {
            if (!this._queuedSelection) {
                this.resetQueuedSelection();
            }
            let id = cyElementOrId;

            if (cyElementOrId && _.isFunction(cyElementOrId.data)) {
                if (type === 'collapsedNode') {
                    const collapsedNode = this.props.product.extendedData.compoundNodes[id];
                    //TODO: getting previous collapsedNode id for this not new
                    if (!collapsedNode) {
                        console.error(`could not find collapsed node with id: ${id}`);
                        return;
                    }
                    collapsedNode.vertexIds.forEach(vertexId => {
                        this.coalesceSelection(action, 'vertices', vertexId);
                    });
                    return;
                } else if (type === 'edges') {
                    cyElementOrId.data('edgeInfos').forEach(edgeInfo => {
                        this.coalesceSelection(action, type, edgeInfo.edgeId);
                    });
                    return;
                } else if (type === 'vertices') {
                    id = cyElementOrId.id();
                } else {
                    console.error(`Invalid type: ${type}`);
                    return;
                }
            }

            if (action !== 'clear') {
                this._queuedSelection[action][type][id] = id;
            }

            if (action === 'add') {
                delete this._queuedSelection.remove[type][id]
            } else if (action === 'remove') {
                delete this._queuedSelection.add[type][id]
            } else if (action === 'clear') {
                this._queuedSelection.add.vertices = {};
                this._queuedSelection.add.edges = {};
                this._queuedSelection.remove.vertices = {};
                this._queuedSelection.remove.edges = {};
            } else {
                console.warn('Unknown action: ', action)
            }

            this._queuedSelectionTrigger();
        },

        showConnectionPopover() {
            const cy = this.cytoscape.state.cy;
            const { connectionType, vertexId, toVertexId, connectionData } = this.state.draw;
            const Popover = Popovers(connectionType);
            Popover.teardownAll();
            Popover.attachTo(this.node, {
                cy,
                cyNode: cy.getElementById(toVertexId),
                otherCyNode: cy.getElementById(vertexId),
                edge: cy.$('edge.drawEdgeToMouse'),
                outVertexId: vertexId,
                inVertexId: toVertexId,
                connectionData
            });
        },

        legacyListeners(map) {
            this.removeEvents = [];

            _.each(map, (handler, events) => {
                var node = this.node;
                var func = handler;
                if (!_.isFunction(handler)) {
                    node = handler.node;
                    func = handler.handler;
                }
                this.removeEvents.push({ node, func, events });
                $(node).on(events, func);
            })
        }
    });

    const getVertexIdsFromCollapsedNode = (collapsedNodes, collapsedNodeId) => {
        
        const vertexIds = [];
        const queue = [collapsedNodes[collapsedNodeId]];

        while (queue.length > 0) {
            const collapsedNode = queue.pop();
            collapsedNode.children.forEach(id => {
                if (collapsedNodes[id]) {
                    queue.push(collapsedNodes[id])
                } else {
                    vertexIds.push(id);
                }
            });
        }

        return vertexIds;
    };

    const mapEdgeToData = (data, edges, ontology, transformers) => {
        const { id, edgeInfos, outCollapsedNodeId, inCollapsedNodeId } = data;

        return memoizeFor('org.visallo.graph.edge.transformer', edges, () => {
            const { inVertexId, outVertexId, label } = edgeInfos[0];
            const base = {
                id,
                source: outCollapsedNodeId || outVertexId,
                target: inCollapsedNodeId || inVertexId,
                type: label,
                label: edgeDisplay(label, ontology, edgeInfos),
                edgeInfos,
                edges
            };

            if (edges.length) {
                return transformers.reduce((data, fn) => {

                    /**
                     * Mutate the object to change the edge data.
                     *
                     * @callback org.visallo.graph.edge.transformer~transformerFn
                     * @param {object} data The cytoscape data object
                     * @param {string} data.source The source vertex id
                     * @param {string} data.target The target vertex id
                     * @param {string} data.type The edge label IRI
                     * @param {string} data.label The edge label display value
                     * @param {array.<object>} data.edgeInfos
                     * @param {array.<object>} data.edges
                     * @example
                     * function transformer(data) {
                     *     data.myCustomAttr = '';
                     * }
                     */
                    fn(data)
                    return data;
                }, base)
            }

            return base;
        }, () => id)
    };

    const mapEdgeToClasses = (edgeInfos, edges, focusing, classers) => {
        let cls = [];
        if (edges.length) {

            /**
             * Mutate the classes array to adjust the classes.
             *
             * @callback org.visallo.graph.edge.class~classFn
             * @param {array.<object>} edges List of edges that are collapsed into the drawn line. `length >= 1`.
             * @param {string} type EdgeLabel of the collapsed edges.
             * @param {array.<string>} classes List of classes that will be added to cytoscape edge.
             * @example
             * function(edges, type, cls) {
             *     cls.push('org-example-cls');
             * }
             */

            cls = memoizeFor('org.visallo.graph.edge.class', edges, function() {
                const cls = [];
                classers.forEach(fn => fn(edges, edgeInfos.label, cls));
                cls.push('e');
                return cls;
            }, () => edges.map(e => e.id).sort())
        } else {
            cls.push('partial')
        }

        const classes = cls.join(' ');

        if (_.any(edgeInfos, info => info.edgeId in focusing.edges)) {
            return classes + ' focus';
        }
        return classes;
    };

    const decorationIdMap = {};

    const decorationForId = id => {
        return decorationIdMap[id];
    };

    const idForDecoration = (function() {
        const decorationIdCache = new WeakMap();
        const vertexIdCache = {};
        var decorationIdCacheInc = 0, vertexIdCacheInc = 0;
        return (decoration, vertexId) => {
            var id = decorationIdCache.get(decoration);
            if (!id) {
                id = decorationIdCacheInc++;
                decorationIdCache.set(decoration, id);
            }
            var vId;
            if (vertexId in vertexIdCache) {
                vId = vertexIdCache[vertexId];
            } else {
                vId = vertexIdCacheInc++;
                vertexIdCache[vertexId] = vId;
            }
            var full = `dec${vId}-${id}`;
            decorationIdMap[full] = decoration;
            return full;
        }
    })();
    const mapDecorationToData = (decoration, vertex, update) => {
        const getData = () => {
            var data;
            /**
             * _**Note:** This will be called for every vertex change event
             * (`verticesUpdated`). Cache/memoize the result if possible._
             *
             * @callback org.visallo.graph.node.decoration~data
             * @param {object} vertex
             * @returns {object} The cytoscape data object for a decoration
             * given a vertex
             */
            if (_.isFunction(decoration.data)) {
                data = decoration.data(vertex);
            } else if (decoration.data) {
                data = decoration.data;
            }
            if (!_.isObject(data)) {
                throw new Error('data is not an object', data)
            }
            var p = Promise.resolve(data);
            p.catch(e => console.error(e))
            p.tap(() => {
                update()
            });
            return p;
        };
        const getIfFulfilled = p => {
            if (p.isFulfilled()) return p.value();
        }
        return getIfFulfilled(getData());
    };
    const mapDecorationToClasses = (decoration, vertex) => {
        var cls = ['decoration'];

        if (_.isString(decoration.classes)) {
            cls = cls.concat(decoration.classes.trim().split(/\s+/));
        } else if (_.isFunction(decoration.classes)) {

            /**
             * @callback org.visallo.graph.node.decoration~classes
             * @param {object} vertex
             * @returns {array.<string>|string} The classnames to add to the
             * node, either an array of classname strings, or space-separated
             * string
             */
            var newClasses = decoration.classes(vertex);
            if (!_.isArray(newClasses) && _.isString(newClasses)) {
                newClasses = newClasses.trim().split(/\s+/);
            }
            if (_.isArray(newClasses)) {
                cls = cls.concat(newClasses)
            }
        }
        return cls.join(' ');
    };

    const mapVertexToClasses = (id, vertices, focusing, classers) => {
        let cls = [];
        if (id in vertices) {
            const vertex = vertices[id];

            /**
             * Mutate the classes array to adjust the classes.
             *
             * @callback org.visallo.graph.node.class~classFn
             * @param {object} vertex The vertex that represents the node
             * @param {array.<string>} classes List of classes that will be added to cytoscape node.
             * @example
             * function(vertex, cls) {
             *     cls.push('org-example-cls');
             * }
             */
            cls = memoizeFor('org.visallo.graph.node.class', vertex, function() {
                const cls = [];
                classers.forEach(fn => fn(vertex, cls));
                cls.push('v');
                return cls;
            })
        } else {
            cls.push('partial')
        }

        const classes = cls.join(' ');
        if (id in focusing.vertices) {
            return classes + ' focus';
        }
        return classes;
    };

    const mapCollapsedNodeToClasses = (collapsedNodeId, collapsedNodes, focusing, vertexIds, classers) => {
        const cls = [];
        if (collapsedNodeId in collapsedNodes) {
            const collapsedNode = collapsedNodes[collapsedNodeId];

            /**
             * Mutate the classes array to adjust the classes.
             *
             * @callback org.visallo.graph.collapsed.class~classFn
             * @param {object} collapsedNode The collapsed item that represents the node
             * @param {array.<string>} classes List of classes that will be added to cytoscape node.
             * @example
             * function(collapsedNode, cls) {
             *     cls.push('org-example-cls');
             * }
             */
            classers.forEach(fn => fn(collapsedNode, cls));
            cls.push('c');

            if (vertexIds.some(vertexId => vertexId in focusing.vertices)) {
                cls.push('focus');
            }
        } else {
            cls.push('partial');
        }
        return cls.join(' ');
    };

//    const collapsedNodeToCyNode = (collapsedNodes, vertices, id) => {
//        const result = memoizeFor('collapsedNodeToCyNode', collapsedNodes[id], function() {
//            const collapsedNode = collapsedNodes[id];
//            const title = generateCollapsedNodeTitle(collapsedNode, vertices);
//            const truncatedTitle = F.string.truncate(title, 3);
//            const conceptType = F.vertex.prop(vertex, 'conceptType');
//            const imageSrc = CollapsedNodeImageHelpers.generateImageDataUriForCollapsedNode(
//               vertices,
//               collapsedNode
//           );
//
//            return {
//                ...collapsedNode,
//                title,
//                truncatedTitle,
//                isTruncated: title !== truncatedTitle,
//                imageSrc,
//                selectedImageSrc: imageSrc
//            };
//        }, () => id);
//
//        return result;
//    }

    const getCyItemTypeAsString = (item) => {
        if (item.isNode()) {
            return item.data.vertexIds ? 'collapsedNode' : 'vertices';
        }
        return 'edges';
    };

    const generateCollapsedNodeTitle = (collapsedNode, vertices) => {
        const children = _.pick(vertices, collapsedNode.children);

        return children.length ?
            collapsedNode.children.map(vertexId => F.vertex.title(vertices[vertexId])).join(', ') :
            i18n('org.visallo.web.product.graph.collapsedNode.entities', collapsedNode.children.length)
    };

    const vertexToCyNode = (vertex, transformers, hovering) => {
        const result = memoizeFor('vertexToCyNode', vertex, function() {
            const title = F.vertex.title(vertex);
            const truncatedTitle = F.string.truncate(title, 3);
            const conceptType = F.vertex.prop(vertex, 'conceptType');
            const imageSrc = F.vertex.image(vertex, null, 150);
            const selectedImageSrc = F.vertex.selectedImage(vertex, null, 150);
            const startingData = {
                id: vertex.id,
                isTruncated: title !== truncatedTitle,
                truncatedTitle,
                conceptType,
                imageSrc,
                selectedImageSrc
            };

            return transformers.reduce((data, t) => {
                /**
                 * Mutate the data object that gets passed to Cytoscape.
                 *
                 * @callback org.visallo.graph.node.transformer~transformerFn
                 * @param {object} vertex The vertex representing this node
                 * @param {object} data The cytoscape data object
                 * @example
                 * function transformer(vertex, data) {
                 *     data.myCustomAttr = '...';
                 * }
                 */
                t(vertex, data)
                return data;
            }, startingData);
        });
        
        if (hovering === vertex.id) {
            return { ...result, truncatedTitle: title }
        }
            
        return result;
    }

    const mapVertexToData = (id, vertices, transformers, hovering) => {
        if (id in vertices) {
            if (vertices[id] === null) {
                return;
            } else {
                const vertex = vertices[id];
                return vertexToCyNode(vertex, transformers, hovering);
            }
        } else {
            return { id }
        }
    };

    const CONFIGURATION = (props) => {
        const { pixelRatio, uiPreferences, product, registry } = props;
        const { edgeLabels } = uiPreferences;
        const edgesCount = product.extendedData.edges.length;
        const styleExtensions = registry['org.visallo.graph.style'];

        return {
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
            style: styles({ pixelRatio, edgesCount, edgeLabels, styleExtensions })
        }
    };

    return RegistryInjectorHOC(Graph, [
        'org.visallo.graph.edge.class',
        'org.visallo.graph.edge.transformer',
        'org.visallo.graph.export',
        'org.visallo.graph.node.class',
        'org.visallo.graph.node.decoration',
        'org.visallo.graph.node.transformer',
        'org.visallo.graph.collapsed.class',
        'org.visallo.graph.options',
        'org.visallo.graph.selection',
        'org.visallo.graph.style',
        'org.visallo.graph.view'
    ]);
});

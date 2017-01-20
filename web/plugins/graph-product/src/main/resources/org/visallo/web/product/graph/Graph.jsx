define([
    'react',
    './Cytoscape',
    './popoverHelper',
    './styles',
    './GraphEmpty',
    './GraphExtensionViews',
    './popovers/index',
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
    const isValidElement = cyElement => cyElement && cyElement.is('.v,.e,.partial') && !isGhost(cyElement);
    const isValidNode = cyElement => cyElement && cyElement.is('node.v,node.partial') && !isGhost(cyElement);
    const edgeDisplay = (label, ontology, edges) => {
        const display = label in ontology.relationships ? ontology.relationships[label].displayName : '';
        const showNum = edges.length > 1;
        const num = showNum ? ` (${F.number.pretty(edges.length)})` : '';
        return display + num;
    };
    const Graph = React.createClass({

        propTypes: {
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
                hovering: null
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
            this.cyNodeIdsWithPositionChanges = {};

            this.popoverHelper = new PopoverHelper(this.refs.node, this.cy);
            this.legacyListeners({
                addRelatedDoAdd: (event, data) => {
                    this.props.onAddRelated(this.props.product.id, data.addVertices)
                },
                selectAll: (event, data) => {
                    this.refs.cytoscape.state.cy.elements().select();
                },
                selectConnected: (event, data) => {
                    event.stopPropagation();
                    const cy = this.refs.cytoscape.state.cy;
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
                    onMenuExport: this.onMenuExport
                },
                cyElements = this.mapPropsToElements(editable),
                extensionViews = registry['org.visallo.graph.view'];

            return (
                <div ref="node" className="org-visallo-graph" style={{ height: '100%' }}>
                    <Cytoscape
                        ref="cytoscape"
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
                        onUpdatePreview={this.onUpdatePreview}></Cytoscape>

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
            return this.props.registry['org.visallo.graph.options'].map(e => ({
                identifier: e.identifier,
                componentPath: e.optionComponentPath
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
                    tap: 'onClick',
                    mouseover: 'onMouseOver',
                    mouseout: 'onMouseOut'
                }[event.type];
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
            const { left, top } = this.refs.node.getBoundingClientRect();
            const pos = this.droppableTransformPosition({
                x: x - left,
                y: y - top
            });
            this.props.onDropElementIds({vertexIds}, pos);
        },

        onKeyboard(event) {
            const { type } = event;
            const cytoscape = this.refs.cytoscape;

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
            const cy = this.refs.cytoscape.state.cy;
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
                const cy = this.refs.cytoscape.state.cy;
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

        onMenuCreateVertex({pageX, pageY }) {
            const position = { x: pageX, y: pageY };
            this.createVertex(position);
        },

        previewVertex(event, data) {
            const cy = this.refs.cytoscape.state.cy;

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
                    $(this.refs.node).trigger('displayInformation', { message: i18n('popovers.preview_vertex.too_many', MaxPreviewPopovers) });
                    add = add.slice(0, Math.max(0, availableToOpen));
                }

                add.forEach(id => {
                    var $popover = $('<div>').addClass('graphDetailPanePopover').appendTo(this.refs.node);
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
                        CreateVertex.attachTo(this.refs.node, {
                            anchorTo: { page: position }
                        });
                    });
            }
        },

        onUpdatePreview(data) {
            this.props.onUpdatePreview(this.props.product.id, data)
        },

        cancelDraw() {
            const cy = this.refs.cytoscape.state.cy;
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
                if (cyTarget.isNode()) {
                    this.props.onVertexMenu(originalEvent.target, cyTarget.id(), { x: pageX, y: pageY });
                } else {
                    const edgeIds = _.pluck(cyTarget.data('edgeInfos'), 'edgeId');
                    this.props.onEdgeMenu(originalEvent.target, edgeIds, { x: pageX, y: pageY });
                }
            }
        },

        onRemove({ cyTarget }) {
            if (isValidElement(cyTarget)) {
                this.coalesceSelection('remove', cyTarget.isNode() ? 'vertices' : 'edges', cyTarget);
            }
        },

        onSelect({ cyTarget }) {
            if (isValidElement(cyTarget)) {
                this.coalesceSelection('add', cyTarget.isNode() ? 'vertices' : 'edges', cyTarget);
            }
        },

        onUnselect({ cyTarget }) {
            if (isValidElement(cyTarget)) {
                this.coalesceSelection('remove', cyTarget.isNode() ? 'vertices' : 'edges', cyTarget);
            }
        },

        onLayoutStop() {
            this.sendPositionUpdates();
        },

        onFree() {
            this.sendPositionUpdates();
        },

        sendPositionUpdates() {
            if (!_.isEmpty(this.cyNodeIdsWithPositionChanges)) {
                this.props.onUpdatePositions(
                    this.props.product.id,
                    _.mapObject(this.cyNodeIdsWithPositionChanges, (cyNode, id) => retina.pixelsToPoints(cyNode.position()))
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
            const cy = this.refs.cytoscape.state.cy;
            const pan = cy.pan();
            const zoom = cy.zoom();
            return retina.pixelsToPoints({
                x: (rpos.x - pan.x) / zoom,
                y: (rpos.y - pan.y) / zoom
            });
        },

        mapPropsToElements(editable) {
            const { selection, ghosts, productElementIds, elements, ontology, registry, focusing } = this.props;
            const { hovering } = this.state;
            const { vertices: productVertices, edges: productEdges } = productElementIds;
            const { vertices, edges } = elements;
            const { vertices: verticesSelectedById, edges: edgesSelectedById } = selection;
            const nodeIds = {};
            const cyNodeConfig = (id, pos, data) => {
                if (data) {
                    nodeIds[id] = true;
                    return {
                        group: 'nodes',
                        data,
                        classes: mapVertexToClasses(id, vertices, focusing, registry['org.visallo.graph.node.class']),
                        position: retina.pointsToPixels(pos),
                        selected: (id in verticesSelectedById),
                        grabbable: editable
                    }
                }
            };
            const cyNodes = productVertices.reduce((nodes, { id, pos }) => {
                const data = mapVertexToData(id, vertices, registry['org.visallo.graph.node.transformer'], hovering);
                const cyNode = cyNodeConfig(id, pos, data);

                if (cyNode && ghosts && id in ghosts) {
                    const ghostData = {
                        ...cyNode.data,
                        id: `${cyNode.data.id}-ANIMATING`,
                        animateTo: {
                            id: data.id,
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
                    const applyDecorations = _.filter(registry['org.visallo.graph.node.decoration'], function(e) {
                        return !_.isFunction(e.applyTo) || e.applyTo(vertex);
                    });
                    if (applyDecorations.length) {
                        const parentId = 'decP' + id;
                        cyNode.data.parent = parentId;
                        const decorations = applyDecorations.map(dec => {
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

            const cyEdges = _.chain(productEdges)
                .filter(edgeInfo => {
                    const elementMarkedAsDeletedInStore =
                        edgeInfo.edgeId in edges &&
                        edges[edgeInfo.edgeId] === null;
                    const edgeNodesExist = edgeInfo.inVertexId in nodeIds && edgeInfo.outVertexId in nodeIds;

                    return !elementMarkedAsDeletedInStore && edgeNodesExist;
                })
                .groupBy(generateCompoundEdgeId)
                .map((edgeInfos, id) => {
                    const edgesForInfos = Object.values(_.pick(edges, _.pluck(edgeInfos, 'edgeId')));
                    return {
                        data: mapEdgeToData(id, edgeInfos, edgesForInfos, ontology, registry['org.visallo.graph.edge.transformer']),
                        classes: mapEdgeToClasses(edgeInfos, edgesForInfos, focusing, registry['org.visallo.graph.edge.class']),
                        selected: _.any(edgeInfos, e => e.edgeId in edgesSelectedById)
                    }
                })
                .value()

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
            var id = cyElementOrId;

            if (cyElementOrId && _.isFunction(cyElementOrId.data)) {
                if (type === 'edges') {
                    cyElementOrId.data('edgeInfos').forEach(edgeInfo => {
                        this.coalesceSelection(action, type, edgeInfo.edgeId);
                    })
                    return;
                } else {
                    id = cyElementOrId.id();
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
            const cy = this.refs.cytoscape.state.cy;
            const { connectionType, vertexId, toVertexId, connectionData } = this.state.draw;
            const Popover = Popovers(connectionType);
            Popover.teardownAll();
            Popover.attachTo(this.refs.node, {
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
                var node = this.refs.node;
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


    const mapEdgeToData = (id, edgeInfos, edges, ontology, transformers) => {
        const { inVertexId, outVertexId, label } = edgeInfos[0];
        const base = {
            id,
            source: outVertexId,
            target: inVertexId,
            type: label,
            label: edgeDisplay(label, ontology, edgeInfos),
            edgeInfos,
            edges
        };

        if (edges.length) {
            return transformers.reduce((data, fn) => {
                fn(data)
                return data;
            }, base)
        }

        return base;
    };
    const mapEdgeToClasses = (edgeInfos, edges, focusing, classers) => {
        const cls = [];
        if (edges.length) {
            classers.forEach(fn => fn(edges, edgeInfos.label, cls));
            cls.push('e');
        } else cls.push('partial')
        if (_.any(edgeInfos, info => info.edgeId in focusing.edges)) {
            cls.push('focus');
        }
        return cls.join(' ')
    };
    const decorationIdMap = {};
    const decorationForId = id => {
        return decorationIdMap[id];
    }
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
    const mapDecorationToData = (function() {
        const decorations = new WeakMap();
        return (decoration, vertex, update) => {
            let vertexMap = decorations.get(decoration);
            if (!vertexMap) {
                vertexMap = {};
                decorations.set(decoration, vertexMap);
            }
            let cache = vertexMap[vertex.id]
            if (!cache) {
                cache = vertexMap[vertex.id] = { promise: null, data: null, previous: null };
            }

            const promise = cache.promise;
            const vertexChanged = cache.previous && vertex !== cache.previous;
            const getData = () => {
                var data;
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
            cache.previous = vertex;

            if (promise) {
                if (vertexChanged) {
                    promise.cancel();
                    cache.promise = getData();
                    return getIfFulfilled(cache.promise);
                } else {
                    return getIfFulfilled(promise);
                }
            } else {
                cache.promise = getData();
                return getIfFulfilled(cache.promise);
            }
        }
    })();
    const mapDecorationToClasses = (decoration, vertex) => {
        var cls = ['decoration'];

        if (_.isString(decoration.classes)) {
            cls = cls.concat(decoration.classes.trim().split(/\s+/));
        } else if (_.isFunction(decoration.classes)) {
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
        const cls = [];
        if (id in vertices) {
            const vertex = vertices[id];
            classers.forEach(fn => fn(vertex, cls));
            cls.push('v');
        } else cls.push('partial')
        if (id in focusing.vertices) {
            cls.push('focus')
        }
        return cls.join(' ')
    };
    const vertexToCyNode = (vertex, transformers, hovering) => {
        const title = F.vertex.title(vertex);
        const truncatedTitle = F.string.truncate(title, 3);
        const conceptType = F.vertex.prop(vertex, 'conceptType');
        const imageSrc = F.vertex.image(vertex, null, 150);
        const selectedImageSrc = F.vertex.selectedImage(vertex, null, 150);
        const startingData = {
            id: vertex.id,
            isTruncated: title !== truncatedTitle,
            truncatedTitle: hovering === vertex.id ? title : truncatedTitle,
            conceptType,
            imageSrc,
            selectedImageSrc
        };

        return transformers.reduce((data, t) => {
            t(vertex, data)
            return data;
        }, startingData);
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
        'org.visallo.graph.options',
        'org.visallo.graph.selection',
        'org.visallo.graph.style',
        'org.visallo.graph.view'
    ]);
});

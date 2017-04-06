define([
    'react',
    'underscore',
    'cytoscape',
    'fast-json-patch',
    'components/NavigationControls',
    'colorjs',
    './betterGrid',
    './Menu',
    './cytoscapeCorsFix',
    'util/formatters'
], function(
    React,
    _,
    cytoscape,
    jsonpatch,
    NavigationControls,
    colorjs,
    betterGrid,
    Menu,
    fixCytoscapeCorsHandling,
    F) {
    const { PropTypes } = React;
    const ANIMATION = { duration: 400, easing: 'spring(250, 20)' };
    const ANIMATION_SLOW = { ...ANIMATION, duration: 800 };
    const PanelPaddingBorder = 35;
    const MAX_ELEMENTS_BEFORE_NO_ANIMATE_LAYOUT = 50;
    const DEFAULT_PNG = Object.freeze({
        bg: 'white',
        full: true,
        maxWidth: 300,
        maxHeight: 300
    });
    const LAYOUT_OPTIONS = {
        // Customize layout options
        random: { padding: 10 },
        cose: {
            animate: function(els) {
                return els.length < MAX_ELEMENTS_BEFORE_NO_ANIMATE_LAYOUT;
            },
            edgeElasticity: 10
        },
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
    };
    const PREVIEW_DEBOUNCE_SECONDS = 3;
    const EVENTS = {
        drag: 'onDrag',
        free: 'onFree',
        grab: 'onGrab',
        position: 'onPosition',
        layoutstop: 'onLayoutStop',
        mouseover: 'onMouseOver',
        mouseout: 'onMouseOut',
        remove: 'onRemove',
        tap: 'onTap',
        tapstart: 'onTapStart',
        tapend: 'onTapEnd',
        taphold: 'onTapHold',
        cxttap: 'onContextTap',
        cxttapstart: 'onCxtTapStart',
        cxttapend: 'onCxtTapEnd',
        pan: 'onPan',
        zoom: 'onZoom',
        fit: 'onFit',
        change: 'onChange',
        select: 'onSelect',
        unselect: 'onUnselect'
    };
    const eventPropTypes = {};
    _.each(EVENTS, propKey => { eventPropTypes[propKey] = PropTypes.func })
    const zoomAcceleration = 5.0;
    const zoomDamping = 0.8;
    const DrawEdgeNodeId = 'DrawEdgeNodeId';

    const isEdge = data => (data.source !== undefined)
    const isNode = _.negate(isEdge)

    const Cytoscape = React.createClass({

        propTypes: {
            initialProductDisplay: PropTypes.bool,
            hasPreview: PropTypes.bool,
            editable: PropTypes.bool,
            ...eventPropTypes
        },

        getDefaultProps() {
            const eventProps = _.mapObject(_.invert(EVENTS), () => () => {})
            return {
                ...eventProps,
                animate: true,
                config: {},
                elements: { nodes: [], edges: [] },
                fit: false,
                hasPreview: false,
                initialProductDisplay: false,
                panelPadding: { left:0, right:0, top:0, bottom:0 },
                tools: [],
                onReady() {},
                onGhostFinished() {},
                onUpdatePreview() {}
            }
        },

        getInitialState() {
            return {
                showGraphMenu: false,
                controlDragSelection: null
            };
        },

        componentDidMount() {
            this.moving = {};
            this.updatePreview = this.props._disablePreviewDelay ?
                this._updatePreview :
                _.debounce(this._updatePreview, PREVIEW_DEBOUNCE_SECONDS * 1000);
            this.previousConfig = this.prepareConfig();
            const cy = cytoscape(this.previousConfig);
            const updateControlDragSelection = (nodeId = null) => this.setState({ controlDragSelection: nodeId });
            fixCytoscapeCorsHandling(cy);
            cytoscape('layout', 'bettergrid', betterGrid);

            this.clientRect = this.refs.cytoscape.getBoundingClientRect();
            this.setState({ cy })

            cy.on('tap mouseover mouseout', 'node.decoration', event => {
                this.props.onDecorationEvent(event);
            });
            cy.on('position grab free', 'node.v', ({ cyTarget }) => {
                if (cyTarget.isChild()) {
                    this.updateDecorationPositions(cyTarget);
                }
            })
            cy.on('mousemove', (event) => {
                const { controlDragSelection } = this.state;
                const { drawEdgeToMouseFrom } = this.props;
                const { cyTarget, cy } = event;
                const targetIsNode = cyTarget !== cy && cyTarget.is('node.v');

                if (drawEdgeToMouseFrom) {
                    if (targetIsNode && !drawEdgeToMouseFrom.toVertexId) {
                        if (cyTarget.data().id !== controlDragSelection) {
                            updateControlDragSelection(cyTarget.id());
                        }
                    } else if (controlDragSelection) {
                        updateControlDragSelection();
                    }

                    if (!drawEdgeToMouseFrom.toVertexId) {
                        const { pageX, pageY } = event.originalEvent;
                        const { left, top } = this.clientRect;
                        const node = cy.getElementById(DrawEdgeNodeId);

                        if (targetIsNode) {
                            node.position(cyTarget.position());
                        } else {
                            node.renderedPosition({ x: pageX - left, y: pageY - top });
                        }
                    }
                } else if (!drawEdgeToMouseFrom && controlDragSelection) {
                    updateControlDragSelection();
                }
            });
        },

        componentWillUnmount() {
            if (this.state.cy) {
                this.state.cy.destroy();
                this.unmounted = true;
            }
        },

        componentDidUpdate() {
            const { cy } = this.state;
            const { elements, drawEdgeToMouseFrom, initialProductDisplay, hasPreview } = this.props;
            const newData = { elements };
            const oldData = cy.json()
            const disableSelection = Boolean(drawEdgeToMouseFrom);

            this.drawEdgeToMouseFrom(newData);
            this.drawPaths(newData);
            this.drawControlDragSelection(newData);

            // Create copies of objects because cytoscape mutates :(
            const getAllData = nodes => nodes.map(({data, selected, grabbable, selectable, locked, position, renderedPosition, classes}) => ({
                data: {...data},
                selected: selected || false,
                classes,
                position: position && {...position},
                grabbable, selectable, locked,
                renderedPosition: renderedPosition && {...renderedPosition}
            }))
            const getTypeData = elementType => [oldData, newData].map(n => getAllData(n.elements[elementType] || []) )
            const [oldNodes, newNodes] = getTypeData('nodes')
            const [oldEdges, newEdges] = getTypeData('edges')

            const viewport = this.updateConfiguration(this.previousConfig, this.props.config);
            let deferredNodes = [], decorations = [], ghostAnimations = [];
            let nodeChanges, edgeChanges;
            cy.batch(() => {
                nodeChanges = this.makeChanges(oldNodes, newNodes, deferredNodes, decorations, ghostAnimations)
                edgeChanges = this.makeChanges(oldEdges, newEdges, null, null);
            })
            deferredNodes.forEach(n => n());
            cy.batch(() => decorations.forEach(n => n()));

            cy.autounselectify(disableSelection)

            this.adjustViewport(cy, viewport, shouldFit())
            if ((initialProductDisplay && !hasPreview) || nodeChanges || edgeChanges) {
                this.updatePreview();
            }
            ghostAnimations.forEach(a => a());

            function shouldFit() {
                const hasViewport = viewport && (viewport.pan || viewport.zoom)
                if (initialProductDisplay && !hasViewport) return true;
                const wasEmpty = oldNodes.length === 0;
                const hasNodes = newNodes.length
                const positionIsEmpty = node => node.position.x === 0 && node.position.y === 0;

                if (wasEmpty && hasNodes) {
                    return _.every(newNodes, positionIsEmpty);
                }

                return false;
            }
        },

        adjustViewport(cy, viewport, fit) {
            const { pan, zoom } = viewport || {};
            if (pan || zoom) {
                var newViewport = { zoom, pan: {...pan} };
                if (this.props.animate) {
                    this.panDisabled = this.zoomDisabled = true;
                    cy.stop().animate(newViewport, {
                        ...ANIMATION,
                        queue: false,
                        complete: () => {
                            this.panDisabled = this.zoomDisabled = false;
                            if (fit) {
                                this.fit(null, { animate: false });
                            }
                        }
                    })
                    return true
                } else {
                    this.disableEvent('pan zoom', () => cy.viewport(newViewport));
                }
            } else if (fit) {
                this.fit(null, { animate: false });
            }
        },

        _updatePreview() {
            if (this.unmounted) return;
            const { cy } = this.state;
            this.props.onUpdatePreview(cy.png(DEFAULT_PNG));
        },

        prepareConfig() {
            const defaults = {
                container: this.refs.cytoscape,
                boxSelectionEnabled: true,
                ready: (event) => {
                    var { cy } = event,
                        eventMap = _.mapObject(EVENTS, (name, key) => (e) => {
                            if (this[key + 'Disabled'] !== true) {
                                this.props[name](e)
                            }
                        });
                    cy.on(eventMap)
                    cy.on('cxttap', (event) => {
                        const {cyTarget, cy} = event;
                        if (cy === cyTarget) {
                            this.setState({ showGraphMenu: event })
                        }
                    })
                    cy.on('tap', (event) => {
                        const {cyTarget, cy} = event;
                        if (cy === cyTarget && event.originalEvent.ctrlKey) {
                            this.setState({ showGraphMenu: event })
                        }
                    })
                    cy._private.originalMinZoom = cy._private.minZoom;
                    cy._private.originalMaxZoom = cy._private.maxZoom;
                    this.props.onReady(event)
                }
            }
            var { config } = this.props;
            if (config) {
                return { ...defaults, ...config }
            }
            return defaults;
        },

        render() {
            const { showGraphMenu } = this.state;
            const { editable } = this.props;

            const menu = showGraphMenu ? (
                <Menu event={showGraphMenu}
                    editable={editable}
                    onEvent={this.onMenu}
                    cy={this.state.cy}/>
            ) : null;

            return (
                <div onMouseDown={this.onMouseDown} style={{height: '100%'}}>
                    <div style={{height: '100%'}} ref="cytoscape"></div>
                    {this.state.cy ? (
                        <NavigationControls
                            rightOffset={this.props.panelPadding.right}
                            tools={this.injectToolProps()}
                            onFit={this.onControlsFit}
                            onZoom={this.onControlsZoom}
                            onPan={this.onControlsPan} />
                    ) : null}
                    {menu}
                </div>
            )
        },

        updateDecorationPositions(cyNode, options = {}) {
            const { animate = false, toPosition } = options;

            if (cyNode.isChild() && !cyNode.hasClass('decoration')) {
                const decorations = cyNode.siblings().filter('.decoration');
                if (decorations && decorations.length) {
                    const specs = specsForNode(cyNode, toPosition);
                    if (animate) {
                        decorations.each(function() {
                            const position = calculatePosition(this.data('padding'), this.data('alignment'), this, specs);
                            this.stop().animate({ position }, { ...ANIMATION });
                        })
                    } else {
                        this.state.cy.batch(function() {
                            decorations.each(function() {
                                this.position(calculatePosition(this.data('padding'), this.data('alignment'), this, specs));
                            })
                        })
                    }
                }
            }
        },

        onMouseDown(event) {
            if (this.state.showGraphMenu) {
                this.setState({ showGraphMenu: false })
            }
        },

        _zoom(factor, dt) {
            const { cy } = this.state;

            var zoom = cy._private.zoom,
                [,, width, height] = cy.renderer().containerBB,
                pos = cy.renderer().projectIntoViewport(width / 2, height / 2);

            cy.zoom({
                level: zoom + factor * dt,
                position: { x: pos[0], y: pos[1] }
            })
        },

        onMenu(event) {
            this.setState({ showGraphMenu: false })

            const dataset = event.target.dataset;
            const args = (dataset.args ? JSON.parse(dataset.args) : []).concat([event])
            const fnName = `onMenu${dataset.func}`;
            if (fnName in this) {
                this[fnName](...args);
            } else if (fnName in this.props) {
                this.props[fnName](...args);
            } else {
                console.warn('No handler for menu item', fnName, args)
            }
        },

        onMenuZoom(level) {
            const { cy } = this.state;
            const zoom1 = cy._private.zoom;
            const zoom2 = level;
            const pan1 = cy._private.pan;
            const bb = cy.renderer().containerBB;
            const pos = { x: bb.width / 2, y: bb.height / 2 }
            const pan2 = {
                x: -zoom2 / zoom1 * (pos.x - pan1.x) + pos.x,
                y: -zoom2 / zoom1 * (pos.y - pan1.y) + pos.y
            };

            cy.animate({ zoom: zoom2, pan: pan2 }, { ...ANIMATION, queue: false });
        },

        onMenuFitToWindow() {
            this.fit();
        },

        onMenuSelect(select) {
            const { cy } = this.state;

            var nodes = cy.nodes();
            var edges = cy.edges();
            var selectedVertices = nodes.filter(':selected');
            var unselectedVertices = nodes.filter(':unselected');
            var selectedEdges = edges.filter(':selected');
            var unselectedEdges = edges.filter(':unselected');

            switch (select) {
                case 'all':
                    unselectedEdges.select();
                    unselectedVertices.select();
                    break;
                case 'none':
                    selectedVertices.unselect();
                    selectedEdges.unselect();
                    break;
                case 'invert':
                    selectedVertices.unselect();
                    selectedEdges.unselect();
                    unselectedVertices.select();
                    unselectedEdges.select();
                    break;
                case 'vertices':
                    selectedEdges.unselect();
                    unselectedVertices.select();
                    break;
                case 'edges':
                    selectedVertices.unselect();
                    unselectedEdges.select();
                    break;
                default:
                    this.props.onMenuSelect(select);
            }
        },

        onMenuLayout(layout, options) {
            const { cy } = this.state;
            const onlySelected = options && options.onlySelected;
            const elements = onlySelected ? cy.collection(cy.nodes().filter(':selected')) : cy.nodes();
            const defaultOptions = {...LAYOUT_OPTIONS[layout] };

            var opts = {
                name: layout,
                fit: false,
                stop: () => {
                    this.layoutDone = true;
                    if (!onlySelected) {
                        this.fit();
                    }
                },
                ..._.each(defaultOptions || {}, function(optionValue, optionName) {
                    if (_.isFunction(optionValue)) {
                        defaultOptions[optionName] = optionValue(elements, options);
                    }
                }),
                ...options
            };

            const ids = _.map(elements, node => node.id())
            this.layoutDone = false;
            this.moving = _.indexBy([...Object.keys(this.moving), ...ids]);

            if (onlySelected) {
                elements.layout(opts);
            } else {
                cy.layout(opts);
            }

        },

        onControlsZoom(dir) {
            const timeStamp = new Date().getTime();
            var dt = timeStamp - (this.lastTimeStamp || 0);
            var zoomFactor = this.zoomFactor || 0;

            if (dt < 30) {
                dt /= 1000;
                zoomFactor += zoomAcceleration * dt * zoomDamping;
            } else {
                dt = 1;
                zoomFactor = 0.01;
            }
            this.zoomFactor = zoomFactor;
            this.lastTimeStamp = timeStamp;
            this._zoom(zoomFactor * (dir === 'out' ? -1 : 1), dt);
        },

        onControlsPan(pan, options) {
            this.state.cy.panBy(pan);
        },

        onControlsFit() {
            this.fit();
        },

        fit(nodes, options = {}) {
            const { animate = true } = options;
            const { cy } = this.state;
            const cyNodes = nodes || cy.nodes('node.v,node.partial,.decoration');

            if (cyNodes.size() === 0) {
                cy.reset();
            } else {
                var bb = cyNodes.boundingBox({ includeLabels: false, includeNodes: true, includeEdges: false }),
                    style = cy.style(),
                    { left, right, top, bottom } = this.props.panelPadding,
                    w = parseFloat(style.containerCss('width')),
                    h = parseFloat(style.containerCss('height')),
                    zoom;

                left += PanelPaddingBorder;
                right += PanelPaddingBorder;
                top += PanelPaddingBorder;
                bottom += PanelPaddingBorder;

                if (!isNaN(w) && !isNaN(h)) {
                    zoom = Math.min(1, Math.min(
                        (w - (left + right)) / bb.w,
                        (h - (top + bottom)) / bb.h
                    ));

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
                            x: (w + left - right - zoom * (bb.x1 + bb.x2)) / 2,
                            y: (h + top - bottom - zoom * (bb.y1 + bb.y2)) / 2
                        },
                        _p = cy._private;

                    if (animate) {
                        return new Promise(function(f) {
                            cy.animate({
                                zoom: zoom,
                                pan: position
                            }, {
                                ...ANIMATION,
                                queue: false,
                                complete: () => {
                                    f();
                                }
                            });
                        })
                    } else {
                        _p.zoom = zoom;
                        _p.pan = position;
                        cy.trigger('pan zoom viewport');
                        cy.notify({ type: 'viewport' });
                    }
                }
            }
        },

        disableEvent(name, fn) {
            var names = name.split(/\s+/);
            names.forEach(name => (this[name + 'Disabled'] = true))
            fn.apply(this)
            names.forEach(name => (this[name + 'Disabled'] = false))
        },

        updateConfiguration(previous, nextConfig) {
            const { cy } = this.state;
            var retViewport = null;

            if (previous) {
                let { style, pan, zoom, ...other } = nextConfig
                _.each(other, (val, key) => {
                    if (!(key in previous) || previous[key] !== val) {
                        if (_.isFunction(cy[key])) {
                            cy[key](val)
                        } else console.warn('Unknown configuration key', key, val)
                    }
                })

                if (!_.isEqual(previous.style, style)) {
                    cy.style()
                        .resetToDefault()
                        .fromJson(style)
                        .update();
                }

                retViewport = { pan, zoom }
            }

            this.previousConfig = nextConfig
            return retViewport;
        },

        makeChanges(older, newer, reparenting, decorations, ghostAnimations) {
            const cy = this.state.cy
            const add = [];
            const remove = [...older];
            const modify = [];
            const oldById = _.indexBy(older, o => o.data.id);

            newer.forEach(item => {
                var id = item.data.id;
                var existing = oldById[id];
                if (existing) {
                    modify.push({ item, diffs: jsonpatch.compare(existing, item) })
                    var index = _.findIndex(remove, i => i.data.id === id);
                    if (index >= 0) remove.splice(index, 1)
                } else {
                    add.push(item)
                }
            })

            let modifiedPosition = false;
            modify.forEach(({ item, diffs }) => {
                const topLevelChanges = _.indexBy(diffs.filter(diff => diff.op !== 'remove'), d => d.path.replace(/^\/([^\/]+).*$/, '$1'))
                Object.keys(topLevelChanges).forEach(change => {
                    const cyNode = cy.getElementById(item.data.id);

                    switch (change) {
                        case 'data':
                            this.disableEvent('data', () => {
                                if (item.data.parent !== cyNode.data('parent')) {
                                    cyNode.removeData().data(_.omit(item.data, 'parent'));
                                    reparenting.push(() => cyNode.move({ parent: item.data.parent }));
                                } else {
                                    cyNode.removeData().data(item.data)
                                }

                                if (decorations) {
                                    decorations.push(() => {
                                        this.updateDecorationPositions(cyNode);
                                    })
                                }
                            })
                            break;

                        case 'grabbable': cyNode[item.grabbable ? 'grabify' : 'ungrabify'](); break;
                        case 'selectable': cyNode[item.selectable ? 'selectify' : 'unselectify'](); break;
                        case 'locked': cyNode[item.locked ? 'lock' : 'unlock'](); break;

                        case 'selected':
                            if (cyNode.selected() !== item.selected) {
                                this.disableEvent('select unselect', () => cyNode[item.selected ? 'select' : 'unselect']());
                            }
                            break;

                        case 'classes':
                            if (item.classes) {
                                cyNode.classes(item.classes)
                            } else if (!_.isEmpty(cyNode._private.classes)) {
                                cyNode.classes();
                            }
                            break;

                        case 'position':
                            if (!cyNode.grabbed() && !(cyNode.id() in this.moving)) {
                                if (!item.data.alignment && !item.data.animateTo) {
                                    const positionChangedWithinTolerance = _.some(cyNode.position(), (oldV, key) => {
                                        const newV = item.position[key];
                                        return (Math.max(newV, oldV) - Math.min(newV, oldV)) >= 1
                                    });

                                    if (positionChangedWithinTolerance) {
                                        modifiedPosition = true;
                                        if (this.props.animate) {
                                            this.positionDisabled = true;
                                            this.updateDecorationPositions(cyNode, { toPosition: item.position, animate: true });
                                            cyNode.stop().animate({ position: item.position }, { ...ANIMATION, complete: () => {
                                                this.positionDisabled = false;
                                            }})
                                        } else {
                                            this.disableEvent('position', () => {
                                                cyNode.position(item.position)
                                                this.updateDecorationPositions(cyNode);
                                            })
                                        }
                                    }
                                }
                            } else if (this.layoutDone) {
                                delete this.moving[cyNode.id()];
                            }
                            break;

                        case 'renderedPosition':
                            if (!topLevelChanges.position && change) {
                                modifiedPosition = true;
                                this.disableEvent('position', () => cyNode.renderedPosition(item.renderedPosition))
                            }
                            break;

                        default:
                            throw new Error('Change not handled: ' + change)
                    }
                })
            })
            add.forEach(item => {
                var { data } = item;

                if (isNode(data)) {
                    if (data.alignment) {
                        const dec = cy.add({
                            ...item,
                            group: 'nodes'
                        })
                        decorations.push(() => {
                            const specs = specsForNode(cy.getElementById(data.vertex.id));
                            dec.position(calculatePosition(data.padding, data.alignment, dec, specs));
                        })
                    } else {
                        var cyNode = cy.add({ ...item, group: 'nodes' })[0]
                        if (cyNode && data.animateTo) {
                            const animation = cyNode._private.animation;
                            var beginAnimation = true;
                            if (animation && animation.current.length === 1) {
                                const toPos = animation.current[0]._private.position;
                                if (toPos.x === data.animateTo.pos.x && toPos.y === data.animateTo.pos.y) {
                                    beginAnimation = false;
                                }
                            }
                            if (beginAnimation) {
                                if (data.animateTo.pos.x !== item.position.x && data.animateTo.pos.y !== item.position.y) {
                                    ghostAnimations.push(() => {
                                        cyNode.stop(true)
                                        _.delay(() => {
                                            cyNode.animate({ position: data.animateTo.pos }, {
                                                ...ANIMATION_SLOW,
                                                complete: () => {
                                                    this.props.onGhostFinished(data.animateTo.id)
                                                }
                                            })
                                        }, 100)
                                    })
                                }
                            }
                        }
                    }
                } else if (isEdge(data)) {
                    cy.add({ ...item, group: 'edges' })
                }
            })
            remove.forEach(item => {
                const cyNode = cy.getElementById(item.data.id);
                if (cyNode.length && !cyNode.removed()) {
                    cy.remove(cyNode)
                }
            })
            return add.length || remove.length || modifiedPosition;
        },

        drawPaths(newData) {
            const { drawPaths } = this.props;

            if (drawPaths) {
                const { paths, sourceId, targetId } = drawPaths;
                const nodesById = _.indexBy(newData.elements.nodes, n => n.data.id);
                const keyGen = (src, target) => [src, target].sort().join('');
                const edgesById = _.groupBy(newData.elements.edges, e => keyGen(e.data.source, e.data.target));
                paths.forEach((path, i) => {
                    const vertexIds = path.filter(v => v !== sourceId && v !== targetId);
                    const end = colorjs('#0088cc').shiftHue(i * (360 / paths.length)).toCSSHex();
                    const existingOrNewEdgeBetween = (node1, node2, count) => {
                        var edges = edgesById[keyGen(node1, node2)];
                        if (edges) {
                            edges.forEach(e => {
                                e.classes = (e.classes ? e.classes + ' ' : '') + 'path-edge';
                                e.data.pathColor = end
                            });
                        } else {
                            newData.elements.edges.push({
                                group: 'edges',
                                classes: 'path-edge path-temp' + (count ? ' path-hidden-verts' : ''),
                                id: node1 + '-' + node2 + 'path=' + i,
                                data: {
                                    source: node1,
                                    target: node2,
                                    pathColor: end,
                                    label: count === 0 ? '' :
                                        i18n('graph.path.edge.label.' + (
                                            count === 1 ? 'one' : 'some'
                                        ), F.number.pretty(count))
                                }
                            });
                        }
                    };

                    var count = 0;
                    var lastNode = sourceId;

                    vertexIds.forEach(vertexId => {
                        if (vertexId in nodesById) {
                            existingOrNewEdgeBetween(lastNode, vertexId, count);
                            lastNode = vertexId;
                            count = 0;
                        } else count++;
                    });

                    existingOrNewEdgeBetween(lastNode, targetId, count);
                });
            }
        },

        drawEdgeToMouseFrom(newData) {
            const { drawEdgeToMouseFrom } = this.props;
            const { cy } = this.state;

            if (drawEdgeToMouseFrom) {
                const { left, top } = this.clientRect;
                const { vertexId, toVertexId } = drawEdgeToMouseFrom;
                const position = toVertexId ?
                    { position: cy.getElementById(toVertexId).position() } :
                    {
                        renderedPosition: {
                            x: window.lastMousePositionX - left,
                            y: window.lastMousePositionY - top
                        }
                    };
                const nodeIndex = newData.elements.nodes.findIndex((node) => node.data.id === DrawEdgeNodeId);

                if (nodeIndex > -1) {
                    newData.elements.nodes[nodeIndex] = {
                        ...newData.elements.nodes[nodeIndex],
                        ...position
                    };
                } else {
                    newData.elements.nodes.push({
                        data: { id: DrawEdgeNodeId },
                        classes: 'drawEdgeToMouse',
                        ...position
                    })
                    newData.elements.edges.push({
                        data: {
                            source: vertexId,
                            target: DrawEdgeNodeId,
                        },
                        classes: 'drawEdgeToMouse'
                    })
                }
            }
        },

        injectToolProps() {
            const { cy } = this.state;
            if (cy) {
                return this.props.tools.map(tool => ({
                    ...tool,
                    props: { cy }
                }))
            }
            return [];
        },

        drawControlDragSelection(newData) {
            const { controlDragSelection } = this.state;
            const select = (node) => {
                if (node.data.id === controlDragSelection) {
                    node.classes += ' controlDragSelection';
                } else {
                    node.classes = node.classes.replace(/controlDragSelection/g, '');
                }
            };

            newData.elements.nodes.forEach((node) => select(node));
        }
    })

    return Cytoscape;

    function calculatePosition(padding, alignment, decorationNode, specs) {
        const paddingX = padding && ('x' in padding) ? padding.x : 8;
        const paddingY = padding && ('y' in padding) ? padding.y : 8;

        if (alignment) {
            var x, y,
                decBBox = decorationNode ?
                    decorationNode.boundingBox({ includeLabels: false }) :
                    { w: 0, h: 0 },
                decBBoxLabels = decorationNode ?
                    decorationNode.boundingBox({ includeLabels: true }) :
                    { w: 0, h: 0 };

            switch (alignment.h) {
              case 'center':
                x = specs.position.x;
                break;

              case 'right':
                if (specs.textVAlign === alignment.v && specs.textVAlign === 'center' && specs.textHAlign === alignment.h) {
                    x = specs.position.x - specs.w / 2 + specs.bboxLabels.w + decBBoxLabels.w / 2
                } else if (specs.textVAlign === alignment.v &&
                          (specs.textVAlign !== 'center' || specs.textHAlign === alignment.h || specs.textHAlign === 'center')) {
                    x = specs.position.x + specs.bboxLabels.w / 2 + decBBoxLabels.w / 2
                } else {
                    x = specs.position.x + specs.w / 2 + decBBox.w / 2;
                }
                x += paddingX
                break;

              case 'left':
              default:
                if (specs.textVAlign === alignment.v && specs.textVAlign === 'center' && specs.textHAlign === alignment.h) {
                    x = specs.position.x + specs.w / 2 - specs.bboxLabels.w - decBBoxLabels.w / 2
                } else if (specs.textVAlign === alignment.v &&
                    (specs.textVAlign !== 'center' || specs.textHAlign === alignment.h || specs.textHAlign === 'center')) {
                    x = specs.position.x - specs.bboxLabels.w / 2 - decBBoxLabels.w / 2
                } else {
                    x = specs.position.x - specs.w / 2 - decBBox.w / 2;
                }
                x -= paddingX
            }
            switch (alignment.v) {
              case 'center':
                y = specs.position.y;
                break;

              case 'bottom':
                if (specs.textVAlign === alignment.v && alignment.h === 'center') {
                    y = specs.position.y - specs.h / 2 + specs.bboxLabels.h + decBBoxLabels.h / 2 + paddingY;
                } else if (specs.textVAlign === alignment.v) {
                    y = specs.position.y + specs.h / 2 + (specs.bboxLabels.h - specs.h) / 2;
                } else {
                    y = specs.position.y + specs.h / 2 + decBBoxLabels.h / 2 + paddingY;
                }
                break;

              case 'top':
              default:
                if (specs.textVAlign === alignment.v && alignment.h === 'center') {
                    y = specs.position.y + specs.h / 2 - specs.bboxLabels.h - decBBoxLabels.h / 2 - paddingY;
                } else if (specs.textVAlign === alignment.v) {
                    y = specs.position.y - specs.h / 2 - (specs.bboxLabels.h - specs.h) / 2;
                } else {
                    y = specs.position.y - specs.h / 2 - decBBoxLabels.h / 2 - paddingY
                }
            }

            return { x: x, y: y };
        }
        throw new Error('Alignment required', alignment);
    }

    function specsForNode(node, position) {
        return {
            position: position || node.position(),
            textHAlign: node.style('text-halign'),
            textVAlign: node.style('text-valign'),
            h: node.height(),
            w: node.width(),
            bbox: node.boundingBox({includeNodes: true, includeLabels: false}),
            bboxLabels: node.boundingBox({includeNodes: true, includeLabels: true})
        }
    }
})


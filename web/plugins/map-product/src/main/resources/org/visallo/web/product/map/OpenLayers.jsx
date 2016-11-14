define([
    'react',
    'openlayers',
    'fast-json-patch',
    './multiPointCluster',
    'components/NavigationControls'
], function(
    React,
    ol,
    jsonpatch,
    MultiPointCluster,
    NavigationControls) {

    const noop = function() {};

    const FEATURE_HEIGHT = 40,
        FEATURE_CLUSTER_HEIGHT = 24,
        ANIMATION_DURATION = 200,
        MIN_FIT_ZOOM_RESOLUTION = 3000,
        MAX_FIT_ZOOM_RESOLUTION = 20000,
        PREVIEW_WIDTH = 300,
        PREVIEW_HEIGHT = 300,
        PREVIEW_DEBOUNCE_SECONDS = 2;

    const { PropTypes } = React;

    const OpenLayers = React.createClass({
        propTypes: {
            source: PropTypes.string.isRequired,
            sourceOptions: PropTypes.object,
            generatePreview: PropTypes.bool,
            onSelectElements: PropTypes.func.isRequired,
            onUpdatePreview: PropTypes.func.isRequired,
            onTap: PropTypes.func,
            onContextTap: PropTypes.func,

            onZoom: PropTypes.func,
            onPan: PropTypes.func
        },

        getInitialState() {
            return { panning: false }
        },

        getDefaultProps() {
            return {
                generatePreview: false,
                onTap: noop,
                onContextTap: noop,
                onZoom: noop,
                onPan: noop
            }
        },

        componentDidUpdate() {
            if (this.state.cluster) {
                const existingFeatures = _.indexBy(this.state.cluster.source.getFeatures(), f => f.getId());
                const newFeatures = [];
                const { source } = this.state.cluster;
                var changed = false;
                this.props.features.forEach(data => {
                    const { id, geoLocations, element, ...rest } = data;
                    const featureValues = {
                        ...rest,
                        element,
                        geoLocations,
                        geometry: new ol.geom.MultiPoint(geoLocations.map(function(geo) {
                            return ol.proj.fromLonLat(geo);
                        }))
                    };

                    if (id in existingFeatures) {
                        const existingFeature = existingFeatures[id];
                        const existingValues = _.omit(data, 'geometry', 'element')
                        const newValues = _.omit(featureValues, 'geometry', 'element')
                        if (!_.isEqual(existingValues, newValues)) {
                            changed = true
                            existingFeature.setProperties(featureValues)
                        }
                        delete existingFeatures[id];
                    } else {
                        var feature = new ol.Feature(featureValues);
                        feature.setId(data.id);
                        newFeatures.push(feature);
                    }
                })

                if (this.props.viewport && !_.isEmpty(this.props.viewport)) {
                    this.state.map.getView().setCenter(this.props.viewport.pan);
                    this.state.map.getView().setResolution(this.props.viewport.zoom);
                }

                if (newFeatures.length) {
                    changed = true
                    source.addFeatures(newFeatures);
                }
                if (!_.isEmpty(existingFeatures)) {
                    changed = true
                    _.forEach(existingFeatures, feature => source.removeFeature(feature));
                }

                if (this.props.generatePreview) {
                    this._updatePreview({ fit: !this.props.viewport });
                } else if (changed) {
                    this.updatePreview();
                }
            }
        },

        _updatePreview(options = {}) {
            const { fit = false } = options;
            const { map, baseLayerSource } = this.state;
            const doFit = () => {
                if (fit) this.fit({ animate: false });
            };

            // Since this is delayed, make sure component not unmounted
            if (!this._canvasPreviewBuffer) return;

            doFit();
            map.once('postcompose', (event) => {
                if (!this._canvasPreviewBuffer) return;
                var loading = 0, loaded = 0, events, captureTimer;

                doFit();

                const mapCanvas = event.context.canvas;
                const capture = _.debounce(() => {
                    if (!this._canvasPreviewBuffer) return;

                    doFit();

                    map.once('postrender', () => {
                        const mapData = mapCanvas.toDataURL('image/png');
                        const ctx = this._canvasPreviewBuffer.getContext('2d');
                        const image = new Image();

                        if (events) {
                            events.forEach(key => ol.Observable.unByKey(key));
                        }

                        image.onload = () => {
                            if (!this._canvasPreviewBuffer) return;

                            var hRatio = PREVIEW_WIDTH / image.width;
                            var vRatio = PREVIEW_HEIGHT / image.height;
                            var ratio = Math.min(hRatio, vRatio);
                            this._canvasPreviewBuffer.width = Math.trunc(image.width * ratio);
                            this._canvasPreviewBuffer.height = Math.trunc(image.height * ratio);
                            ctx.drawImage(
                                image, 0, 0, image.width, image.height,
                                0, 0, this._canvasPreviewBuffer.width, this._canvasPreviewBuffer.height
                            );
                            this.props.onUpdatePreview(this._canvasPreviewBuffer.toDataURL('image/png'));
                        };
                        image.onerror = (e) => console.error(e)
                        image.src = mapData;
                    });
                    map.renderSync();
                }, 100)

                const tileLoadStart = () => {
                    clearTimeout(captureTimer);
                    ++loading;
                };
                const tileLoadEnd = (event) => {
                    clearTimeout(captureTimer);
                    if (loading === ++loaded) {
                        captureTimer = capture();
                    }
                };

                events = [
                    baseLayerSource.on('tileloadstart', tileLoadStart),
                    baseLayerSource.on('tileloadend', tileLoadEnd),
                    baseLayerSource.on('tileloaderror', tileLoadEnd)
                ];
            });
            map.renderSync();
        },

        componentDidMount() {
            this._canvasPreviewBuffer = document.createElement('canvas');
            this._canvasPreviewBuffer.width = PREVIEW_WIDTH;
            this._canvasPreviewBuffer.height = PREVIEW_HEIGHT;

            this.olEvents = [];
            this.domEvents = [];
            this.updatePreview = _.debounce(this._updatePreview, PREVIEW_DEBOUNCE_SECONDS * 1000);
            const { map, cluster, baseLayerSource } = this.configureMap();
            this.setState({ map, cluster, baseLayerSource })
        },

        componentWillUnmount() {
            this._canvasPreviewBuffer = null;
            if (this.state.cluster) {
                this.olEvents.forEach(key => ol.Observable.unByKey(key));
                this.olEvents = null;

                this.domEvents.forEach(fn => fn());
                this.domEvents = null;
            }
        },

        render() {
            // Cover the map when panning/dragging to avoid sending events there
            const moveWrapper = this.state.panning ? (<div className="draggable-wrapper"/>) : '';
            return (
                <div style={{height: '100%'}}>
                    <div style={{height: '100%'}} ref="map"></div>
                    <NavigationControls
                        rightOffset={this.props.panelPadding.right}
                        onFit={this.onControlsFit}
                        onZoom={this.onControlsZoom}
                        onPan={this.onControlsPan} />
                    {moveWrapper}
                </div>
            )
        },

        onControlsFit() {
            this.fit();
        },

        onControlsZoom(type) {
            const { map } = this.state;
            const view = map.getView();

            if (!this._slowZoomIn) {
                this._slowZoomIn = _.throttle(zoomByDelta(1), ANIMATION_DURATION, {trailing: false});
                this._slowZoomOut = _.throttle(zoomByDelta(-1), ANIMATION_DURATION, {trailing: false});
            }

            if (type === 'in') {
                this._slowZoomIn();
            } else {
                this._slowZoomOut();
            }

            function zoomByDelta(delta) {
                return () => {
                    var currentResolution = view.getResolution();
                    if (currentResolution) {
                        map.beforeRender(ol.animation.zoom({
                            resolution: currentResolution,
                            duration: ANIMATION_DURATION
                        }));
                        const newResolution = view.constrainResolution(currentResolution, delta);
                        view.setResolution(newResolution);
                    }
                }
            }
        },

        onControlsPan({ x, y }, { state }) {
            if (state === 'panningStart') {
                this.setState({ panning: true })
            } else if (state === 'panningEnd') {
                this.setState({ panning: false })
            } else {
                const { map } = this.state;
                const view = map.getView();

                var currentCenter = view.getCenter(),
                    resolution = view.getResolution(),
                    center = view.constrainCenter([
                        currentCenter[0] - x * resolution,
                        currentCenter[1] + y * resolution
                    ]);

                view.setCenter(center);
            }
        },

        fit(options = {}) {
            const { animate = true } = options;
            const { map, cluster } = this.state;
            const extent = cluster.source.getExtent();
            const view = map.getView();

            if (!ol.extent.isEmpty(extent)) {
                var resolution = view.getResolution(),
                    extentWithPadding = extent,
                    { left, right, top, bottom } = this.props.panelPadding,
                    clientBox = this.refs.map.getBoundingClientRect(),
                    viewportWidth = clientBox.width - left - right - 20 * 2,
                    viewportHeight = clientBox.height - top - bottom - 20 * 2,
                    extentWithPaddingSize = ol.extent.getSize(extentWithPadding),

                    // Figure out ideal resolution based on available realestate
                    idealResolution = Math.max(
                        extentWithPaddingSize[0] / viewportWidth,
                        extentWithPaddingSize[1] / viewportHeight
                    );

                if (animate) {
                    map.beforeRender(ol.animation.pan({
                        source: view.getCenter(),
                        duration: ANIMATION_DURATION
                    }))
                    map.beforeRender(ol.animation.zoom({
                        resolution: resolution,
                        duration: ANIMATION_DURATION
                    }));
                }

                view.setResolution(view.constrainResolution(
                    Math.min(MAX_FIT_ZOOM_RESOLUTION, Math.max(idealResolution, MIN_FIT_ZOOM_RESOLUTION)), -1
                ));

                var center = ol.extent.getCenter(extentWithPadding),
                    offsetX = left - right,
                    offsetY = top - bottom,
                    lon = offsetX * view.getResolution() / 2,
                    lat = offsetY * view.getResolution() / 2;

                view.setCenter([center[0] - lon, center[1] - lat]);
            } else {
                if (animate) {
                    map.beforeRender(ol.animation.zoom({
                        resolution: view.getResolution(),
                        duration: ANIMATION_DURATION
                    }));
                    map.beforeRender(ol.animation.pan({
                        source: view.getCenter(),
                        duration: ANIMATION_DURATION
                    }))
                }
                var params = this.getDefaultViewParameters();
                view.setZoom(params.zoom);
                view.setCenter(params.center);
            }
        },

        getDefaultViewParameters() {
            return {
                zoom: 2,
                center: [0, 0]
            };
        },

        configureMap() {
            const { source, sourceOptions = {} } = this.props;
            const cluster = this.configureCluster()
            const map = new ol.Map({
                loadTilesWhileInteracting: true,
                keyboardEventTarget: document,
                controls: [],
                layers: [],
                target: this.refs.map
            });

            this.configureEvents({ map, cluster });

            var baseLayerSource;

            sourceOptions.crossOrigin = 'Anonymous';

            if (source in ol.source && _.isFunction(ol.source[source])) {
                baseLayerSource = new ol.source[source](sourceOptions)
            } else {
                console.error('Unknown map provider type: ', source);
                throw new Error('map.provider is invalid')
            }

            map.addLayer(new ol.layer.Tile({ source: baseLayerSource }));
            map.addLayer(cluster.layer)

            const view = new ol.View(this.getDefaultViewParameters());
            this.olEvents.push(view.on('change:center', (event) => this.props.onPan(event)));
            this.olEvents.push(view.on('change:resolution', (event) => this.props.onZoom(event)));

            map.setView(view);

            return { map, cluster, baseLayerSource }
        },

        configureCluster() {
            const source = new ol.source.Vector({ features: [] });
            const clusterSource = new MultiPointCluster({
                distance: Math.max(FEATURE_CLUSTER_HEIGHT, FEATURE_HEIGHT),
                source
            });
            const layer = new ol.layer.Vector({
                id: 'elementsLayer',
                style: cluster => this.clusterStyle(cluster),
                source: clusterSource
            });

            return { source, clusterSource, layer }
        },

        clusterStyle() {
            if (!this._clusterStyleWithCache) {
                this._clusterStyleWithCache = _.memoize(
                    this._clusterStyle,
                    function clusterStateHash(cluster, options = { selected: false }) {
                        var count = cluster.get('count'),
                            selectionState = cluster.get('selectionState') || 'none',
                            key = [count, selectionState, JSON.stringify(options)];

                        if (count === 1) {
                            const feature = cluster.get('features')[0];
                            const compare = _.omit(feature.getProperties(), 'geoLocations', 'geometry', 'element');
                            key.push(JSON.stringify(compare, null, 0))
                        }
                        return key.join('')
                    }
                );
            }

            return this._clusterStyleWithCache.apply(this, arguments);
        },

        _clusterStyle(cluster, options = { selected: false }) {
            var count = cluster.get('count'),
                selectionState = cluster.get('selectionState') || 'none',
                selected = options.selected || selectionState !== 'none';

            if (count === 1) {
                const feature = cluster.get('features')[0];
                const isSelected = options.selected || feature.get('selected');

                return [new ol.style.Style({
                    image: new ol.style.Icon({
                        src: feature.get(isSelected ? 'iconUrlSelected' : 'iconUrl'),
                        imgSize: feature.get('iconSize'),
                        scale: 1 / feature.get('pixelRatio'),
                        anchor: feature.get('iconAnchor')
                    })
                })]
            } else {
                var radius = Math.min(count || 0, FEATURE_CLUSTER_HEIGHT / 2) + 10,
                    unselectedFill = 'rgba(241,59,60, 0.8)',
                    unselectedStroke = '#AD2E2E',
                    stroke = selected ? '#08538B' : unselectedStroke,
                    strokeWidth = Math.round(radius * 0.1),
                    textStroke = stroke,
                    fill = selected ? 'rgba(0,112,195, 0.8)' : unselectedFill;

                if (selected && selectionState === 'some') {
                    fill = unselectedFill;
                    textStroke = unselectedStroke;
                    strokeWidth *= 2;
                }

                return [new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: radius,
                        stroke: new ol.style.Stroke({
                            color: stroke,
                            width: strokeWidth
                        }),
                        fill: new ol.style.Fill({
                            color: fill
                        })
                    }),
                    text: new ol.style.Text({
                        text: count.toString(),
                        font: `bold condensed ${radius}px sans-serif`,
                        textAlign: 'center',
                        fill: new ol.style.Fill({
                            color: '#fff',
                        }),
                        stroke: new ol.style.Stroke({
                            color: textStroke,
                            width: 2
                        })
                    })
                })];
            }
        },

        configureEvents({ map, cluster }) {
            var self = this;

            // Feature Selection
            const selectInteraction = new ol.interaction.Select({
                condition: ol.events.condition.click,
                layers: [cluster.layer],
                style: cluster => this.clusterStyle(cluster, { selected: true })
            });

            this.olEvents.push(selectInteraction.on('select', function(e) {
                var clusters = e.target.getFeatures().getArray(),
                    elements = { vertices: [], edges: [] };

                clusters.forEach(cluster => {
                    cluster.get('features').forEach(feature => {
                        const el = feature.get('element');
                        const key = el.type === 'vertex' ? 'vertices' : 'edges';
                        elements[key].push(el.id);
                    })
                })
                self.props.onSelectElements(elements);
            }));

            this.olEvents.push(map.on('click', function(event) {
                self.props.onTap(event);
            }));
            this.olEvents.push(map.on('pointerup', function(event) {
                const { pointerEvent } = event;
                if (pointerEvent && pointerEvent.button === 2) {
                    self.props.onContextTap(event);
                }
            }));

            this.olEvents.push(cluster.clusterSource.on(ol.events.EventType.CHANGE, _.debounce(function() {
                var selected = selectInteraction.getFeatures(),
                    clusters = this.getFeatures(),
                    newSelection = [],
                    isSelected = feature => feature.get('selected');

                clusters.forEach(cluster => {
                    var innerFeatures = cluster.get('features');
                    if (_.any(innerFeatures, isSelected)) {
                        newSelection.push(cluster);
                        if (_.all(innerFeatures, isSelected)) {
                            cluster.set('selectionState', 'all');
                        } else {
                            cluster.set('selectionState', 'some');
                        }
                    } else {
                        cluster.unset('selectionState');
                    }
                })

                selected.clear()
                if (newSelection.length) {
                    selected.extend(newSelection)
                }
            }, 100)));

            map.addInteraction(selectInteraction);

            const viewport = map.getViewport();
            this.domEvent(viewport, 'contextmenu', function(event) {
                event.preventDefault();
            })
            this.domEvent(viewport, 'mouseup', function(event) {
                event.preventDefault();
                if (event.button === 2 || event.ctrlKey) {
                    // TODO
                    //self.handleContextMenu(event);
                }
            });
            this.domEvent(viewport, 'mousemove', function(event) {
                const pixel = map.getEventPixel(event);
                const hit = map.forEachFeatureAtPixel(pixel, () => true);
                if (hit) {
                    map.getTarget().style.cursor = 'pointer';
                } else {
                    map.getTarget().style.cursor = '';
                }
            });
        },

        domEvent(el, type, handler) {
            this.domEvents.push(() => el.removeEventListener(type, handler));
            el.addEventListener(type, handler, false);
        }
    })

    return OpenLayers;
})


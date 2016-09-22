/*globals google:false*/
define([
    'flight/lib/component',
    'tpl!./template',
    'tpl!./instructions/regionCenter',
    'tpl!./instructions/regionRadius',
    'tpl!./instructions/regionLoading',
    'util/retina',
    'util/controls',
    'util/vertex/formatters',
    'util/withAsyncQueue',
    'util/withContextMenu',
    'util/withDataRequest',
    './featureMoveInteraction'
], function(defineComponent,
    template,
    centerTemplate,
    radiusTemplate,
    loadingTemplate,
    retina,
    Controls,
    F,
    withAsyncQueue,
    withContextMenu,
    withDataRequest,
    FeatureMoveInteraction) {
    'use strict';

    const MODE_NORMAL = 0,
        FEATURE_SIZE = [22, 40],
        FEATURE_HEIGHT = 40,
        FEATURE_CLUSTER_HEIGHT = 24,
        ANIMATION_DURATION = 200,
        MODE_REGION_SELECTION_MODE_POINT = 1,
        MODE_REGION_SELECTION_MODE_RADIUS = 2,
        MODE_REGION_SELECTION_MODE_LOADING = 3;

    return defineComponent(MapViewOpenLayers, withContextMenu, withAsyncQueue, withDataRequest);

    function MapViewOpenLayers() {

        var ol;

        this.mode = MODE_NORMAL;

        this.defaultAttrs({
            mapSelector: '.map-container',
            contextMenuSelector: '.contextmenu',
            contextMenuVertexSelector: '.contextmenuvertex',
            controlsSelector: '.controls'
        });

        this.before('teardown', function() {
            this.mapReady(function(map) {
                this.pinSourceVector.clear(true);
                map.dispose();
                map = null;
                this.mapUnload();
                this.select('mapSelector').empty().removeClass('olMap');
            })
        })

        this.after('initialize', function() {
            var self = this;

            this.initialized = false;
            this.setupAsyncQueue('map');
            this.$node.html(template({})).find('.shortcut').each(function() {
                var $this = $(this), command = $this.text();
                $this.text(F.string.shortcut($this.text()));
            });

            this.on(document, 'mapShow', this.onMapShow);
            this.on(document, 'mapCenter', this.onMapCenter);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'updateWorkspace', this.onUpdateWorkspace);
            this.on(document, 'verticesDropped', this.onVerticesDropped);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'verticesDeleted', this.onVerticesDeleted);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'searchResultsWithinRadius', this.onSearchResultsWithinRadius);
            this.on('registerForPositionChanges', this.onRegisterForPositionChanges);
            this.on('unregisterForPositionChanges', this.onUnregisterForPositionChanges);

            this.padding = {l: 0, r: 0, b: 0, t: 0};

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: i18n('map.help.scope'),
                shortcuts: {
                    '-': { fire: 'zoomOut', desc: i18n('map.help.zoom_out') },
                    '=': { fire: 'zoomIn', desc: i18n('map.help.zoom_in') },
                    'alt-f': { fire: 'fit', desc: i18n('map.help.fit') }
                }
            });

            this.attachToZoomPanControls();

        });

        this.onRegisterForPositionChanges = function(event, data) {
            var self = this,
                anchorTo = data && data.anchorTo;

            if (!anchorTo || !anchorTo.vertexId) {
                return console.error('Registering for position events requires a vertexId');
            }

            this.mapReady(function(map) {
                if (!self.viewportPositionChanges) {
                    self.viewportPositionChanges = [];
                    self.viewportPositionChangesMapKeys = map.on('moveend', self.onViewportChangesForPositionChanges, self);
                    self.viewportPositionChangesMapViewKeys = map.getView().on('change:center', self.onViewportChangesForPositionChanges, self);
                }

                self.viewportPositionChanges.push({
                    el: event.target,
                    fn: function(el) {
                        var vertexId = anchorTo.vertexId,
                            feature = self.pinSourceVector.getFeatureById(vertexId);

                        if (!feature) return;

                        var offset = self.$node.offset(),
                            position = map.getPixelFromCoordinate(feature.getGeometry().getCoordinates()[0]),
                            height = feature.get('count') > 1 ? FEATURE_CLUSTER_HEIGHT : FEATURE_HEIGHT,
                            eventData = {
                                anchor: anchorTo,
                                position: {
                                    x: position[0] + offset.left,
                                    y: position[1] + offset.top
                                },
                                positionIf: {
                                    above: {
                                        x: position[0] + offset.left,
                                        y: position[1] + offset.top - height
                                    },
                                    below: {
                                        x: position[0] + offset.left,
                                        y: position[1] + offset.top
                                    }
                                }
                            };

                        this.trigger(el, 'positionChanged', eventData);
                    }
                });
                self.onViewportChangesForPositionChanges();
            })
        };

        this.onViewportChangesForPositionChanges = function() {
            var self = this;

            if (this.viewportPositionChanges) {
                this.viewportPositionChanges.forEach(function(vpc) {
                    vpc.fn.call(self, vpc.el);
                })
            }
        };

        this.onUnregisterForPositionChanges = function(event, data) {
            var self = this;

            if (this.viewportPositionChanges) {
                var index = _.findIndex(this.viewportPositionChanges, function(vpc) {
                    return vpc.el === event.target;
                })
                if (index >= 0) {
                    this.viewportPositionChanges.splice(index, 1);
                }
                if (this.viewportPositionChanges.length === 0) {
                    this.viewportPositionChanges = null;
                }

                this.mapReady(function(map) {
                    map.unByKey(self.viewportPositionChangesMapKeys);
                    map.getView().unByKey(self.viewportPositionChangesMapViewKeys);
                })
            }
        };

        this.attachToZoomPanControls = function() {
            Controls.attachTo(this.select('controlsSelector'));

            this.mapReady(function(map) {
                var view = map.getView();

                // While panning add a div so map doesn't swallow mousemove
                // events
                this.on('startPan', function() {
                    this.$node.append('<div class="draggable-wrapper"/>');
                });
                this.on('endPan', function() {
                    this.$node.find('.draggable-wrapper').remove();
                });

                this.on('pan', function(e, data) {
                    e.stopPropagation();

                    var currentCenter = view.getCenter(),
                        resolution = view.getResolution(),
                        center = view.constrainCenter([
                            currentCenter[0] - data.pan.x * resolution,
                            currentCenter[1] + data.pan.y * resolution
                        ]);

                    view.setCenter(center);
                });
                this.on('fit', function(e) {
                    e.stopPropagation();
                    this.fit(map);
                });

                var getResolutionForValueFunction = view.getResolutionForValueFunction(),
                    getValueForResolutionFunction = view.getValueForResolutionFunction(),
                    slowZoomIn = _.throttle(zoomByDelta(1), ANIMATION_DURATION, {trailing: false}),
                    slowZoomOut = _.throttle(zoomByDelta(-1), ANIMATION_DURATION, {trailing: false});

                this.on('zoomIn', slowZoomIn)
                this.on('zoomOut', slowZoomOut);

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
            });
        };

        this.onMapShow = function() {
            if (!this.mapIsReady()) {
                this.initializeMap();
            }
        };

        this.onMapCenter = function(evt, data) {
            this.mapReady(function(map) {
                map.getView().setCenter(ol.proj.fromLonLat([data.longitude, data.latitude]));
            });
        };

        this.onGraphPaddingUpdated = function(event, data) {
            this.padding = $.extend({}, data.padding);
        };

        this.onUpdateWorkspace = function(event, data) {
            if (data && data.entityDeletes) {
                this.removeVertexIds(data.entityDeletes);
            }
        };

        this.onWorkspaceUpdated = function(event, data) {
            var self = this;

            this.mapReady(function(map) {
                this.removeVertexIds(data.entityDeletes);
                if (data.entityUpdates.length) {
                    this.dataRequest('vertex', 'store', { vertexIds: _.pluck(data.entityUpdates, 'vertexId') })
                        .done(function(vertices) {
                            if (vertices.length) {
                                self.updateOrAddVertices(vertices, { adding: true, preventShake: true });
                            }
                        });
                }
            });
        };

        this.onWorkspaceLoaded = function(evt, workspaceData) {
            var self = this;
            this.isWorkspaceEditable = workspaceData.editable;
            this.mapReady(function(map) {
                this.pinSourceVector.clear();
                this.updateOrAddVertices(workspaceData.data.vertices, {
                    adding: true,
                    preventShake: true
                });
            });
        };

        this.onVerticesDropped = function(evt, data) {
            if (this.$node.is(':visible')) {
               this.trigger('updateWorkspace', {
                   entityUpdates: data.vertices.map(function(vertex) {
                       return {
                           vertexId: vertex.id,
                           graphLayoutJson: { }
                       };
                   })
               });
            }
        };

        this.onVerticesUpdated = function(evt, data) {
            this.updateOrAddVertices(data.vertices);
        };

        this.onVerticesDeleted = function(evt, data) {
            if (data.vertexIds) {
                this.removeVertexIds(data.vertexIds);
            }
        };

        this.removeVertexIds = function(ids) {
            var self = this;

            if (!ids || ids.length === 0) {
                return;
            }

            this.mapReady(function(map) {
                ids.forEach(function(vertexId) {
                    var feature = self.pinSourceVector.getFeatureById(vertexId)
                    self.removeFeatures(feature);
                })
            });
        }

        this.removeFeatures = function(features) {
            var self = this;

            if (!Array.isArray(features)) features = [features];

            _.compact(features).forEach(function(feature) {
                self.pinSourceVector.removeFeature(feature);
                var remove,
                    selected = self.pinSelectInteraction.getFeatures();

                selected.forEach(function(selected) {
                    var innerFeatures = selected.get('features');
                    if (innerFeatures.length === 1 && innerFeatures[0] === feature) {
                        remove = selected;
                    }
                })
                if (remove) {
                    selected.remove(remove);
                }
            })
        }

        this.onObjectsSelected = function(evt, data) {
            var self = this,
                vertices = data.vertices;

            this.mapReady(function(map) {
                var self = this,
                    selectedIds = _.pluck(vertices, 'id'),
                    toRemove = [],
                    selected = this.pinSelectInteraction.getFeatures();

                // Remove features not selected and not in workspace
                self.pinSourceVector.forEachFeature(function(feature) {
                    if (selectedIds.indexOf(feature.getId()) === -1) {
                        if (feature.get('inWorkspace')) {
                            feature.set('externalGraphic', feature.get('externalGraphic').replace(/&selected/, ''));
                        } else {
                            toRemove.push(feature);
                        }
                    }
                });

                if (toRemove.length) self.removeFeatures(toRemove);

                // Create new features for new selections
                var add = [];
                vertices.forEach(function(vertex) {
                    var operation = self.findAndUpdateOrCreatePinFeature(map, vertex);
                    if (operation && operation.op === 'add') {
                        add.push(operation.feature);
                    }
                });

                if (add.length) {
                    self.pinSourceVector.addFeatures(add);
                }
            });
        };

        this.findAndUpdateOrCreatePinFeature = function(map, vertex, inWorkspace) {
            var self = this,
                geoLocationProperties = this.ontologyProperties.byDataType.geoLocation,
                geoLocations = geoLocationProperties &&
                    _.chain(geoLocationProperties)
                        .map(function(geoLocationProperty) {
                            return F.vertex.props(vertex, geoLocationProperty.title);
                        })
                        .compact()
                        .flatten()
                        .filter(function(g) {
                            return g.value && g.value.latitude && g.value.longitude;
                        })
                        .value(),
                conceptType = F.vertex.prop(vertex, 'conceptType'),
                selected = vertex.id in visalloData.selectedObjects.vertexIds,
                iconUrl = 'map/marker/image?' + $.param({
                    type: conceptType,
                    scale: retina.devicePixelRatio > 1 ? '2' : '1'
                }),
                heading = F.vertex.heading(vertex),
                featureId = vertex.id,
                previousFeature = this.pinSourceVector.getFeatureById(featureId);

            if (!geoLocations || geoLocations.length === 0) {
                if (previousFeature) {
                    return { op: 'remove', feature: previousFeature };
                }
                return;
            }

            if (selected) iconUrl += '&selected';

            if (previousFeature) {
                previousFeature.set('externalGraphic', iconUrl);
                if (!_.isUndefined(inWorkspace)) {
                    previousFeature.set('inWorkspace', inWorkspace);
                }
                previousFeature.set('vertex', vertex);
                previousFeature.set('rotation', heading);
                previousFeature.setGeometry(geometryFromGeolocationProperties(geoLocations))
                return // updated already no nothing
            } else {
                var newFeature = new ol.Feature({
                    geometry: geometryFromGeolocationProperties(geoLocations),
                    vertex: vertex,
                    graphic: true,
                    inWorkspace: inWorkspace,
                    externalGraphic: iconUrl,
                    graphicWidth: 22,
                    graphicHeight: 40,
                    graphicXOffset: -11,
                    graphicYOffset: -40,
                    rotation: heading,
                    cursor: 'pointer'
                });
                newFeature.setId(featureId);
                return { op: 'add', feature: newFeature };
            }
        };

        this.updateOrAddVertices = function(vertices, options) {
            var self = this,
                adding = options && options.adding,
                preventShake = options && options.preventShake,
                validAddition = false;

            this.mapReady(function(map) {
                self.dataRequest('workspace', 'store')
                    .done(function(workspaceVertices) {
                        var ops = { add: [], remove: [] };
                        vertices.forEach(function(vertex) {
                            var inWorkspace = vertex.id in workspaceVertices,
                                featureOperation = self.findAndUpdateOrCreatePinFeature(map, vertex, inWorkspace);

                            if (featureOperation) {
                                ops[featureOperation.op].push(featureOperation.feature);
                                validAddition = validAddition || featureOperation.op === 'add';
                            }
                        });

                        if (ops.add.length) self.pinSourceVector.addFeatures(ops.add);
                        if (ops.remove.length) self.removeFeatures(ops.remove);

                        if (adding && vertices.length && validAddition) {
                            self.fit(map);
                        }

                        if (adding && !validAddition && !preventShake) {
                            self.invalidMap();
                        }
                    })
            });

        };

        this.fit = function(map) {
            var self = this,
                extent = this.pinSourceVector.getExtent(),
                view = map.getView();

            if (!ol.extent.isInfinite(extent) && !ol.extent.isEmpty(extent)) {
                var screenPadding = 2,
                    resolution = view.getResolution(),
                    mapDistance = screenPadding * resolution,
                    extentWithPadding = ol.extent.createEmpty(),
                    extentSize = ol.extent.getSize(extent),
                    buffer = _.max(extentSize) * 0.33

                ol.extent.buffer(extent, buffer, extentWithPadding);

                var padding = {
                        l: this.padding.l,
                        r: this.padding.r + this.select('controlsSelector').width(),
                        t: this.padding.t,
                        b: this.padding.b
                    },
                    viewportWidth = this.$node.width() - padding.l - padding.r,
                    viewportHeight = this.$node.height() - padding.t - padding.b,
                    extentWithPaddingSize = ol.extent.getSize(extentWithPadding),

                    // Figure out ideal resolution based on available realestate
                    idealResolution = view.constrainResolution(Math.max(
                        extentWithPaddingSize[0] / viewportWidth,
                        extentWithPaddingSize[1] / viewportHeight
                    ));

                map.beforeRender(ol.animation.pan({
                    source: view.getCenter(),
                    duration: ANIMATION_DURATION
                }))
                map.beforeRender(ol.animation.zoom({
                    resolution: resolution,
                    duration: ANIMATION_DURATION
                }));
                view.setResolution(idealResolution);

                var center = ol.extent.getCenter(extentWithPadding),
                    offsetX = padding.l - padding.r,
                    offsetY = padding.t - padding.b,
                    lon = offsetX * view.getResolution() / 2,
                    lat = offsetY * view.getResolution() / 2;

                view.setCenter([center[0] - lon, center[1] - lat]);
            } else {
                map.beforeRender(ol.animation.zoom({
                    resolution: view.getResolution(),
                    duration: ANIMATION_DURATION
                }));
                map.beforeRender(ol.animation.pan({
                    source: view.getCenter(),
                    duration: ANIMATION_DURATION
                }))
                var params = this.getDefaultViewParameters();
                view.setZoom(params.zoom);
                view.setCenter(params.center);
            }
        };

        this.invalidMap = function() {
            var map = this.select('mapSelector'),
                cls = 'invalid',
                animate = function() {
                    map.removeClass(cls);
                    _.defer(function() {
                        map.on(ANIMATION_END, function() {
                            map.off(ANIMATION_END);
                            map.removeClass(cls);
                        });
                        map.addClass(cls);
                    });
                };

            if (this.$node.closest('.visible').length === 0) {
                return;
            } else if (!this.preventShake) {
                animate();
            }
        };

        this.handleContextMenu = function(event) {
            event.originalEvent = event.originalEvent || event;

            this.mapReady(function(map) {
                var coordinate = map.getEventCoordinate(event),
                    extent = ol.extent.createEmpty(),
                    mapDistance = FEATURE_HEIGHT * map.getView().getResolution();

                ol.extent.createOrUpdateFromCoordinate(coordinate, extent);
                ol.extent.buffer(extent, mapDistance, extent);

                var clusters = this.pinSourceCluster.getFeaturesInExtent(extent),
                    vertexIds = _.uniq(_.flatten(clusters.map(c => c.get('features').map(f => f.getId()))));

                if (vertexIds.length) {
                    this.trigger('selectObjects', { vertexIds: vertexIds });
                    if (vertexIds.length === 1) {
                        this.trigger('showVertexContextMenu', {
                            vertexId: vertexIds[0],
                            position: {
                                x: event.pageX,
                                y: event.pageY
                            }
                        });
                    } else {
                        var menu = this.select('contextMenuVertexSelector');
                        this.toggleMenu({ positionUsingEvent: event }, menu);
                    }
                }
            });
        };

        this.onSearchResultsWithinRadius = function(event, data) {
            var self = this;

            if (data && data.currentValue && data.currentValue.longitude && data.currentValue.longitude) {
                this.$node.find('.instructions').remove();
                this.$node.append(radiusTemplate({}));
                this.mapReady(function(map) {
                    if (this.moveInteraction) {
                        map.removeInteraction(this.moveInteraction);
                    }
                    var {longitude, latitude, radius} = data.currentValue;
                    var centerCoordinate = ol.proj.fromLonLat([longitude, latitude])
                    var move = new FeatureMoveInteraction({ center: centerCoordinate, radius: radius * 1000 });
                    this.moveInteraction = move;
                    map.addInteraction(move);
                    this.mode = MODE_REGION_SELECTION_MODE_RADIUS;
                })
            } else {
                if (this.mode === MODE_NORMAL) {
                    this.mode = MODE_REGION_SELECTION_MODE_POINT;
                    this.$node.find('.instructions').remove();
                    this.$node.append(centerTemplate({}));
                    $(document).on('keydown.regionselection', function(e) {
                        if (e.which === $.ui.keyCode.ESCAPE) {
                            self.endRegionSelection();
                        }
                    });
                } else if (this.mode === MODE_REGION_SELECTION_MODE_RADIUS) {
                    this.panToActiveRadius();
                }
            }
        };

        this.panToActiveRadius = function() {
            if (this.moveInteraction) {
                this.moveInteraction.fit();
            }
        };

        this.endRegionSelection = function() {
            this.mode = MODE_NORMAL;

            this.off('mousemove');
            $('#map_mouse_position_hack').remove();
            this.$node.find('.instructions').remove();

            if (this.moveInteraction) {
                this.mapReady(function(map) {
                    map.removeInteraction(this.moveInteraction);
                });
            }

            $(document).off('keydown.regionselection');
        };

        this.onMapClicked = function(evt, map) {
            var self = this;
            this.$node.find('.instructions').remove();

            switch (self.mode) {
                case MODE_NORMAL:
                    $('.dialog-popover').remove();
                    break;

                case MODE_REGION_SELECTION_MODE_POINT:

                    self.mode = MODE_REGION_SELECTION_MODE_RADIUS;

                    this.$node.append(radiusTemplate({}));

                    var offset = self.$node.offset(),
                        centerCoordinate = map.getCoordinateFromPixel([evt.pageX - offset.left, evt.pageY - offset.top]),
                        move = new FeatureMoveInteraction({ center: centerCoordinate, fit: false });

                    this.moveInteraction = move;
                    map.addInteraction(move);

                    break;

                case MODE_REGION_SELECTION_MODE_RADIUS:

                    self.mode = MODE_REGION_SELECTION_MODE_LOADING;

                    var { center, radius } = this.moveInteraction.getRegion(),
                        lonlat = ol.proj.toLonLat(center),
                        region = {
                            radius: radius,
                            longitude: lonlat[0],
                            latitude: lonlat[1]
                        };

                    self.$node.find('.instructions').remove();
                    self.$node.append(loadingTemplate({}));

                    self.trigger('regionSaved', region);
                    self.endRegionSelection();

                    break;
            }
        };

        this.initializeMap = function() {
            var self = this,
                openlayersDeferred = $.Deferred(),
                clusterStrategyDeferred = $.Deferred(),
                mapProviderDeferred = $.Deferred();

            require(['openlayers'], openlayersDeferred.resolve);
            require(['map/multipointCluster'], clusterStrategyDeferred.resolve);

            this.dataRequest('config', 'properties').done(function(configProperties) {
                mapProviderDeferred.resolve(configProperties);
            });

            $.when(
              openlayersDeferred,
              clusterStrategyDeferred,
              mapProviderDeferred
            ).done(function(openlayers, cluster, configProperties) {
              ol = openlayers;
              self.createMap(ol, cluster, configProperties);
            });
        };

        this.createMap = function(ol, MultiPointCluster, configProperties) {
            var self = this;

            this.pinSourceVector = new ol.source.Vector({ features: [] });
            this.pinSourceCluster = new MultiPointCluster({
                distance: Math.max(FEATURE_CLUSTER_HEIGHT, FEATURE_HEIGHT),
                source: this.pinSourceVector
            });
            var styleCache = {};
            this.pinClusters = new ol.layer.Vector({
                id: 'workspaceVerticesLayer',
                source: this.pinSourceCluster,
                style: cluster => this.clusterStyle(cluster)
            });

            var map = new ol.Map({
                    loadTilesWhileInteracting: true,
                    keyboardEventTarget: document,
                    controls: [],
                    layers: [
                    ],
                    target: this.select('mapSelector')[0]
                });

            // Feature Selection
            this.pinSelectInteraction = new ol.interaction.Select({
                condition: ol.events.condition.click,
                layers: [this.pinClusters],
                style: cluster => this.clusterStyle(cluster)
            });
            this.pinSourceCluster.on(ol.events.EventType.CHANGE, _.debounce(function() {
                var selected = self.pinSelectInteraction.getFeatures(),
                    clusters = this.getFeatures();

                if (visalloData.selectedObjects.vertices.length) {
                    var newSelection = [],
                        selectedInnerIds = _.indexBy(_.flatten(
                            selected.getArray().map(c => c.get('features').map(f => f.getId()))
                        )),
                        isSelected = feature => feature.getId() in visalloData.selectedObjects.vertexIds;

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
                }


            }, 100));
            map.addInteraction(this.pinSelectInteraction);
            this.pinSelectInteraction.on('select', function(e) {
                var features = e.target.getFeatures().getArray(),
                    vertexIds = _.chain(features)
                        .map(function(feature) { return feature.get('features').map(f => f.getId()); })
                        .flatten()
                        .value()
                self.trigger('selectObjects', { vertexIds: vertexIds });
            });

            map.getViewport().addEventListener('contextmenu', function (evt) {
                evt.preventDefault();
            });
            map.getViewport().addEventListener('mouseup', function (evt) {
                evt.preventDefault();
                if (evt.button === 2 || evt.ctrlKey) {
                    self.handleContextMenu(evt);
                }
            });
            map.on('click', function(event) {
                self.closeMenu();
                self.onMapClicked(event, map);
            })

            var provider = configProperties['map.provider'] || 'osm',
                config = Object.assign({}, configProperties),
                baseLayerSource,
                getOptions = function(providerName) {
                    try {
                        var obj,
                            prefix = `map.provider.${providerName}.`,
                            options = _.chain(config)
                                .pick((val, key) => key.indexOf(`map.provider.${providerName}.`) === 0)
                                .tap(o => { obj = o })
                                .pairs()
                                .map(([key, value]) => {
                                    if (/^[\d.-]+$/.test(value)) {
                                        value = parseFloat(value, 10);
                                    } else if ((/^(true|false)$/).test(value)) {
                                        value = value === 'true'
                                    } else if ((/^\[[^\]]+\]$/).test(value) || (/^\{[^\}]+\}$/).test(value)) {
                                        value = JSON.parse(value)
                                    }
                                    return [key.replace(prefix, ''), value]
                                })
                                .object()
                                .value()
                        return options;
                    } catch(e) {
                        console.error(`${prefix} options could not be parsed. input:`, obj)
                        throw e;
                    }
                };

            if (provider === 'google') {
                console.warn('google map.provider is no longer supported, switching to OpenStreetMap provider');
            }

            if (provider === 'google' || provider === 'osm') {
                // Legacy configs accepted csv urls, warn and pick first
                var osmURL = config['map.provider.osm.url'];
                if (osmURL && osmURL.indexOf(',') >= 0) {
                    console.warn('Comma-separated Urls not supported, using first url. Use urls with {a-c} for multiple CDNS');
                    console.warn('For Example: https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png');
                    config['map.provider.osm.url'] = osmURL.split(',')[0].trim().replace(/[$][{]/g, '{');
                }
                baseLayerSource = new ol.source.OSM(getOptions('osm'))

            } else if (provider === 'ArcGIS93Rest') {
                var urlKey = 'map.provider.ArcGIS93Rest.url';
                // New OL3 ArcGIS Source will throw an error if url doesn't end
                // with [Map|Image]Server
                if (config[urlKey]) {
                    config[urlKey] = config[urlKey].replace(/\/export(Image)?\/?\s*$/, '');
                }
                baseLayerSource = new ol.source.TileArcGISRest(Object.assign({
                    params: { layers: 'show:0,1,2' }
                }, getOptions(provider)))

            } else if (provider in ol.source && _.isFunction(ol.source[provider])) {
                // http://openlayers.org/en/latest/apidoc/ol.source.html
                baseLayerSource = new ol.source[provider](getOptions(provider))

            } else {
                console.error('Unknown map provider type: ', config['map.provider']);
                throw new Error('map.provider is invalid')
            }

            var params = self.getDefaultViewParameters();
            map.addLayer(new ol.layer.Tile({ source: baseLayerSource }));
            map.addLayer(self.pinClusters);
            map.setView(new ol.View(params));
            self.dataRequest('ontology', 'properties')
                .done(function(p) {
                    self.ontologyProperties = p;
                    // Prevent map shake on initialize while catching up with vertexAdd
                    // events
                    self.preventShake = true;
                    self.mapMarkReady(map);
                    self.mapReady().done(function(m) {
                        self.preventShake = false;
                    });
                })
        };

        this.getDefaultViewParameters = function() {
            return {
                zoom: 2,
                center: [0, 0]
            };
        };

        this.clusterStyle = function() {
            if (!this._clusterStyleWithCache) {
                this._clusterStyleWithCache = _.memoize(
                    this._clusterStyle.bind(this),
                    function clusterStateHash(cluster) {
                        var count = cluster.get('count'),
                            selectionState = cluster.get('selectionState') || 'none',
                            key = [count, selectionState];

                        if (count === 1) {
                            var vertex = cluster.get('features')[0].get('vertex'),
                                conceptType = F.vertex.prop(vertex, 'conceptType');
                            key.push('type=' + conceptType);
                        }
                        return key.join('')
                    }
                );
            }

            return this._clusterStyleWithCache.apply(this, arguments);
        };

        this._clusterStyle = function(cluster) {
            var count = cluster.get('count'),
                selectionState = cluster.get('selectionState') || 'none',
                selected = selectionState !== 'none';

            if (count === 1) {
                var vertex = cluster.get('features')[0].get('vertex'),
                    type = F.vertex.prop(vertex, 'conceptType'),
                    scale = retina.devicePixelRatio,
                    src = 'map/marker/image?' + $.param({ type, scale: scale > 1 ? '2' : '1', selected }),
                    imgSize = FEATURE_SIZE.map(v => v * scale);

                return [new ol.style.Style({
                    image: new ol.style.Icon({ src, imgSize, scale: 1 / scale, anchor: [0.5, 1.0] })
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
        }

        function geometryFromGeolocationProperties(geoLocations) {
            return new ol.geom.MultiPoint(geoLocations.map(function(geo) {
                var coord = [geo.value.longitude, geo.value.latitude];
                return ol.proj.fromLonLat(coord);
            }))
        }
    }

});

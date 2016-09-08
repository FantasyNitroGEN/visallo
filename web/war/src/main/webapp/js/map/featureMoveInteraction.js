define(['openlayers'], function(ol) {
    'use strict';

    var ANIMATION_DURATION = 200,
        RESIZE_RADIUS = 6,
        FeatureMoveInteraction = function(optOptions) {
            ol.interaction.Pointer.call(this, {
                handleEvent: this.handle.bind(this)
            });
            var options = optOptions ? optOptions : {};
            this.lastCentroid = null;
            this.circleCenter = options.center;
            this.circleRadius = options.radius;
            this.shouldFit = options.fit !== false;
            this.condition_ = options.condition ?
                options.condition : ol.events.condition.primaryAction;

        };
    ol.inherits(FeatureMoveInteraction, ol.interaction.Pointer);

    FeatureMoveInteraction.prototype.fit = function() {
        var map = this.getMap(),
            view = map.getView();

        map.beforeRender(ol.animation.zoom({
            resolution: view.getResolution(),
            duration: ANIMATION_DURATION
        }));
        map.beforeRender(ol.animation.pan({
            source: view.getCenter(),
            duration: ANIMATION_DURATION
        }));
        view.fit(this.circleFeature.getGeometry().getExtent(), map.getSize())
    }

    FeatureMoveInteraction.prototype.setMap = function(map) {
        if (!map) {
            this.getMap().removeLayer(this.layer)
        }

        ol.interaction.Pointer.prototype.setMap.apply(this, arguments);

        if (!map) return;

        var center = this.circleCenter,
            extent = map.getView().calculateExtent(map.getSize()),
            circleRadius = this.circleRadius || ol.extent.getWidth(extent) * 0.1 / 2,
            circleGeometry = new ol.geom.Circle(center, circleRadius),
            resizeGeometry = new ol.geom.Point(calculateResizeCoordinate(center, circleRadius)),
            circleFeature = new ol.Feature({ geometry: circleGeometry }),
            resizeFeature = new ol.Feature({ geometry: resizeGeometry });

        circleFeature.setId('circle');
        resizeFeature.setId('resize');

        this.circleFeature = circleFeature;
        this.resizeFeature = resizeFeature;

        var vectorSource = new ol.source.Vector({
                features: [circleFeature, resizeFeature],
            }),
            circleLayer = new ol.layer.Vector({
                source: vectorSource,
                style: function(feature) {
                    var fill = new ol.style.Fill({ color: 'rgba(255,255,255,0.4)' });
                    var stroke = new ol.style.Stroke({ color: '#3399CC', width: 1.25 });
                    if (feature.getId() === 'resize') {
                        return [new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: RESIZE_RADIUS,
                                stroke: new ol.style.Stroke({
                                    color: stroke.getColor(),
                                    width: 2
                                }),
                                fill: new ol.style.Fill({
                                    color: 'white'
                                })
                            })/*,
                            text: new ol.style.Text({
                                text: '10KM',
                                font: 'bold 20px sans-serif',
                                fill: new ol.style.Fill({ color: 'black' })
                            })
                            */
                        })]
                    } else {
                        return [new ol.style.Style({
                            image: new ol.style.Circle({
                                fill: fill,
                                stroke: stroke,
                                radius: 5
                            }),
                            fill: fill,
                            stroke: stroke
                        })]
                    }
                }
            });

        this.layer = circleLayer;
        map.addLayer(circleLayer);

        if (this.shouldFit) {
            this.fit();
        }
    }

    FeatureMoveInteraction.prototype.getRegion = function(e) {
        var geo = this.circleFeature.getGeometry()
        return {
            center: geo.getCenter(),
            radius: geo.getRadius() / 1000
        };
    }

    FeatureMoveInteraction.prototype.handle = function(e) {
        var handled = false;
        if (this[e.type]) {
            handled = this[e.type].apply(this, arguments);
        }
        return ol.interaction.Pointer.handleEvent.call(this, e) && !handled;
    }

    FeatureMoveInteraction.prototype.pointerdown = function(event) {
        var extent = ol.extent.createEmpty(),
            mapDistance = RESIZE_RADIUS * this.getMap().getView().getResolution();

        ol.extent.createOrUpdateFromCoordinate(event.coordinate, extent);
        ol.extent.buffer(extent, mapDistance, extent);

        var features = this.layer.getSource().getFeaturesInExtent(extent);
        if (features.length) {
            this.startCoordinate = event.coordinate;
            this.circle = this.circleFeature.getGeometry()
            this.resize = this.resizeFeature.getGeometry()

            if (features.length === 1 && features[0].getId() === 'circle') {
                this.center = this.circle.getCenter();
                this.state = 'down'
            } else {
                this.center = this.resize.getCoordinates();
                this.state = 'resize'
            }
            return true;
        }
    }

    FeatureMoveInteraction.prototype.pointermove = function(event) {
        if (this.state) {
            var delta = subtractCoordinates(event.coordinate, this.startCoordinate),
                newCenter = addCoordinates(this.center, delta);
        }

        if (this.state === 'down') {
            this.circle.setCenter(newCenter)
            this.resize.setCoordinates(calculateResizeCoordinate(newCenter, this.circle.getRadius()))
            return true;
        } else if (this.state === 'resize') {
            var circleCenter = this.circle.getCenter(),
                newRadius = new ol.geom.LineString([newCenter, circleCenter]).getLength();
            this.circle.setRadius(newRadius);
            this.resize.setCoordinates(newCenter);//calculateResizeCoordinate(circleCenter, newRadius));
            return true;
        }
    }

    FeatureMoveInteraction.prototype.pointerup = function(event) {
        this.state = null;
    }

    return FeatureMoveInteraction;

    function subtractCoordinates(c1, c2) {
        return c1.map((c, i) => c - c2[i]);
    }

    function addCoordinates(c1, c2) {
        return c1.map((c, i) => c + c2[i])
    }

    function calculateResizeCoordinate(coord, radius) {
        var angle = Math.PI / -4
        return [
            coord[0] + radius * Math.cos(angle),
            coord[1] + radius * Math.sin(angle)
        ];
    }
});

define([
    'flight/lib/component',
    '../withPopover',
    'openlayers',
    './featureMoveInteraction',
    'util/mapConfig'
], function(
    defineComponent,
    withPopover,
    ol,
    FeatureMoveInteraction,
    mapConfig) {
    'use strict';

    var MODE_REGION_SELECTION_MODE_POINT = 1,
        MODE_REGION_SELECTION_MODE_RADIUS = 2,
        MODE_REGION_SELECTION_MODE_RADIUS_DRAGGING = 3;

    return defineComponent(MapSearchPopover, withPopover);

    function MapSearchPopover() {

        this.defaultAttrs({
            mapSelector: '.map',
            closeSelector: '.close'
        });

        this.before('initialize', function(node, config) {
            config.template = 'mapSearch/template';

            this.after('setupWithTemplate', function() {
                var self = this;

                this.positionDialog();
                this.mode = MODE_REGION_SELECTION_MODE_POINT;
                this.setInfo();
                this.setupMap();
            });
        });

        this.onToggle = function(e) {
            e.stopPropagation();
            this.teardown();
        };

        this.setupMap = function() {
            var currentValue = this.attr.currentValue;
            var { latitude, longitude, radius } = currentValue;
            var existing = true;
            if (_.isUndefined(latitude)) {
                existing = false;
                latitude = 0;
            }
            if (_.isUndefined(longitude)) {
                existing = false;
                longitude = 0;
            }

            var { source, sourceOptions } = mapConfig();
            var map = new ol.Map({
                target: this.popover.find(this.attr.mapSelector)[0],
                layers: [
                    new ol.layer.Tile({ source: new ol.source[source](sourceOptions) })
                ],
                controls: [new ol.control.Zoom()],
                view: new ol.View({
                    center: ol.proj.transform([longitude, latitude], 'EPSG:4326', 'EPSG:3857'),
                    zoom: 1
                })
            });
            this.map = map;

            map.on('click', event => this.onMapClicked(event))
            this.on(this.popover, 'click', {
                closeSelector: this.teardown
            })

            if (existing) {
                this.mode = MODE_REGION_SELECTION_MODE_RADIUS;
                this.setInfo();
                this.createCircleInteraction({
                    fit: true,
                    center: ol.proj.fromLonLat([longitude, latitude]),
                    radius: (radius || 100) * 1000
                })
            }
        };

        this.onMapClicked = function(event, map) {
            if (this.blockDoubleTimer) {
                clearTimeout(this.blockDoubleTimer);
                this.blockDoubleTimer = null;
            } else {
                this.blockDoubleTimer = _.delay(() => {
                    this.blockDoubleTimer = null;
                    this.onMapClickedBlockDoubleClicks(event)
                }, 250);
            }
        };

        this.onMapClickedBlockDoubleClicks = function(event) {
            var map = this.map;

            switch (this.mode) {

                case MODE_REGION_SELECTION_MODE_POINT:
                    this.mode = MODE_REGION_SELECTION_MODE_RADIUS;
                    this.createCircleInteraction({ center: event.coordinate, fit: false });
                    break;

            }

            this.setInfo();
        };

        this.update = function() {
            var { center, radius } = this.moveInteraction.getRegion(),
                lonlat = ol.proj.toLonLat(center),
                region = {
                    radius: radius,
                    longitude: lonlat[0],
                    latitude: lonlat[1]
                };
            this.trigger('mapSearchRegionUpdated', { region });
            this.setInfo();
        };

        this.setInfo = function() {
            var $info = this.popover.find('.info');
            switch (this.mode) {
                case MODE_REGION_SELECTION_MODE_POINT:
                    return $info.text(i18n('field.geolocation.radius.search.selectpoint'));
                case MODE_REGION_SELECTION_MODE_RADIUS:
                    return $info.text(i18n('field.geolocation.radius.search.selectradius'));
                case MODE_REGION_SELECTION_MODE_RADIUS_DRAGGING:
                    return $info.text(i18n('field.geolocation.radius.search.dragging',
                        this.radius.toFixed(2),
                        (this.radius * 0.62137).toFixed(2)
                    ));
            }
        };

        this.createCircleInteraction = function(options) {
            this.moveInteraction = new FeatureMoveInteraction(options);
            this.moveInteraction.on('radiusChanged', event => {
                this.mode = MODE_REGION_SELECTION_MODE_RADIUS_DRAGGING;
                this.radius = event.newRadius;
                this.update();
            })
            this.moveInteraction.on('centerChanged', event => {
                this.center = event.newCenter;
                this.update();
            })
            this.map.addInteraction(this.moveInteraction);
        };
    }
});

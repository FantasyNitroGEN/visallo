/**
 * This file is a fork of http://openlayers.org/en/master/apidoc/ol.source.Cluster.html
 * with support for multi-point clusters.
 */
define(['openlayers'], function(ol) {
    'use strict';

    var MultiPointCluster = function(options) {
        ol.source.Vector.call(this, {
            attributions: options.attributions,
            extent: options.extent,
            logo: options.logo,
            projection: options.projection,
            wrapX: options.wrapX
        });
        this.resolution_ = undefined;
        this.distance_ = options.distance !== undefined ? options.distance : 20;
        this.features_ = [];
        this.geometryFunction_ = options.geometryFunction || function(feature) {
            var geometry = feature.getGeometry();
            ol.asserts.assert(geometry instanceof ol.geom.MultiPoint, 10);
            return geometry;
        };
        this.source_ = options.source;
        this.source_.on(ol.events.EventType.CHANGE,
            MultiPointCluster.prototype.refresh_, this);
    };

    ol.inherits(MultiPointCluster, ol.source.Cluster);

    MultiPointCluster.prototype.getDistance = function() {
        return this.distance_;
    }

    MultiPointCluster.prototype.cluster_ = function() {
        var self = this;
        if (this.resolution_ === undefined) {
            return;
        }
        this.features_.length = 0;
        var mapDistance = this.distance_ * this.resolution_;
        var source = this.source_;
        var features = source.getFeatures();
        var clustered = {};

        features.forEach(clusterFeature);

        function clusterFeature(feature) {
            var geometry = self.geometryFunction_(feature);
            if (geometry) {
                var coordinates = geometry.getCoordinates();
                coordinates.forEach(clusterCoordinate);
            }

            function clusterCoordinate(coordinate, coordinateIndex) {
                var extent = ol.extent.createEmpty();
                if (!((ol.getUid(feature).toString() + '_' + coordinateIndex) in clustered)) {
                    ol.extent.createOrUpdateFromCoordinate(coordinate, extent);
                    ol.extent.buffer(extent, mapDistance, extent);

                    var neighbors = source.getFeaturesInExtent(extent),
                        coords = [],
                        count = 0;

                    neighbors = neighbors.filter(function(neighbor) {
                        var neighborGeometry = self.geometryFunction_(neighbor);
                        var neighborCoordinates = neighborGeometry.getCoordinates();
                        var neighborUid = ol.getUid(neighbor).toString() + '_';

                        var coordsInCluster = neighborCoordinates.filter(function(coordinate, coordinateIndex) {
                            var uid = neighborUid + coordinateIndex;
                            if (ol.extent.containsCoordinate(extent, coordinate)) {
                                if (!(uid in clustered)) {
                                    coords.push(coordinate)
                                    clustered[uid] = true;
                                    return true;
                                }
                            }
                            return false;
                        }).length
                        count += coordsInCluster;
                        return coordsInCluster > 0;
                    });
                    self.features_.push(self.createCluster_(neighbors, coords, count));
                }
            }
        }
    };

    MultiPointCluster.prototype.createCluster_ = function(features, coords, count) {
        var centers = coords.reduce((sums, c) => {
                return sums.map((s, i) => s + c[i])
            }, [0, 0]),
            average = centers.map(val => val / coords.length),
            cluster = new ol.Feature(new ol.geom.Point(average));
        cluster.set('features', features);
        cluster.set('count', count);
        return cluster;
    };

    return MultiPointCluster;
});

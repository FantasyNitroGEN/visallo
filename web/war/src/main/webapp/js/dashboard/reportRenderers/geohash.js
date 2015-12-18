define([
    'flight/lib/component',
    'util/formatters',
    './withRenderer',
    './withMapTiles',
    'd3'
], function(
    defineComponent,
    F,
    withRenderer,
    withMapTiles,
    d3) {
    'use strict';

    return defineComponent(Geohash, withRenderer, withMapTiles);

    function Geohash() {
        this.defaultAttrs({ svgCls: 'geohash' });

        this.processData = function(data) {
            var results = data.root[0].buckets;
            if (results && results.length) {
                var min = Infinity,
                    max = -Infinity,
                    features = _.map(results, function(bucket) {
                        var northWest = bucket.value.cell.northWest,
                            southEast = bucket.value.cell.southEast,
                            amount = bucket.value.count || 0;

                        max = Math.max(amount, max);
                        min = Math.min(amount, min);

                        return {
                              type: 'Feature',
                              geometry: {
                                  type: 'Polygon',
                                  coordinates: [[
                                    [northWest.longitude, northWest.latitude],
                                    [southEast.longitude, northWest.latitude],
                                    [southEast.longitude, southEast.latitude],
                                    [northWest.longitude, southEast.latitude],
                                    [northWest.longitude, northWest.latitude]
                                  ]]
                              },
                              properties: { amount: amount }
                              // FIXME search doesn't support geohash values
                                  //_.extend({}, bucket, { amount: amount })
                          };
                    }),
                    featureCollection = { type: 'FeatureCollection', features: features, max: max, min: min };

                return _.extend(featureCollection, { bbox: _.flatten(d3.geo.bounds(featureCollection)) });
            }
            return null;
        };
    }
});

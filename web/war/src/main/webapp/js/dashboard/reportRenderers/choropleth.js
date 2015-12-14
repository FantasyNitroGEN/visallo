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

    return defineComponent(Choropleth, withRenderer, withMapTiles);

    function Choropleth() {
        this.defaultAttrs({ svgCls: 'choropleth' });

        this.processData = function(data) {
            var self = this,
                results = data.root[0].buckets,
                zipCodeBoundary = function(params) {
                    return self.dataRequest('dashboard', 'requestData', '/zip-code-boundary', params);
                };

            if (results && results.length) {
                return zipCodeBoundary({ zipCode: _.pluck(results, 'name') })
                           .then(function(zipCodes) {
                               var min = Infinity,
                                   max = -Infinity,
                                   features = zipCodes.features.map(function(feature) {
                                           var bucket = _.findWhere(results, { name: feature.zipCode }),
                                               amount = bucket ? bucket.value.count : 0;

                                           min = Math.min(min, amount);
                                           max = Math.max(max, amount);

                                           return {
                                               type: 'Feature',
                                               geometry: {
                                                   type: 'Polygon',
                                                   coordinates: feature.coordinates.map(function(c) {
                                                           return _.invoke(c, 'reverse');
                                                       })

                                               },
                                               properties: _.extend({}, bucket, {
                                                   label: feature.zipCode,
                                                   amount: amount
                                               })
                                           }
                                       }),
                                   featureCollection = { type: 'FeatureCollection', features: features, max: max, min: min };

                               return _.extend(featureCollection, { bbox: _.flatten(d3.geo.bounds(featureCollection)) });
                           });
            }
            return null;
        };
    }
});

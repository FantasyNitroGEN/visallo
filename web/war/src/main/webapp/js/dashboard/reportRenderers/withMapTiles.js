define([
    'd3',
    'd3-plugins/tile/amd/index'
], function(
    d3,
    d3Tile) {
    'use strict';

    return withMapTiles;

    function withMapTiles() {
        this.render = function(d3, node, data, d3tip) {
            var $node = $(node);
            if (!data) {
                $node.empty();
                return;
            }

            var self = this,
                width = node.offsetWidth,
                height = node.offsetHeight,
                projection = this.calculateDataProjection(data.features, width, height),
                path = d3.geo.path().projection(projection),
                scale = d3.scale
                            .linear()
                            .domain([data.min, data.max])
                            .range(['#c9dbc1', '#003f19'])
                            .clamp(true),
                svg = d3.select(node)
                    .selectAll('svg')
                    .data([1])
                    .call(function() {
                        this.enter()
                            .append('svg')
                            .classed('vector-map', true)
                            .classed(self.attr.svgCls, true)
                            .call(function() {
                                this.append('g').attr('class', 'tiles');
                                this.append('g').attr('class', 'data');
                            })
                    })
                    .attr({ width: width, height: height }),
                tip = this.createTooltip(svg,
                                            d3tip,
                                            function(d) { return d.properties.amount; },
                                            function(d) { return d.properties.label; });

            this.addMapTileLayer(svg, projection);

            svg.select('g.data')
                .selectAll('path')
                .data(data.features)
                .call(function() {
                    this.enter()
                        .append('path')
                        .attr({ fill: setFill, d: path })
                    this.exit().remove();
                })
                .on('click', function(d) {
                    if (!d.properties.field) return;
                    self.handleClick({
                        filters: [
                            {
                                propertyId: d.properties.field,
                                predicate: 'equal',
                                values: [d.properties.name]
                            }
                        ]
                    })
                })
                .on('mouseover', function(d) {
                    var d3This = d3.select(this),
                        currentFill = d3This.style('fill'),
                        hoverFill = d3.rgb(currentFill).brighter(0.3);
                    d3This.style('fill', hoverFill);
                    tip.show.apply(this, arguments);
                })
                .on('mousedown', tip.hide)
                .on('mouseout', function(d) {
                    d3.select(this).style('fill', setFill(d));
                    tip.hide.apply(this, arguments);
                })
                .transition()
                .attr({ fill: setFill, d: path });

            function setFill(d) {
                var amount = d.properties.amount;
                return scale(amount || 0);
            }

            function setStroke(d) {
                var amount = d.properties.amount;
                return d3.rgb(scale(amount || 0)).darker(0.5);
            }
        };

        this.calculateDataProjection = function(features, width, height) {
            var projection = d3.geo.mercator().scale(1).translate([0, 0]),
                path = d3.geo.path().projection(projection);

            var b = [[Infinity, Infinity], [-Infinity, -Infinity]];

            features.forEach(function(feature) {
               var b2 = path.bounds(feature);
               b[0][0] = Math.min(b[0][0], b2[0][0]);
               b[0][1] = Math.min(b[0][1], b2[0][1]);

               b[1][0] = Math.max(b[1][0], b2[1][0]);
               b[1][1] = Math.max(b[1][1], b2[1][1]);
            })

            var s = .95 / Math.max((b[1][0] - b[0][0]) / width, (b[1][1] - b[0][1]) / height),
                t = [(width - s * (b[1][0] + b[0][0])) / 2, (height - s * (b[1][1] + b[0][1])) / 2];

            projection.scale(s).translate(t);
            return projection;
        };

        this.addMapTileLayer = function(svg, projection) {
            var tile = d3Tile.default()
                    .scale(projection.scale() * 2 * Math.PI)
                    .translate(projection.translate())
                    .size([svg.attr('width'), svg.attr('height')]),
                tiles = tile();

            svg.select('g.tiles')
                .style('transform', matrix3d(tiles.scale, tiles.translate))
                .selectAll('.tile')
                    .data(tiles, _.identity)
                    .call(function() {
                        this.exit().each(function(d) {
                            if (this._xhrs) this._xhr.abort();
                        }).remove();
                        this.enter().append('g').attr('class', 'tile')

                        this.attr('transform', function(d) {
                            return 'translate(' + (d[0] * 256) + ',' + (d[1] * 256) + ')';
                        })
                        .each(function(d) {
                            this._xhr = requestMapTiles(d3.select(this), d);
                        });
                    });

            function matrix3d(scale, translate) {
                var k = scale / 256, r = scale % 1 ? Number : Math.round;
                return 'matrix3d(' + [k, 0, 0, 0, 0, k, 0, 0, 0, 0, k, 0, r(translate[0] * scale), r(translate[1] * scale), 0, 1 ] + ')';
            }
        };

        function requestMapTiles(tile, d) {
            var z = d[2],
                tileProjection = d3.geo.mercator(),
                tilePath = d3.geo.path().projection(tileProjection);

            return d3.json('mapzen/osm/all/' + [z, d[0], d[1]].join('/') + '.json', function(err, json) {
                if (err) {
                    return console.error(err);
                }

                var k = Math.pow(2, z) * 256; // size of the world in pixels

                tilePath.projection()
                    .translate([k / 2 - d[0] * 256, k / 2 - d[1] * 256]) // [0°,0°] in pixels
                    .scale(k / 2 / Math.PI)
                    .precision(0);

                var featureTypes = ['water.ocean', 'earth', 'water.riverbank', 'water.reservoir', 'water.river', 'water.lake', 'water.canal', 'landuse', 'roads', 'boundaries']; // water, landuse, boundaries, buildings, earth, landuse_labels, places, pois, roads, transit
                _.each(featureTypes, function(featureType) {
                    var featureTypeComponents = featureType.split('.'),
                        featureMajorType = featureTypeComponents[0],
                        featureMinorType = featureTypeComponents.length > 1 ? featureTypeComponents[1] : null,
                        featurePathData = json[featureMajorType].features.sort(function(a, b) { return a.properties.sort_key ? a.properties.sort_key - b.properties.sort_key : 0; });

                    if (featureMinorType) {
                        featurePathData = _.filter(featurePathData, function(d) {
                            return featureMinorType === (d.properties.type || d.properties.kind);
                        });
                    }

                    tile.selectAll('g.' + featureType)
                        .data([1])
                        .call(function() {
                            this.enter()
                                .append('g')
                                .classed(featureMajorType, true);

                            if (featureMinorType) {
                                this.classed(featureMinorType, true);
                            }

                        })
                        .selectAll('path')
                            .data(featurePathData)
                            .enter().append('path')
                            .attr('class', function(d) { return d.properties.type || d.properties.kind; })
                            .attr('d', tilePath);
                });
              })
        }
    }
});


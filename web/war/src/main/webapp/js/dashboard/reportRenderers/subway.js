define([
    'flight/lib/component',
    'util/formatters',
    'colorjs',
    './withRenderer'
], function(
    defineComponent,
    F,
    colorjs,
    withRenderer) {
        'use strict';

        return defineComponent(Subway, withRenderer);

        function Subway() {

            this.processData = function(data) {
                if (this.attr.report.mapping && this.attr.report.mapping.transformerModulePath) {
                    return data.root[0].buckets;
                }
                return _.chain(data.root[0].buckets)
                    .sortBy(function(b) {
                        return b.value.count;
                    })
                    .value()
                    .reverse();
            };

            this.render = function renderSubway(d3, node, data, d3tip) {
                node = $(node).empty();

                var self = this,
                    report = this.attr.report,
                    config = _.extend({
                        variance: 'NONE',// ['PERCENT', 'AMOUNT']
                        defaultColor: '#ccc',
                        sort: 'COUNT_DESC'
                    }, this.attr.reportConfiguration || {}),
                    width = node.width(),
                    height = node.height(),
                    outerRadius = 45,
                    outerDiameter = outerRadius * 2,
                    strokeColor = '#C7E2F0',
                    strokeWidth = Math.round(outerRadius * 0.1),
                    titleDistance = Math.round(outerRadius * 0.8),
                    radius = outerRadius - strokeWidth * 2,
                    diameter = radius * 2,
                    distance = Math.max(50, Math.floor((width - (data.length * outerDiameter)) / Math.max(1, (data.length - 1)))),
                    titleColor = '#555',
                    subtitleColor = '#999',
                    getColor = function(d) {
                    return d.color || config.defaultColor;
                },
                getStrokeColor = function(d) {
                    var color = colorjs(getColor(d)),
                        //adjust = color.toHSL().desaturateByAmount(0.6).lightenByAmount(0.1).toString();
                        adjust = color.toHSL().lightenByAmount(0.1).desaturateByAmount(0.6).toString();
                    return adjust;
                },
                getVarianceColor = function(d) {
                    var color = colorjs(getColor(d)),
                        adjust = color.toHSL().darkenByAmount(0.05).desaturateByAmount(0.6).toString();
                    return adjust;
                },
                getVariance = function(d, i) {
                    var next = (i + 1) < data.length ? data[i + 1] : null;
                    if (next) {
                        switch (config.variance) {
                            case 'PERCENT':
                                if (d[report.mapping.value] === 0) return;
                            var percent = 1 - (next[report.mapping.value] / d[report.mapping.value]);
                            return Math.round(percent * 100);
                            case 'AMOUNT':
                                return Math.round(d[report.mapping.value] - next[report.mapping.value]);
                        }
                    }
                };

                var svg = d3.select(node.get(0)).append('svg')
                    .attr({
                        width: '100%',
                        height: '100%',
                        viewBox: null
                    });

                var group = svg.append('g')
                .attr({
                    transform: 'translate(' + 0 + ' ' + 0 + ')'
                })
                .append('g')
                .call(function() {
                    this.selectAll('g.point')
                    .data(data)
                    .call(function() {
                        this.enter().append('g')
                        .attr('class', 'point')
                        .call(function() {
                            this.append('circle')
                            .attr({
                                class: 'border',
                                r: outerRadius,
                                cx: outerRadius,
                                cy: 0
                            })
                            this.append('circle')
                            .attr({
                                class: 'inner',
                                r: radius,
                                cx: outerRadius,
                                cy: 0
                            })
                            this.append('text')
                            .attr({
                                class: 'title',
                                x: outerRadius,
                                y: radius + strokeWidth + titleDistance,
                                fill: titleColor,
                                'font-family': 'HelveticaNeue-Light',
                                'font-size': '140%',
                                'text-anchor': 'middle'
                            })
                            this.append('text')
                            .attr({
                                class: 'count',
                                fill: 'white',
                                x: outerRadius,
                                y: '.3em',
                                'font-family': 'HelveticaNeue',
                                'font-weight': 'bold',
                                'font-size': '170%',
                                'text-anchor': 'middle'
                            })
                            this.append('text')
                            .attr({
                                class: 'subtitle',
                                fill: subtitleColor,
                                x: outerRadius,
                                y: radius + strokeWidth + titleDistance,
                                dy: '1.5em',
                                'font-family': 'HelveticaNeue',
                                'font-style': 'italic',
                                'font-size': '100%',
                                'text-anchor': 'middle'
                            })
                            this.append('g')
                            .attr('transform', 'translate(' + (outerRadius * 2) + ' 0)')
                            .call(function() {
                                this.append('rect')
                                .attr({
                                    x: 0,
                                    y: strokeWidth * -1,
                                    width: distance,
                                    height: strokeWidth * 2,
                                    fill: getStrokeColor,
                                    display: function(d, i) {
                                        if (i === data.length - 1) return 'none';
                                    }
                                })
                                this.append('path')
                                .attr({
                                    transform: function(d, i) {
                                        var variance = getVariance(d, i),
                                            rotate = 180,
                                            translate = [distance / 2, strokeWidth * -5];
                                        if (variance > 0) {
                                            rotate = 0;
                                            translate[1] *= -1;
                                        }
                                        return ' translate(' + translate.join(' ') + ') rotate(' + rotate + ')';
                                    },
                                    d: 'M 0 3 l-6,-6 h 12',
                                    fill: getVarianceColor,
                                    display: function(d, i) {
                                        var variance = getVariance(d, i);
                                        if (i === data.length - 1 || config.variance === 'NONE' || !variance) return 'none'
                                    }
                                })
                                this.append('text')
                                .attr({
                                    class: 'variance',
                                    x: distance / 2,
                                    y: 0,
                                    dy: '.3em',
                                    fill: getVarianceColor,
                                    'font-family': 'HelveticaNeue-Light',
                                    'font-size': '120%',
                                    'text-anchor': 'middle'
                                })
                            })
                        })
                    })
                    .attr('transform', function(d, i) {
                        return 'translate(' + (i * (outerRadius * 2 + distance - strokeWidth / 2)) + ' ' + height / 2 + ')'
                    })
                    .on('click', self.handleClick.bind(self))
                    .call(function() {
                        this.select('circle.border')
                        .attr({
                            fill: getStrokeColor
                        })
                        this.select('circle.inner')
                        .attr({
                            fill: getColor
                        })
                        this.select('text.count')
                        .text(function(d) {
                            return d.value.count;
                        })
                        this.select('text.title')
                        .text(function(d) {
                            return self.displayName(d);
                        })
                        this.select('text.subtitle')
                        .text(function(d) {
                            return d.subtitle || '';
                        })
                        this.select('text.variance')
                        .attr('y', function(d, i) {
                            var variance = getVariance(d, i),
                                y = strokeWidth * -2.5;
                            if (variance > 0) {
                                y *= -1;
                            }
                            return y;
                        })
                        .text(function(d, i) {
                            var variance = getVariance(d, i);
                            if (!isNaN(variance)) {
                                switch (config.variance) {
                                    case 'PERCENT':
                                        return Math.abs(variance) + '%';
                                    case 'AMOUNT':
                                        return Math.abs(variance);
                                }
                            }
                        })
                    })
                })

                var box = group.node().getBoundingClientRect(),
                    svgBox = svg.node().getBoundingClientRect(),
                    padding = 30;

                svg.attr('viewBox', '0 0 ' +
                     Math.max(svgBox.width, box.width + padding) +
                     ' ' +
                     Math.max(box.height + padding, svgBox.height)
                );
            };
        }
    });

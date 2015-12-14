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

    return defineComponent(TextOverview, withRenderer);

    function countFn(d) {
        return d.value.count;
    }

    function nameFn(d) {
        return d.name;
    }

    function TextOverview() {

        this.processData = function(data) {
            return _.sortBy(data.root[0].buckets, countFn).reverse();
        };

        this.render = function renderTextOverview(d3, node, data, d3tip) {
            var self = this,
                area = node.offsetWidth * node.offsetHeight,
                config = this.attr.reportConfiguration || {},
                limit = config.limit,
                color = config.color;

            if (limit) {
                data = data.slice(0, parseInt(limit, 10));
            }

            d3.select(node)
                .selectAll('ul')
                .data([1])
                .call(function() {
                    this.enter().append('ul').attr('class', 'text-overview');
                    this.selectAll('li')
                        .data(data)
                        .call(function() {
                            this.enter().append('li').attr('class', 'clickable')
                                .call(function() {
                                    this.append('h1')
                                    this.append('h2')
                                })
                            this.exit().remove();

                            this.order()
                                .on('click', function(d) {
                                    self.handleClick({
                                        filters: [{
                                            propertyId: d.field,
                                            predicate: 'equal',
                                            values: [nameFn(d)]
                                        }]
                                    });
                                })
                                .classed('light', colorjs(color || 'white').getLuminance() > 0.5)
                                .style('background-color', color || 'white')
                            this.select('h1').text(function(d) {
                                return (d.format || d3.format(','))(countFn(d));
                            });
                            this.select('h2')
                                .text(self.displayName)
                                .attr('title', self.displayName)
                        });
                })
                .each(function() {
                    this.style.fontSize = '100%';
                    var areas = _.toArray(this.querySelectorAll('li')).map(function(li) {
                            var dim = li.getBoundingClientRect();
                            return (dim.width * 1.125) * (dim.height * 1.125);
                        }),
                        used = d3.sum(areas),
                        percent = Math.floor(Math.sqrt(area / used) * 100);

                    self.tryPercent(node, this, percent);
                })
        };

        this.tryPercent = function(node, container, percent) {
            container.style.fontSize = percent + '%';

            if (container.scrollHeight > node.offsetHeight) {
                _.defer(this.tryPercent.bind(this, node, container, percent * 0.95));
            }
        };
    }
});

define([
    'flight/lib/component',
    'configuration/admin/utils/withFormHelpers',
    'util/formatters',
    'util/withDataRequest',
    'util/withCollapsibleSections'
], function(
    defineComponent,
    withFormHelpers,
    F,
    withDataRequest,
    withCollapsibleSections) {
    'use strict';

    return defineComponent(PluginList, withDataRequest, withFormHelpers, withCollapsibleSections);

    function PluginList() {

        this.after('initialize', function() {
            var self = this;

            this.$node.html(
                '<ul class="nav nav-list">' +
                  '<li class="nav-header">Plugins<span class="badge loading"></span></li>' +
                '</ul>'
            );

            this.dataRequest('admin', 'plugins')
                .then(this.renderPlugins.bind(this))
                .catch(this.showError.bind(this))
                .finally(function() {
                    self.$node.find('.badge').remove();
                });
        });

        this.renderPlugins = function(plugins) {
            var self = this,
                $list = this.$node.empty().addClass('admin-plugin-list');

            require(['d3'], function(d3) {
                d3.select($list.get(0))
                    .selectAll('section.collapsible')
                    .data(
                        _.chain(plugins)
                        .pairs()
                        .map(function(pair) {
                            return [
                                pair[0].replace(/[A-Z]/g, function(cap) {
                                    return ' ' + cap;
                                }),
                                pair[1]
                            ];
                        })
                        .sortBy(function(pair) {
                            return pair[0].toLowerCase();
                        })
                        .value()
                    )
                    .call(function() {
                        this.enter()
                            .append('section').attr('class', 'collapsible has-badge-number')
                            .call(function() {
                                this.append('h1').attr('class', 'collapsible-header')
                                    .call(function() {
                                        this.append('span').attr('class', 'badge');
                                        this.append('strong');
                                    });
                                this.append('div').append('ol').attr('class', 'inner-list');
                            });

                        this.select('h1 strong').text(function(d) {
                            return d[0];
                        });
                        this.select('.badge').text(function(d) {
                            return F.number.pretty(d[1].length);
                        });
                        this.select('ol.inner-list')
                            .selectAll('li')
                            .data(function(d) {
                                return _.chain(d[1])
                                  .tap(function(l) {
                                    if (l.length) {
                                        l.forEach(function(d) {
                                            if (!d.name && d.className) {
                                                var lastSeparator = d.className.lastIndexOf('.');
                                                if (lastSeparator >= 0) {
                                                    d.name = d.className.substring(lastSeparator + 1);
                                                }
                                            }
                                        });
                                    }
                                    return l;
                                  })
                                  .sortBy(function(p) {
                                    return (p.name || p.className).toLowerCase();
                                  })
                                  .value();
                            })
                            .call(function() {
                                this.enter()
                                    .append('li')
                                    .call(function() {
                                        this.append('h1').attr('class', 'name');
                                        this.append('h2').attr('class', 'description');
                                        this.append('dl')
                                            .call(function() {
                                                this.append('dt').attr('class', 'fileName');
                                                this.append('dd').attr('class', 'fileName');
                                                this.append('dt').attr('class', 'className');
                                                this.append('dd').attr('class', 'className');
                                                this.append('dt').attr('class', 'projectVersion');
                                                this.append('dd').attr('class', 'projectVersion');
                                                this.append('dt').attr('class', 'builtBy');
                                                this.append('dd').attr('class', 'builtBy');
                                                this.append('dt').attr('class', 'builtOn');
                                                this.append('dd').attr('class', 'builtOn');
                                                this.append('dt').attr('class', 'gitRevision');
                                                this.append('dd').attr('class', 'gitRevision');
                                            })
                                    });

                                'name description'.split(' ')
                                    .forEach(function(key) {
                                        this.select('.' + key)
                                            .style('display', function(d) {
                                                return d[key] ? '' : 'none'
                                            })
                                            .text(_.property(key));
                                    }.bind(this));

                                'fileName className projectVersion builtBy builtOn gitRevision'.split(' ')
                                    .forEach(function(key) {
                                        this.select('.' + key)
                                            .style('display', function(d) {
                                                return d[key] ? '' : 'none'
                                            });

                                        this.select('dt.' + key).text(function(d) {
                                            return key.substring(0, 1).toUpperCase() +
                                                key.substring(1).replace(/[A-Z]/g, function(m) {
                                                return ' ' + m;
                                            })
                                        });
                                        this.select('dd.' + key).text(function(d) {
                                            if (key === 'builtOn') {
                                                return F.date.dateTimeString(d[key]);
                                            }
                                            return d[key]
                                        });
                                    }.bind(this));
                            });

                    })
                    .exit().remove();
            });
        };

    }
});

require([
    'configuration/admin/plugin',
    'util/formatters',
    'util/withDataRequest',
    'util/withCollapsibleSections',
    'configuration/plugins/registry'
], function(
    defineVisalloAdminPlugin,
    F,
    withDataRequest,
    withCollapsibleSections,
    registry) {
    'use strict';

    return defineVisalloAdminPlugin(UIExtensionList, {
        mixins: [withDataRequest, withCollapsibleSections],
        section: 'Plugin',
        name: 'UI Extensions',
        subtitle: 'Extensions Available / Usages'
    });

    function UIExtensionList() {

        this.after('initialize', function() {
            var self = this;

            this.$node.html('<ul class="nav nav-list"></ul>');

            this.renderPlugins();
        });

        this.renderPlugins = function(plugins) {
            var self = this,
                $list = this.$node.empty();

            Promise.all([
                Promise.require('d3'),
                this.dataRequest('extensionRegistry', 'get')
            ]).done(function(results) {
                var d3 = results.shift(),
                    webWorkerRegistry = _.mapObject(results.shift(), function(e) {
                        e.webWorker = true;
                        return e;
                    })

                d3.select($list.get(0))
                    .selectAll('section.collapsible')
                    .data(
                        _.chain({})
                        .extend(registry.extensionPointDocumentation())
                        .extend(webWorkerRegistry)
                        .pairs()
                        .tap(function(list) {
                            if (list.length === 0) {
                                self.$node.text('No extensions found')
                            }
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
                                    })
                                this.append('div')
                                    .call(function() {
                                        this.append('p')
                                        this.append('pre').style('font-size', '75%')
                                        this.append('ol').attr('class', 'inner-list');
                                    })
                            });

                        this.select('h1 strong').text(function(d) {
                            return d[0] + (d[1].webWorker ? ' (webworker)' : '');
                        });
                        this.select('p').html(function(d) {
                            return d[1].description;
                        })
                        this.select('pre').text(function(d) {
                            return d[1].validator.replace(/^\s*function\s*[^(*]/, 'validator');
                        })
                        this.select('.badge').text(function(d) {
                            return F.number.pretty(d[1].registered.length);
                        });
                        this.select('ol.inner-list')
                            .selectAll('li')
                            .data(function(d) {
                                return d[1].registered;
                            })
                            .call(function() {
                                this.enter()
                                    .append('li')
                                    .append('a').style('white-space', 'pre')

                                this.select('a')
                                    .text(function(d) {
                                        return d;
                                    })
                            });

                    })
                    .exit().remove();
            });
        };

    }
});

define([
    'flight/lib/component',
    'configuration/admin/utils/withFormHelpers',
    'util/formatters',
    'util/withDataRequest',
    'util/withCollapsibleSections',
    'configuration/plugins/registry',
    'beautify'
], function(
    defineComponent,
    withFormHelpers,
    F,
    withDataRequest,
    withCollapsibleSections,
    registry,
    beautify) {
    'use strict';

    return defineComponent(UIExtensionList, withDataRequest, withFormHelpers, withCollapsibleSections);

    function UIExtensionList() {

        this.after('initialize', function() {
            var self = this;

            this.renderPlugins();
        });

        this.renderPlugins = function(plugins) {
            var self = this,
                $list = this.$node.empty().text('Loading...');

            Promise.all([
                Promise.require('d3'),
                this.dataRequest('extensionRegistry', 'get')
            ]).done(function(results) {
                var d3 = results.shift(),
                    webWorkerRegistry = _.mapObject(results.shift(), function(e) {
                        e.webWorker = true;
                        return e;
                    })

                d3.select($list.empty().get(0))
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
                        .sortBy(function(pair) {
                            return pair[1].webWorker ? 1 : 0
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
                                this.append('div').attr('class', 'ui-extension-body')
                                    .call(function() {
                                        this.append('p')
                                        this.append('a')
                                            .attr('target', 'ext-docs')
                                            .attr('class', 'external-link')
                                            .text(function(d) { return 'External Documentation'; })

                                        this.append('div').attr('class', 'collapsible val')
                                            .call(function() {
                                                this.append('a')
                                                    .attr('class', 'collapsible-header')
                                                    .text(function(d) { return 'Validation Function'; })
                                                    .attr('title', 'Registered extensions must pass validation')
                                                this.append('div')
                                                    .append('pre').style('font-size', '75%')
                                            })
                                        this.append('div').attr('class', 'collapsible reg')
                                            .call(function() {
                                                this.append('a')
                                                    .attr('class', 'collapsible-header')
                                                this.append('div')
                                                    .append('ol').attr('class', 'inner-list ui-extension-list');
                                            })
                                    })
                            });

                        this.select('h1 strong').text(function(d) {
                            return d[0] + (d[1].webWorker ? ' (webworker)' : '');
                        });
                        this.select('p').html(function(d) {
                            return d[1].description;
                        })
                        this.select('a.external-link')
                            .attr('title', function(d) {
                                return 'Open external documentation for ' + d[0];
                            })
                            .style('display', function(d) {
                                if (!d[1].externalDocumentationUrl) {
                                    return 'none'
                                }
                            })
                            .attr('href', function(d) {
                                return d[1].externalDocumentationUrl;
                            })
                        this.select('pre').text(function(d) {
                            return beautify.js_beautify(d[1].validator, {
                                /*eslint camelcase:0 */
                                indent_size: 2,
                                wrap_line_length: 80
                            })
                        })
                        this.select('.badge').text(function(d) {
                            return F.number.pretty(d[1].registered.length);
                        });
                        this.select('.reg').style('display', function(d) {
                                if (d[1].registered.length === 0) return 'none';
                            })
                            .select('.collapsible-header')
                            .text(function(d) {
                                return F.string.plural(d[1].registered.length, 'plugin') + ' registered';
                            })
                        this.select('ol.inner-list')
                            .selectAll('li')
                            .data(function(d) {
                                return d[1].registered.map(replaceFunctions);
                                function replaceFunctions(object) {
                                    if (_.isString(object) && (/^FUNCTION/).test(object)) {
                                        return beautify.js_beautify(object.substring('FUNCTION'.length).toString(), { indent_size: 2});
                                    } else if (_.isArray(object)) {
                                        return _.map(object, replaceFunctions);
                                    } else if (_.isObject(object)) {
                                        return _.mapObject(object, replaceFunctions);
                                    }
                                    return object;
                                }
                            })
                            .call(function() {
                                this.enter()
                                    .append('li')
                                    .append('a').style('white-space', 'pre')

                                this.select('a')
                                    .text(function(d) {
                                        return JSON.stringify(d, null, 2).replace(/\\n/g, '\n');
                                    })
                            });

                    })
                    .exit().remove();
            });
        };

    }
});

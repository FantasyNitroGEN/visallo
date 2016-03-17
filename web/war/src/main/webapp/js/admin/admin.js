define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'hbs!./template',
    'tpl!util/alert',
    './bundled/index'
], function(
    defineComponent,
    registry,
    template,
    alertTemplate) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.admin',
        'Add admin tools to admin pane',
        function(e) {
            return (e.Component || e.componentPath) &&
                e.section && e.name && e.subtitle
        },
        'http://docs.visallo.org/extension-points/front-end/admin'
    )

    return defineComponent(AdminList);

    function AdminList() {

        this.defaultAttrs({
            listSelector: '.admin-list',
            pluginItemSelector: '.admin-list > li a',
            formSelector: '.admin-form'
        });

        this.after('initialize', function() {
            this.on(document, 'showAdminPlugin', this.onShowAdminPlugin);
            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on('click', {
                pluginItemSelector: this.onClickPluginItem
            });
            this.$node.html(template({}));
            this.update();
        });

        this.onToggleDisplay = function(event, data) {
            if (data.name === 'admin' && this.$node.closest('.visible').length === 0) {
                this.$node.find('.admin-list .active').removeClass('active');
                this.select('formSelector')
                    .hide()
                    .find('.content')
                        .teardownAllComponents()
                        .removePrefixedClasses('admin_less_cls')
                        .empty();
            }
        };

        this.onClickPluginItem = function(event) {
            event.preventDefault();
            this.trigger('showAdminPlugin', $(event.target).closest('li').data('component'));
        };

        this.onShowAdminPlugin = function(event, data) {
            var self = this;

            if (data && data.name && data.section) {
                data.name = data.name.toLowerCase();
                data.section = data.section.toLowerCase();
            }

            this.select('listSelector').find('li').filter(function() {
                    return _.isEqual($(this).data('component'), data);
                })
                .addClass('active')
                .siblings('.active')
                .removeClass('active');

            var container = this.select('formSelector'),
                form = container.resizable({
                        handles: 'e',
                        minWidth: 120,
                        maxWidth: 500,
                        resize: function() {
                            self.trigger(document, 'paneResized');
                        }
                    }).show().find('.content'),
                component = _.find(registry.extensionsForPoint('org.visallo.admin'), function(e) {
                    return e.name.toLowerCase() === data.name &&
                        e.section.toLowerCase() === data.section;
                });

            form.teardownAllComponents()
                .removePrefixedClasses('admin_less_cls')
                .empty();
            if (component) {
                (
                    component.Component ?
                        Promise.resolve(component.Component) :
                        Promise.require(component.componentPath)
                ).then(function(Component) {
                    Component.attachTo(form, data);
                    self.trigger(container, 'paneResized');
                })
            }

            this.trigger(container, 'paneResized');
        };

        this.update = function() {
            var self = this,
                extensions = registry.extensionsForPoint('org.visallo.admin');

            require(['d3'], function(d3) {
                d3.select(self.select('listSelector').get(0))
                    .selectAll('li')
                    .data(
                        _.chain(extensions)
                        .groupBy('section')
                        .pairs()
                        .sortBy(function(d) {
                            return d[0];
                        })
                        .each(function(d) {
                            d[1] = _.sortBy(d[1], 'name');
                        })
                        .flatten()
                        .value()
                    )
                    .call(function() {
                        this.exit().remove();
                        this.enter().append('li')
                            .attr('class', function(component) {
                                if (_.isString(component)) {
                                    return 'nav-header';
                                }
                            }).each(function(component) {
                                if (!_.isString(component)) {
                                    d3.select(this).append('a');
                                }
                            });

                        this.each(function(component) {
                            if (_.isString(component)) {
                                this.textContent = component;
                                return;
                            }

                            d3.select(this)
                                .attr('data-component', JSON.stringify(
                                    _.chain(component)
                                    .pick('section', 'name')
                                    .tap(function(c) {
                                        c.name = c.name.toLowerCase();
                                        c.section = c.section.toLowerCase();
                                    }).value()
                                ))
                                .select('a')
                                .call(function() {
                                    this.append('div')
                                        .attr('class', 'nav-list-title')
                                        .text(component.name)

                                    this.append('div')
                                        .attr('class', 'nav-list-subtitle')
                                        .attr('title', component.subtitle)
                                        .text(component.subtitle)
                                });
                        });
                    })

                if (extensions.length === 0) {
                    self.$node.prepend(alertTemplate({
                        warning: i18n('admin.plugins.none_available')
                    }));
                } else {
                    self.$node.children('.alert').remove();
                }
            });
        }
    }
});

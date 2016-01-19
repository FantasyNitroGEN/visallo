define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'util/popovers/withElementScrollingPositionUpdates',
    'util/withDataRequest',
    'util/withWorkspaceHelper',
    'util/promise',
    'hbs!./dashboardTpl',
    'hbs!./item',
    'hbs!./addItemTpl',
    'gridstack',
    'require',
    './registerDefaultItems',
    './reportRenderers'
], function(
    defineComponent,
    registry,
    withElementUpdates,
    withDataRequest,
    withWorkspaceHelper,
    Promise,
    template,
    itemTemplate,
    addItemTemplate,
    gridStack,
    require) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.web.dashboard.item',
        'Add items that can be placed on dashboards',
        function(e) {
            return (_.isString(e.componentPath) || _.isObject(e.report));
        }
    );

    registry.documentExtensionPoint('org.visallo.web.dashboard.reportrenderer',
        'Define custom report renderers for dashboard',
        function(e) {
            return _.isFunction(e.supportsResponse) &&
                _.isString(e.identifier) &&
                e.identifier &&
                _.isString(e.label) &&
                e.componentPath;
        }
    );

    registry.documentExtensionPoint('org.visallo.web.dashboard.layout',
        'Define dashboard layout for new workspaces',
        function(e) {
            return _.isArray(e);
        }
    );

    var reportRenderers,
        extensions,
        extensionsById,
        layouts,
        ConfigPopover,
        ignoreGridStackChange = false,
        defaultGridOptions = Object.freeze({
            width: 4,
            height: 4,
            'min-width': 2,
            'min-height': 2
        });

    return defineComponent(Dashboard, withElementUpdates, withDataRequest, withWorkspaceHelper);

    function Dashboard() {

        this.defaultAttrs({
            containerSelector: '.grid-stack',
            headerSelector: '.header input',
            refreshSelector: '.header .refresh',
            editDashboardSelector: '.edit-dashboard',
            configureItemSelector: '.grid-stack-item-content > h1 .configure',
            removeItemSelector: '.remove-item',
            gridScrollerSelector: '.grid-scroller'
        });

        this.after('initialize', function() {
            var self = this;

            reportRenderers = registry.extensionsForPoint('org.visallo.web.dashboard.reportrenderer');
            extensions = registry.extensionsForPoint('org.visallo.web.dashboard.item');
            layouts = registry.extensionsForPoint('org.visallo.web.dashboard.layout');
            extensionsById = _.indexBy(extensions, 'identifier');

            this.request = this.dataRequest.bind(this, 'dashboard');

            // TODO: make utility loading interface
            this.$node.text('Loading...');

            this.on('click', function(e) {
                if ($(e.target).is('.grid-scroller, .grid-stack, .grid-stack-item, .item-content')) {
                    self.trigger('selectObjects');
                }
            });
            this.on('click', {
                editDashboardSelector: this.onEditDashboard,
                configureItemSelector: this.onConfigure,
                removeItemSelector: this.onRemoveItem,
                refreshSelector: this.onRefresh
            });
            this.on('change keyup', {
                headerSelector: this.onChangeTitle
            });
            this.on('showError', this.onShowError);
            this.on('finishedLoading', this.onFinishedLoading);
            this.on('configureItem', this.onConfigureItem);
            this.on('configurationChanged', this.onConfigurationChanged);
            this.on('addItem', this.onAddItem);
            this.on('drag dragcreate dragstart dropcreate drop dropover dropout resizecreate resizestart resizestop', function(event) {
                event.stopPropagation();
                if (event.type === 'resizestart' || event.type === 'dragstart') {
                    self.removeAddItem();
                }
            });
            this.on(document, 'windowResize', this.onWindowResize);
            this.on(document, 'escape', this.onEscapeKey);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'dashboardRefreshData', this.onDashboardRefreshData);

            var load = _.once(this.loadWorkspaceAndListenOn.bind(this, this.onWorkspaceLoaded));
            this.on(document, 'didToggleDisplay', function(event, data) {
                if (data.name === 'dashboard' && data.visible) {
                    load();
                }
            })
        });

        this.onRefresh = function(event) {
            this.trigger('dashboardRefreshData');
        };

        this.onEscapeKey = function(event) {
            if (this.$node.hasClass('editing')) {
                this.toggleEditDashboard();
            }
        };

        this.onWindowResize = function(event) {
            this.$node.find('.item-content').trigger('reflow')
        };

        this.onDashboardRefreshData = function(event, data) {
            var self = this,
                items = data && data.identifier ?
                    _.where(this.dashboard.items, { extensionId: data.identifier }) :
                    this.dashboard.items;

            items.forEach(function(item) {
                var $gridItem = self.$node.find('.grid-stack-item').filter(function() {
                  return $(this).data('item-id') === item.id;
                });
                $gridItem.find('.item-content').trigger('refreshData');
            });
        };

        this.onFinishedLoading = function(event) {
            $(event.target).closest('.grid-stack-item').removeClass('loading-card');
        };

        this.onShowError = function(event) {
            $(event.target)
                .closest('.grid-stack-item')
                .find('.item-content')
                .empty()
                .append(
                    $('<div>')
                        .addClass('error')
                        .append(
                            $('<h1>')
                                .text(i18n('dashboard.error'))
                                .append($('<div>').text(i18n('dashboard.error.description')))
                        )
                );
        };

        this.onScroll = function(event) {
            this.$node.toggleClass('scrolled', this.select('gridScrollerSelector').scrollTop() > 4);
        };

        this.onWorkspaceUpdated = function(event, data) {
            if (data.workspace.workspaceId === this.currentWorkspaceId) {
                if (!this.dashboard.title || this.dashboard.title === this.workspaceTitle) {
                    this.$node.children('h1').find('input').val(data.workspace.title);
                    this.dashboard.title = '';
                    this.adjustHeader();
                }
                this.workspaceTitle = data.workspace.title;
            }
        };

        this.onWorkspaceLoaded = function(event, workspace) {
            this.currentWorkspaceId = workspace.workspaceId;
            this.isCreator = workspace.createdBy === visalloData.currentUser.id;
            this.loadDashboards(workspace);
        };

        this.onConfigurationChanged = function(event, data) {
            var node = $(event.target).closest('.grid-stack-item'),
                cloned = this.cloneItemConfiguration(data.item),
                changed = !_.isEqual(this.configuringItem, cloned);


            if (data.options && data.options.changed === 'item.title') {
                node.find('.grid-stack-item-content > h1 span').text(
                    data.item.title || data.extension.title
                );
            } else if (changed && data.recreate !== false) {
                this.createDashboardItemComponent(node, data.item);
            }
            if (changed) {
                this.configuringItem = cloned;
                this.request('dashboardItemUpdate', data.item);
            }
        };

        this.cloneItemConfiguration = function(item) {
            return JSON.parse(JSON.stringify(_.pick(item, 'configuration', 'title')));
        };

        this.onConfigureItem = function(event) {
            var self = this,
                $gridItem = $(event.target).closest('.grid-stack-item'),
                node = $gridItem.find('.grid-stack-item-content > h1 .configure'),
                alreadyAttached = ConfigPopover && node.lookupComponent(ConfigPopover);

            if (alreadyAttached) {
                return node.teardownComponent(ConfigPopover);
            }

            require(['./configure'], function(_ConfigPopover) {
                ConfigPopover = _ConfigPopover;
                var itemId = $gridItem.data('item-id'),
                    item = _.findWhere(self.dashboard.items, { id: itemId });

                self.configuringItem = self.cloneItemConfiguration(item);

                if (item) {
                    if (node.lookupComponent(ConfigPopover)) {
                        node.teardownComponent(ConfigPopover);
                    } else {
                        ConfigPopover.attachTo(node, {
                            item: item,
                            scrollSelector: '.grid-scroller'
                        });
                    }
                }
            })
        };

        this.onConfigure = function(event) {
            this.trigger(event.target, 'configureItem');
        };

        this.onChangeTitle = function(event) {
            if (event.type === 'change' || event.which === 13) {
                var newTitle = event.target.value.trim(),
                    previousTitle = this.dashboard.title;

                if (newTitle.length) {
                    this.dashboard.title = newTitle;
                    this.request('dashboardUpdate', {
                        dashboardId: this.dashboard.id,
                        title: newTitle
                    })
                        .catch(function() {
                            event.target.value = previousTitle;
                            this.dashboard.title = previousTitle;
                        });
                } else {
                    event.target.value = previousTitle || this.workspaceTitle;
                }
                if (event.which === 13) {
                    event.target.blur();
                    this.adjustHeader();
                }
            }
        };

        this.onEditDashboard = function(event) {
            this.toggleEditDashboard();
        };

        this.toggleEditDashboard = function() {
            var self = this,
                $edit = this.select('editDashboardSelector'),
                $header = this.select('headerSelector');

            if (this.isCreator) {
                this.gridstack.batch_update();

                var finished;
                if (this.$node.hasClass('editing')) {
                    $edit.text('Edit');
                    ignoreGridStackChange = true;
                    this.$node.find('.new-item').each(function() {
                        self.gridstack.remove_widget(this);
                    });
                    this.gridstack.disable();
                    this.$node.removeClass('editing');
                    finished = Promise.resolve();
                    $header.attr('readonly', true);
                } else {
                    $edit
                        .text(i18n('dashboard.title.editing.done'))
                        .append($('<small>').text(' ' + i18n('dashboard.title.editing.done.key')));
                    this.$node.addClass('editing');
                    this.gridstack.enable();
                    finished = this.createDashboardItemToGridStack();
                    $header.removeAttr('readonly');
                }
                this.adjustHeader();

                finished
                    .catch(function(error) {
                        console.error(error)
                    })
                    .then(function() {
                        ignoreGridStackChange = false;
                        self.gridstack.commit();
                    })
            }
        };

        this.removeAddItem = function() {
            var newItem = this.$node.find('.new-item').hide();
        };

        this.onRemoveItem = function(event) {
            var self = this,
                gridItem = $(event.target).closest('.grid-stack-item'),
                itemId = gridItem.data('item-id');

            if (itemId) {
                this.request('dashboardItemDelete', itemId)
                    .done(function() {
                        ignoreGridStackChange = true;
                        self.gridstack.batch_update();
                        self.gridstack.remove_widget(gridItem);
                        self.createDashboardItemToGridStack().then(function() {
                            self.gridstack.commit();
                            ignoreGridStackChange = false;
                        });
                    })
            }
        };

        this.onAddItem = function(event, data) {
            var self = this,
                extension = data.extension,
                node = this.createDashboardItemNode(undefined, extension),
                placeholderGridItem = $(event.target).closest('.grid-stack-item'),
                placeholderMetrics = this.metricsForGridItem(placeholderGridItem),
                width = (extension.grid && extension.grid.width) || defaultGridOptions.width,
                height = (extension.grid && extension.grid.height) || defaultGridOptions.height,
                item = {
                    extensionId: extension.identifier,
                    configuration: {
                        metrics: _.extend(placeholderMetrics, {
                            width: width,
                            height: height
                        })
                    }
                };

            ignoreGridStackChange = true;
            this.gridstack.batch_update();
            this.gridstack.remove_widget($(event.target).closest('.grid-stack-item'));
            this.gridstack.add_widget(node,
                placeholderMetrics.x,
                placeholderMetrics.y,
                placeholderMetrics.width,
                placeholderMetrics.height,
                false
            );

            Promise.resolve(this.createDashboardItemToGridStack())
                .then(function() {
                    self.gridstack.commit();
                    ignoreGridStackChange = false;
                    return self.createDashboardItemComponent(node, item)
                })
                .then(function() {
                    return self.request('dashboardItemNew', self.dashboard.id, item)
                })
                .done(function(result) {
                    item.id = result.dashboardItemId;
                    $(node).attr('data-item-id', item.id);
                });

            this.dashboard.items.push(item);
        };

        this.metricsForGridItem = function(el) {
            var node = $(el).data('_gridstack_node');
            return {
                x: node.x,
                y: node.y,
                width: node.width,
                height: node.height
            };
        };

        this.onGridChange = function(el, items) {
            if (ignoreGridStackChange) {
                return;
            }

            var self = this,
                validItems = _.reject(items, function(item) {
                    return $(item.el).hasClass('new-item');
                });

            if (!validItems.length) {
                return self.createDashboardItemToGridStackInBatch();
            }

            Promise.resolve(validItems).map(
                function(gridItem) {
                    var $gridItem = $(gridItem.el),
                        itemId = $gridItem.data('item-id'),
                        item = _.findWhere(self.dashboard.items, { id: itemId });

                    if (item) {
                        $gridItem.on(TRANSITION_END, function animationComplete(e) {
                            $gridItem
                                .off(TRANSITION_END, animationComplete)
                                .find('.item-content').trigger('reflow');
                        });

                        if (!item.configuration) {
                            item.configuration = {};
                        }
                        item.configuration.metrics = _.pick(gridItem, 'x', 'y', 'width', 'height');

                        return self.request('dashboardItemUpdate', item);
                    }
                },
                { concurrency: 1 }
            ).done(function() {
                self.createDashboardItemToGridStackInBatch();
            })
        };

        this.createDashboardItemToGridStackInBatch = function() {
            var self = this,
                stack = this.gridstack,
                begin = stack.batch_update.bind(stack),
                commit = stack.commit.bind(stack);

            return Promise.resolve()
                .then(begin)
                .then(this.createDashboardItemToGridStack.bind(this))
                .catch(function(error) {
                    console.error(error);
                })
                .then(commit)
        };

        this.createDashboardItemToGridStack = function() {
            var self = this;

            var newItem = this.$node.find('.new-item');
            if (newItem.length) {
                ignoreGridStackChange = true;
                this.gridstack.remove_widget(newItem);
            }

            return this.createNewDashboardItem()
                .then(function(newItem) {
                    ignoreGridStackChange = true;
                    self.gridstack.add_widget(newItem);
                    self.gridstack.movable(newItem, false);
                    self.gridstack.resizable(newItem, false);
                    ignoreGridStackChange = false;
                });
        };

        this.createDashboardItemComponent = function(node, item) {
            var extension = extensionsById[item.extensionId],
                path = extension.componentPath,
                isReport = false,
                reportConfiguration = {},
                report;

            if (!item.configuration) {
                item.configuration = {};
            }

            if (extension.report) {
                report = extension.report;
            }
            if (item.configuration.report) {
                report = item.configuration.report;
                path = '';
            }

            if (report) {
                var reportRendererIdentifier = (item.configuration && item.configuration.reportRenderer) || report.defaultRenderer,
                    renderer = _.find(reportRenderers, function(renderer) {
                        if (reportRendererIdentifier) {
                            return reportRendererIdentifier === renderer.identifier;
                        }
                    });

                if (renderer) {
                    if (!item.configuration[renderer.identifier]) {
                        item.configuration[renderer.identifier] = report.configuration && report.configuration[renderer.identifier] || {};
                    }
                    item.configuration.reportRenderer = renderer.identifier;
                    reportConfiguration = item.configuration[renderer.identifier];
                    path = renderer.componentPath;
                } else {
                    path = 'dashboard/reportRenderers/unknownType';
                }
            }

            if (path) {
                return Promise.require(path).then(function(Component) {
                    var $gridItem = $(node),
                        $content = $gridItem.find('.item-content')
                            .attr('class', 'item-content')
                            .removeAttr('style');

                    $gridItem.find('.grid-stack-item-content > h1 > span').text(
                        item.title || extension.title
                    );

                    if (report || (extension.options && extension.options.flushContent)) {
                        $content.addClass('flush-content');
                    }
                    $content.teardownAllComponents().empty();
                    Component.attachTo($content, {
                        reportConfiguration: reportConfiguration,
                        report: report,
                        extension: extension,
                        item: item
                    });
                    if (!report && (!extension.options || !extension.options.manuallyFinishLoading)) {
                        $gridItem.removeClass('loading-card');
                    }
                })
            } else {
                console.error('No component to render for ', extension);
                $(node).trigger('finishedLoading');
            }
        };

        this.renderItems = function() {
            if (!this.dashboard.items.length) {
                this.initializeGridStack();
                this.toggleEditDashboard();
                return;
            }

            var self = this,
                gridItems = this.select('containerSelector')
                    .html(_.chain(this.dashboard.items)
                          .map(function(item) {
                            var extension = extensionsById[item.extensionId];
                            if (extension) {
                                return self.createDashboardItemNode(item, extension);
                            }
                          })
                          .compact()
                          .value()
                   ).find('.grid-stack-item');

            this.initializeGridStack();

            Promise.all(
                $.map(gridItems, function(node) {
                    var itemId = $(node).data('item-id'),
                        item = _.findWhere(self.dashboard.items, { id: itemId });
                    return self.createDashboardItemComponent(node, item);
                })
            ).done();
        };

        this.createNewDashboardItem = function() {
            var self = this,
                node = $(addItemTemplate({
                    dataAttrs: self.gridOptions({ 'no-move': true, 'no-resize': true })
                }));

            return Promise.require('dashboard/addItem').then(function(AddItem) {
                AddItem.attachTo(node.find('.item-content'), {});
                return node;
            });
        };

        this.initializeGridStack = function() {
            var $container = this.select('containerSelector');
            this.gridstack = $container.gridstack({
                    /*eslint camelcase:0*/
                    cell_height: 60,
                    float: true,
                    animate: true,
                    vertical_margin: 40,
                    always_show_resize_handle: true,
                    width: 12
                }).on('change', this.onGridChange.bind(this))
                  .data('gridstack');

            this.gridstack.disable();
        };

        this.gridOptions = function(extended) {
            var options = _.extend({}, defaultGridOptions, extended);

            if (!('x' in options) || !('y' in options)) {
                options['auto-position'] = true;
            }

            return _.chain(options)
                .map(function(value, key) {
                    return {
                        key: 'gs-' + key,
                        value: value
                    };
                })
                .value();
        };

        this.createDashboardItemNode = function(item, extension) {
            var config = item && item.configuration || {},
                metrics = config.metrics || {};
            return $(itemTemplate({
                title: item && item.title || extension.title,
                extension: extension,
                creator: this.isCreator,
                dataAttrs: this.gridOptions(_.extend({}, extension.grid, metrics))
                    .concat([{ key: 'item-id', value: item && item.id }])
            }));
        };

        this.loadDashboards = function(workspace) {
            var self = this;

            return this.request('dashboards')
                .then(function(dashboards) {
                    if (dashboards.length) {
                        return dashboards[0];
                    }

                    if (layouts.length) {
                        if (layouts.length > 1) {
                            console.warn(layouts.length + ' org.visallo.web.dashboard.layout extensions were found.'
                            + ' Only the first one will be used.');
                        }
                        return self.requestDashboards(layouts[0]);
                    } else {
                        return Promise.require('dashboard/defaultLayout')
                            .then(function(items) {
                                return self.requestDashboards(items);
                            });
                    }
                })
                .then(function(dashboard) {
                    self.dashboard = dashboard;
                    self.workspaceTitle = workspace.title;
                    self.$node.removeClass('editing').html(template({
                        creator: self.isCreator,
                        title: dashboard.title || workspace.title,
                        workspace: workspace,
                        dashboard: self.dashboard
                    }));
                    self.adjustHeader();
                    self.on(self.select('gridScrollerSelector'), 'scroll', self.onScroll);
                    self.renderItems();
                })
                .done();
        };

        this.requestDashboards = function(items) {
            return this.request('dashboardNew', { items: items })
                .then(function(result) {
                    if (result.itemIds && result.itemIds.length === items.length) {
                        items.forEach(function(item) {
                            item.id = result.itemIds.shift();
                        })
                    }
                    return { id: result.id, items: items, title: '' };
                });
        };

        this.adjustHeader = function() {
            var $input = this.$node.find('h1.header input'),
                $span = $('<span>').text($input.val()).insertAfter($input.next('button'));

            $input.width(($span.outerWidth() + 2) + 'px');
            $span.remove();
        };
    }
});

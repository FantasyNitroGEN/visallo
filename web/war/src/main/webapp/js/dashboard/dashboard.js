define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'util/popovers/withElementScrollingPositionUpdates',
    'util/withDataRequest',
    'util/withWorkspaceHelper',
    'util/promise',
    'util/component/attacher',
    './dashboardTpl.hbs',
    './item.hbs',
    './addItemTpl.hbs',
    './extensionToolbarPopover',
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
    Attacher,
    template,
    itemTemplate,
    addItemTemplate,
    ToolbarExtensionPopover,
    gridStack,
    require) {
    'use strict';

    /**
     * Allow custom content to be rendered in a card on dashboards.
     * They can be included in defaults dashboards using the {@link org.visallo.dashboard.layout}
     * extension, or added manually by users using the "Add Item" button
     * when editing dashboards.
     *
     * The bundled items are defined in [registerDefaultItems.js](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/dashboard/registerDefaultItems.js) for examples.
     *
     * Either `componentPath` or `report` is required.
     *
     * ## Report
     *
     * Instead of specifying a component to render, specify a report
     * template that requests data from the server and passes the results
     * to a {@link org.visallo.dashboard.reportRenderer} that can handle that data. The most common
     * report uses search with aggregations configured.
     *
     * An item can be a report if either:
     * * The extension defines the `report`
     * * a component registered with `componentPath` saves a `report` inside the items configuration, e.g. `item.configuration.report = { ... }`
     *
     * @param {string} identifier Unique identifier for this type of dashboard item. Only used internally, not exposed to user.
     * @param {string} title The title shown in "Add Item" list
     * @param {string} description Shown under the `title` in "Add Item" list
     * @param {object} [report] Use Visallo reportRenderers to render a search aggregation
     * @param {string} [report.defaultRenderer] The identifier of a report renderer to use as default when adding this item.
     * @param {string} report.endpoint The endpoint path to access the data. See [`Router`](https://github.com/visallo/visallo/blob/master/web/web-base/src/main/java/org/visallo/web/Router.java) for all available endpoints.
     * @param {object} [report.endpointParameters] Parameters to pass to endpoint.
     * Parameters when using search: {@link org.visallo.dashboard.item~reportParametersForSearch}
     * @param {object} [report.mapping] Custom configuration for mapping results
     * @param {string} [report.mapping.transformerModulePath] RequireJS path to function that can transform the endpoint results to something a
     * reportRenderer can handle. Only necessary if aggregations or search aren't used.
     * @param {string} [report.clickHandlerModulePath] RequireJS path to a function that handles click events. Called with arguments: `target`, `object.`
     * @param {string} [componentPath] The path to the {@link org.visallo.dashboard.item~Component|Component}
     * to render when the user selects this item from the list.
     * @param {string} [configurationPath] The path to the {@link org.visallo.dashboard.item~ConfigComponent|ConfigComponent}.
     * Provides custom interface displayed in the configuration popover,
     * when the user clicks the gear icon in the items toolbar.
     * @param {object} [grid] Default sizing of item in grid
     * @param {number} [grid.width] Default width of item in grid units when added `1-12`
     * @param {number} [grid.height] Default height of item in grid units when added `>0`
     * @param {object} [options]
     * @param {boolean} [options.flushContent=false] By default all cards get some default content padding, settings this to `true` will remove the padding.
     * @param {boolean} [options.preventDefaultConfig=false] Set to `true` to disable the system adding a title configuration form.
     *
     * @example <caption>Report of Concept Type Counts</caption>
     * registry.registerExtension('org.visallo.dashboard.item', {
     *     title: 'Concept Type Counts',
     *     description: 'Show total counts for entity types',
     *     identifier: 'org-example-concept-counts',
     *     report: {
     *         defaultRenderer: 'org-visallo-pie',
     *         endpoint: '/vertex/search',
     *         endpointParameters: {
     *             q: '*',
     *             size: 0,
     *             filter: '[]',
     *             aggregations: [
     *                 {
     *                     type: 'term',
     *                     name: 'field',
     *                     field: 'http://visallo.org#conceptType'
     *                 }
     *             ].map(JSON.stringify)
     *         }
     *     }
     * });
     */
    registry.documentExtensionPoint('org.visallo.dashboard.item',
        'Add items that can be placed on dashboards',
        function(e) {
            return _.isString(e.identifier) &&
                (_.isString(e.componentPath) || _.isObject(e.report)) &&
                e.title &&
                e.description;
        },
        {
            url: 'http://docs.visallo.org/extension-points/front-end/dashboard/item.html',
            legacyName: 'org.visallo.web.dashboard.item'
        }
    );

    /**
     * @typedef org.visallo.dashboard.item~filter
     * @property {string} propertyName Iri of property name to filter.
     * @property {string} predicate Type of filter operation `has`, `hasNot`, `equal`, `contains`, `range`, `<`, `>`
     * @property {Array.<object>} [values] The values for the property length of `2` when range filter, usually `1`
     */

    /**
     * `term` Group by value of `field` and return counts.
     *
     * `geohash` Group by value of `field` with geohash [`precision` _(required)_](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-geohashgrid-aggregation.html) and return counts.
     *
     * `histogram` Group range (specified by [`interval` _(required)_](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-histogram-aggregation.html)) of values and their counts.
     *
     * `statistics` Statistics for property: [`min`, `max`, `count`, `average`, `sum`](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-stats-aggregation.html).
     *
     * @typedef org.visallo.dashboard.item~aggregation
     * @property {string} type Type of aggregation: `term`, `geohash`, `histogram`
     * @property {string} name Name the aggregation that's returned with results. Useful when more than one aggregation is used.
     * @property {string} field Property name iri to aggregate.
     */

    /**
     * These work for search `endpoints`
     * * `vertex/search`: Only return vertices.
     * * `edge/search`: Only return edges.
     * * `element/search`: Search both vertices and edges.
     *
     * @typedef org.visallo.dashboard.item~reportParametersForSearch
     * @property {string} q Search query, or asterisk.
     * @property {number} [size] Number of results to limit to, use `0` if using aggregations.
     * @property {number} [offset] Index of results to start.
     * @property {string} filter JSON array of {@link org.visallo.dashboard.item~filter} objects to `AND`.
     * @property {Array.<string>} [aggregations] List of {@link org.visallo.dashboard.item~aggregation} to apply, each item is json string.
     * The aggregations should be converted from objects to strings. See example.
     * @example
     * {
     *     q: '*',
     *     size: 0,
     *     filter: '[]',
     *     aggregations: [
     *         {
     *             type: 'term',
     *             name: 'field',
     *             field: 'http://visallo.org#conceptType'
     *         }
     *     ].map(JSON.stringify) // Convert each item to strings
     * }
     *
     */

    /**
     * Adds additional output types for dashboard items that define a `report` or `item.configuration.report`.
     *
     * There are several built-in renderers defined in
     * [`reportRenderers.js`](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/dashboard/reportRenderers.js).
     *
     * ## withReportRenderer Mixin
     *
     * If the renderer uses the mixin, the only function required is `render`. Optionally, a `processData` function can be defined to transform the raw server results. It's better to process the data in `processData` function instead of `render` because it will run once on `refreshData` events, instead of on every `reflow` event.
     *
     * The render function is called with four parameters `render(d3, svgNode, data, d3tip)`
     * * `d3` `[Object]` The d3 library object
     * * `node` `[DomElement]` The dom element to populate
     * * `data` `[?]` The response from the server (after processData)
     * * `d3tip` `[Object]` The d3tip library object (for tooltips)
     *
     * @param {string} identifier Unique identifier for this type of renderer. Can be referenced by dashboard report item using `defaultRenderer: [id]` in report configuration.
     * @param {string} label Shown in the configuration interface (see tutorial) in _Visualization_ section.
     * @param {function} supportsResponse Return `true` if this renderer can handle the `data` argument passed to it.
     * @param {string} componentPath RequireJS path to {@link org.visallo.dashboard.item~Component} component.
     * @param {string} [configurationPath] RequireJS path to extra configuration.
     * @example <caption>Using Mixin</caption>
     * define(['public/v1/api', 'dashboard/reportRenderers/withRenderer'], function(defineComponent, withReportRenderer) {
     *     return defineComponent(MyReportRenderer, withReportRenderer)
     *     function MyReportRenderer() {
     *         this.render = function() { ... }
     *     }
     * })
     */
    registry.documentExtensionPoint('org.visallo.dashboard.reportrenderer',
        'Define custom report renderers for dashboard',
        function(e) {
            return _.isFunction(e.supportsResponse) &&
                _.isString(e.identifier) &&
                e.identifier &&
                _.isString(e.label) &&
                e.componentPath;
        },
        {
            url: 'http://docs.visallo.org/extension-points/front-end/dashboard/report.html',
            legacyName: 'org.visallo.web.dashboard.reportrenderer'
        }
    );

    /**
     * When a new case is created or new user logs in, this will define
     * the default dashboard items and their layout. The user is able to modify it upon its creation.
     *
     * Only one extension should be registered or an error will log to console.
     * The default layout is defined in [`defaultLayout.js`](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/dashboard/defaultLayout.js).
     *
     * @param {Array} config Array of dashboard item configurations
     * @example
     * registry.registerExtension('org.visallo.dashboard.layout', [
     *     {
     *         extensionId: 'org-example-card-default',
     *         configuration: { metrics: { x: 0, y: 0, width: 6, height: 5 } }
     *     }
     * ])
     */
    registry.documentExtensionPoint('org.visallo.dashboard.layout',
        'Define dashboard layout for new cases',
        function(e) {
            return _.isArray(e);
        },
        {
            url: 'http://docs.visallo.org/extension-points/front-end/dashboard/layout.html',
            legacyName: 'org.visallo.web.dashboard.layout'
        }
    );

    /**
     * Allows custom buttons to be rendered next to the cards configuration button.
     * These buttons (displayed as icons) can send an event on click,
     * or specify content to be rendered in a popover.
     *
     * @param {string} identifier Unique identifier for this type of toolbar item. Only used internally, not exposed to user
     * @param {string} icon Path to icon to render in button
     * @param {object} action The type of action when clicked
     * @param {string} action.type Must be either `popover`, or `event`
     * @param {string} action.type Must be either `popover`, or `event`
     * @param {string} [action.componentPath] Required when `type=popover`.
     * Path to {@link org.visallo.dashboard.toolbar.item~Component|Component} to render in popover
     * @param {string} [action.name] Required when `type=event`. Event to trigger
     * @param {string} [tooltip] Help text to display when user hovers over button
     * @param {org.visallo.dashboard.toolbar.item~canHandle} [canHandle] Function to decide
     * if this item should be added to this card
     */
    registry.documentExtensionPoint('org.visallo.dashboard.toolbar.item',
        'Define toolbar items for dashboard cards',
        function(e) {
            return e.identifier && e.icon && e.action && (
                (e.action.type === 'popover' && e.action.componentPath) ||
                e.action.type === 'event'
            );
        },
        'http://docs.visallo.org/extension-points/front-end/dashboard/toolbar.html'
    );

    var reportRenderers,
        toolbarExtensions,
        toolbarExtensionsById,
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
            itemContentSelector: '.grid-stack-item-content > .item-content',
            configureItemSelector: '.grid-stack-item-content > h1 .configure',
            removeItemSelector: '.remove-item',
            gridScrollerSelector: '.grid-scroller',
            toolbarExtensionSelector: '.card-toolbar-item'
        });

        this.after('initialize', function() {
            var self = this;

            reportRenderers = registry.extensionsForPoint('org.visallo.dashboard.reportrenderer');
            extensions = registry.extensionsForPoint('org.visallo.dashboard.item');
            layouts = registry.extensionsForPoint('org.visallo.dashboard.layout');
            toolbarExtensions = registry.extensionsForPoint('org.visallo.dashboard.toolbar.item');
            toolbarExtensionsById = _.indexBy(toolbarExtensions, 'identifier');
            extensionsById = _.indexBy(extensions, 'identifier');

            this.request = this.dataRequest.bind(this, 'dashboard');

            // TODO: make utility loading interface
            this.$node.text('Loading...');

            this.on('click', function(e) {
                if ($(e.target).closest('.header, .grid-scroller, .grid-stack, .grid-stack-item, .item-content').length) {
                    self.trigger('selectObjects');
                }
            });
            this.on('click', {
                editDashboardSelector: this.onEditDashboard,
                configureItemSelector: this.onConfigure,
                removeItemSelector: this.onRemoveItem,
                refreshSelector: this.onRefresh,
                toolbarExtensionSelector: this.onToolbarExtensionClick
            });
            this.on('change keyup', {
                headerSelector: this.onChangeTitle
            });
            this.on('addItem', this.onAddItem);
            this.on('configurationChanged', {
                configureItemSelector: function(event, data) {
                    event.stopPropagation();
                    this.onConfigurationChanged({ node: () => event.target }, data);
                }
            })
            this.on('drag dragcreate dragstart dropcreate drop dropover dropout resizecreate resizestart resizestop', function(event) {
                if ($(event.target).is('.grid-stack-item')) {
                    event.stopPropagation();
                    if (event.type === 'resizestart' || event.type === 'dragstart') {
                        self.removeAddItem();
                    }
                }
            });
            this.on(document, 'windowResize', this.onWindowResize);
            this.on(document, 'escape', this.onEscapeKey);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'dashboardRefreshData', this.onDashboardRefreshData);

            var load = _.once(this.loadWorkspaceAndListenOn.bind(this, this.onWorkspaceLoaded));
            this.on(document, 'didToggleDisplay', function(event, data) {
                if (data.name === 'dashboard' && data.visible) {
                    this.adjustHeader();
                    load();
                    this.$node.find('.item-content').trigger('reflow');
                }
            })
        });

        this.onToolbarExtensionClick = function(event) {
            var $item = $(event.target).closest('.card-toolbar-item'),
                $button = $item.find('button'),
                extensionId = $item.data('identifier'),
                extension = toolbarExtensionsById[extensionId],
                itemId = $item.closest('.grid-stack-item').attr('data-item-id'),
                item = _.findWhere(this.dashboard.items, { id: itemId });

            if (extension && item) {
                switch (extension.action && extension.action.type || '') {
                  case 'popover':
                    this.toggleExtensionPopover($button, item, extension);
                    break;

                  case 'event':
                    $item.trigger(extension.action.name, {
                        item: item,
                        extension: extension
                    });
                    break;

                  default:
                      throw new Error('Unknown action for toolbar item extension: ' + JSON.stringify(extension));
                }
            } else throw new Error('Extension not found for toolbar item: ' + event.target);
        };

        this.onRefresh = function(event) {
            this.trigger('dashboardRefreshData');
        };

        this.onEscapeKey = function(event) {
            if (this.$node.hasClass('editing')) {
                this.toggleEditDashboard();
            }
        };

        this.onWindowResize = function(event) {

            /**
             * Dashboard requests card to reflow because its container size has changed.
             *
             * Consider using `_.throttle` or `_.debounce` to limit the cost of many reflow events from user resizing.
             *
             * @event org.visallo.dashboard.item#reflow
             */
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
                /**
                 * The dashboard includes a refresh button in the top left.
                 * When activited, needs to notify cards to update content if necessary.
                 *
                 * @event org.visallo.dashboard.item#refreshData
                 */
                $gridItem.find('.item-content').trigger('refreshData');
            });
        };

        this.onFinishedLoading = function(attacher) {
            $(attacher.node()).closest('.grid-stack-item').removeClass('loading-card');
        };

        this.onShowError = function(attacher) {
            $(attacher.teardown().node())
                .html(
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
            var self = this;
            if (data.workspace.workspaceId === this.currentWorkspaceId) {
                if (!this.dashboard.title || (this.dashboard.title === this.workspaceTitle && this.dashboard.title !== data.workspace.title)) {
                    var previousTitle = this.dashboard.title;
                    var newTitle = this.dashboard.title = data.workspace.title;
                    this.$node.children('h1').find('input').val(newTitle);
                    this.adjustHeader();
                    this.requestTitleChange(newTitle)
                        .catch(function() {
                            self.dashboard.title = previousTitle;
                        });
                }
                this.workspaceTitle = data.workspace.title;
            }
        };

        this.onWorkspaceLoaded = function(event, workspace) {
            this.currentWorkspaceId = workspace.workspaceId;
            this.isCreator = workspace.createdBy === visalloData.currentUser.id;
            this.loadDashboards(workspace);
        };

        this.onConfigurationChanged = function(attacher, data) {
            var node = $(attacher.node()).closest('.grid-stack-item'),
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
                if (!data.extension) {
                    data.extension = extensionsById[data.item.extensionId];
                }
                this.updateToolbarExtensions(node, data.item, data.extension);
                this.configuringItem = cloned;
                var index = _.findIndex(this.dashboard.items, (i) => i.id === data.item.id);
                if (index >= 0) {
                    this.dashboard.items.splice(index, 1, data.item);
                }
                this.request('dashboardItemUpdate', data.item);
            }
        };

        this.cloneItemConfiguration = function(item) {
            return JSON.parse(JSON.stringify(_.pick(item, 'configuration', 'title')));
        };

        this.onConfigureItem = function(attacher) {
            var self = this,
                $gridItem = $(attacher.node()).closest('.grid-stack-item'),
                configureNode = $gridItem.find('.grid-stack-item-content > h1 .configure'),
                alreadyAttached = ConfigPopover && configureNode.lookupComponent(ConfigPopover);

            if (alreadyAttached) {
                _.defer(function() {
                    configureNode.teardownComponent(ConfigPopover);
                });
                return;
            }

            require(['./configure'], function(_ConfigPopover) {
                ConfigPopover = _ConfigPopover;
                var itemId = $gridItem.data('item-id'),
                    item = _.findWhere(self.dashboard.items, { id: itemId });

                self.configuringItem = self.cloneItemConfiguration(item);

                if (item) {
                    if (configureNode.lookupComponent(ConfigPopover)) {
                        configureNode.teardownComponent(ConfigPopover);
                    } else {
                        ConfigPopover.attachTo(configureNode, {
                            item: item,
                            scrollSelector: '.grid-scroller'
                        });
                        self.trigger('positionDialog');
                    }
                }
            })
        };

        this.onConfigure = function(event) {
            this.onConfigureItem({ node: () => event.target });
        };

        this.toggleExtensionPopover = function($toolbarButton, item, toolbarExtension) {
            var self = this;

            if ($toolbarButton.lookupComponent(ToolbarExtensionPopover)) {
                _.defer(function() {
                    $toolbarButton.teardownAllComponents();
                });
                return;
            }

            Promise.require(toolbarExtension.action.componentPath)
                .then(function(Component) {
                    ToolbarExtensionPopover.attachTo($toolbarButton, {
                        Component: Component,
                        item: item,
                        extension: extensionsById[item.extensionId],
                        scrollSelector: '.grid-scroller'
                    })
                })
        };

        this.requestTitleChange = function(newTitle) {
            return this.request('dashboardUpdate', {
                dashboardId: this.dashboard.id,
                title: newTitle
            });
        };

        this.onChangeTitle = function(event) {
            var self = this;
            if (event.type === 'change' || event.which === 13) {
                var newTitle = event.target.value.trim().replace(/\s+/g, ' '),
                    previousTitle = this.dashboard.title;

                if (newTitle.length) {
                    this.dashboard.title = newTitle;
                    this.$node.find('h1.header input').val(newTitle);
                    this.requestTitleChange(newTitle)
                        .catch(function() {
                            event.target.value = previousTitle;
                            self.dashboard.title = previousTitle;
                        });
                } else {
                    event.target.value = previousTitle || this.workspaceTitle;
                }
                if (event.which === 13) {
                    event.target.blur();
                    this.adjustHeader();
                }
            } else if (event.which === $.ui.keyCode.ESCAPE) {
                this.onEscapeKey(event);
            } else if (event.type === 'keyup') {
                this.adjustHeader();
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
                    $header.attr('disabled', true);
                } else {
                    $edit
                        .text(i18n('dashboard.title.editing.done'))
                        .append($('<small>').text(' ' + i18n('dashboard.title.editing.done.key')));
                    this.$node.addClass('editing');
                    this.gridstack.enable();
                    finished = this.createDashboardItemToGridStack();
                    $header.removeAttr('disabled');
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
                $content = gridItem.find('.item-content'),
                itemId = gridItem.data('item-id');

            if (itemId) {
                this.request('dashboardItemDelete', itemId)
                    .done(function() {
                        ignoreGridStackChange = true;
                        self.gridstack.batch_update();
                        Attacher().node($content).teardown();
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
                    self.dashboard.items.push(item);
                    self.updateAllDashboardItems();
                });
        };

        this.updateAllDashboardItems = function(){
            var self = this,
                stackItemMap = {},
                nodes = _.reject($(this.$node).find('.grid-stack-item'), function(item) {
                  return $(item.el).hasClass('new-item');
                });

            _.each(nodes, function(item){
                var id = $(item).data('itemId');
                stackItemMap[id] = item;
            });

            _.each(self.dashboard.items, function(item){
                var id = item.id;
                item.configuration.metrics = self.metricsForGridItem(stackItemMap[id]);
                self.request('dashboardItemUpdate', item);
            });
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
            )
            .then(this.updateAllDashboardItems.bind(this))
            .done(this.createDashboardItemToGridStackInBatch.bind(this));
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
            var self = this,
                extension = extensionsById[item.extensionId],
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

                    /**
                     * FlightJS or React component that renders the card
                     * content.
                     *
                     * For Flight, `trigger` an event with the name of the
                     * function instead of invoking directly.
                     *
                     * @typedef org.visallo.dashboard.item~Component
                     * @property {object} extension
                     * @property {object} item
                     * @property {object} [reportConfiguration]
                     * @property {object} [report]
                     * @property {function} showError Render a generic error instead of this component
                     * @property {function} finishedLoading Notify that content is ready (removes loading spinner)
                     * @property {function} configureItem Open the configuration popover for this card
                     * @property {org.visallo.dashboard.item~configurationChanged} configurationChanged Change the configuration
                     * @listens org.visallo.dashboard.item#reflow
                     * @listens org.visallo.dashboard.item#refreshData
                     * @example <caption>React Notify Finished</caption>
                     * componentDidMount() {
                     *     this.props.finishedLoading();
                     * }
                     * @example <caption>React Change Configuration</caption>
                     * handleClick() {
                     *     const { item, extension } = this.props;
                     *     const updated = getUpdatedConfiguration();
                     *     this.props.configurationChanged({
                     *         item: { ...item, configuration: updated },
                     *         extension
                     *     });
                     * }
                     * @example <caption>FlightJS Notify Finished</caption>
                     * this.after('initialize', function() {
                     *     this.trigger('finishedLoading');
                     * })
                     * @example <caption>FlightJS Change Configuration</caption>
                     * this.after('initialize', function() {
                     *    this.on('click', function() {
                     *        // ... update item.configuration
                     *        this.trigger('configurationChanged', {
                     *            item: item,
                     *            extension: extension
                     *        });
                     *    })
                     * })
                     */
                    Attacher().node($content)
                        .component(Component)
                        .params({
                            reportConfiguration: reportConfiguration,
                            report: report,
                            extension: extension,
                            item: item
                        })
                        .behavior({
                            showError: self.onShowError.bind(self),
                            finishedLoading: self.onFinishedLoading.bind(self),
                            configureItem: self.onConfigureItem.bind(self),

                            /**
                             * @callback org.visallo.dashboard.item~configurationChanged
                             * @param {object} data
                             * @param {object} data.item
                             * @param {object} data.extension
                             * @param {object} [data.options]
                             * @param {string} [data.options.changed] What key changed
                             * (if `item.title` only the card title is updated)
                             */
                            configurationChanged: self.onConfigurationChanged.bind(self)
                        })
                        .attach({ teardown: true, empty: true, legacyFlightEventsNode: $gridItem });
                    if (!report && (!extension.options || !extension.options.manuallyFinishLoading)) {
                        $gridItem.removeClass('loading-card');
                    }
                    self.updateToolbarExtensions($gridItem, item, extension);
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
                metrics = config.metrics || {},
                dom = $(itemTemplate({
                    title: item && item.title || extension.title,
                    extension: extension,
                    creator: this.isCreator,
                    dataAttrs: this.gridOptions(_.extend({}, extension.grid, metrics))
                        .concat([{ key: 'item-id', value: item && item.id }])
                }));
            return dom;
        };

        this.updateToolbarExtensions = function(el, item, extension) {
            var validExtensions = _.reject(toolbarExtensions, function(e) {

                    /**
                     * @callback org.visallo.dashboard.toolbar.item~canHandle
                     * @param {object} options
                     * @param {object} options.item The dashboard item
                     * @param {object} options.extension The dashboard item extension
                     * @param {Element} options.element The `grid-stack-item` element
                     * @returns {boolean} `true` if extension should apply to this item
                     */
                    return _.isFunction(e.canHandle) &&
                        !e.canHandle({
                            item: item,
                            extension: extension,
                            element: _.isElement(el) ? el : el.get(0)
                        });
                }),
                $toolbar = $(el).find('.card-toolbar'),
                $configureLi = $toolbar.find('.configure').closest('li'),
                $extensionRows = $.map(validExtensions, function(e) {
                    return $('<li>')
                        .addClass('card-toolbar-item')
                        .attr('data-identifier', e.identifier)
                        .append($('<button>').attr('title', e.tooltip).css('background-image', 'url(' + e.icon + ')'))
                });

            $toolbar.find('.card-toolbar-item').remove();
            if ($configureLi.length) {
                $configureLi.before($extensionRows);
            } else {
                $toolbar.prepend($extensionRows);
            }
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
                            console.warn(layouts.length + ' org.visallo.dashboard.layout extensions were found.'
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
                    self.select('itemContentSelector').each((i, node) => Attacher().node(node).teardown());
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
                $span = $('<span>')
                    .text($input.val())
                    .css('white-space', 'pre')
                    .insertAfter($input.next('button')),
                outerWidth = $span.outerWidth();

            if (outerWidth) {
                $input.width((outerWidth + 2) + 'px');
                $span.remove();
            }
        };
    }
});

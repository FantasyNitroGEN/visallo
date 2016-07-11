define([
    'react',
    'react-dom',
    'util/withDataRequest',
    './DashboardHeader',
    './DashboardContent',
    './Loading'
], function(React, ReactDom, {dataRequest}, DashboardHeader, DashboardContent, Loading) {
    'use strict';

    const WindowResizeThrottleMs = 250;
    const dashboards = _.memoize(function() {
        return request('dashboards');
    }, function(workspaceId) {
        if (!workspaceId) throw new Error('Pass workspace id to correctly memoize the dashboard');
        return workspaceId;
    });
    const PropTypes = React.PropTypes;
    const Dashboard = React.createClass({
        propTypes: {},
        getInitialState() {
            return {
                editing: false,
                contentHeight: 0
            }
        },
        componentWillUnmount() {
            this.unload();
        },
        componentDidMount() {
            this.store.addStoreListeners('workspace')
            this.load(this.props.store);
        },
        componentWillReceiveProps(props) {
            if (props.store.workspace) {
                if (!this.request || !this.props.store.workspace || props.store.workspace.workspaceId !== this.props.store.workspace.workspaceId) {
                    this.load(props.store);
                }
            }
        },
        unload() {
            if (this.request) {
                this.request.cancel();
            }
            this.unlistenForChanges();
            this.unregisterWindowResize();
        },
        load(store) {
            var self = this;
            if (store.workspace) {
                this.unload();
                this.loadDashboard(store.workspace.workspaceId)
                    .then(function() {
                        self.listenForChanges();
                        self.registerWindowResize();
                    });
            }
        },
        onToggleEdit() {
            this.setState({ editing: !this.state.editing });
        },
        onItemChanged(changedItem) {
            var item = _.findWhere(this.state.dashboard.items, { id: changedItem.id });
            if (item) {
                Object.assign(item.configuration.metrics, _.pick(changedItem, 'x', 'y'));
                item.configuration.metrics.width = changedItem.w;
                item.configuration.metrics.height = changedItem.h;
                this.setState({ dashboard: this.state.dashboard })
            }
        },
        render() {
            var self = this,
                state = this.state,
                store = this.props.store;

            if (!state.dashboard || !store.workspace) {
                return (<Loading message="Loading Dashboard" />)
            }

            return (
                <div className="dashboard-container">
                    <DashboardHeader
                        editing={this.state.editing}
                        title={store.workspace.title}
                        onToggleEdit={this.onToggleEdit}
                        onRefresh={this.onRefresh} />

                    <DashboardContent
                        ref={this.dashboardContentRef}
                        items={state.dashboard.items}
                        height={state.contentHeight}
                        onItemChanged={this.onItemChanged} />
                </div>
            )
        },
        dashboardContentRef(dashboardContent) {
            this._dashboardContentEl = ReactDom.findDOMNode(dashboardContent);
            this.updateDimensions();
        },
        updateDimensions() {
            if (this._dashboardContentEl) {
                this.setState({ contentHeight: this._dashboardContentEl.clientHeight })
            }
        },
        registerWindowResize() {
            this.updateDimensionsThrottled = _.debounce(this.updateDimensions, WindowResizeThrottleMs);
            window.addEventListener('resize', this.updateDimensionsThrottled);
        },
        unregisterWindowResize() {
            window.removeEventListener('resize', this.updateDimensionsThrottled);
        },
        loadDashboard(workspaceId) {
            var self = this;
            this.request = dashboards(workspaceId);
            return this.request.then(function(dashboards) {
                if (dashboards.length) {
                    return dashboards[0];
                }
                var layouts = self.props.store.extensions.extensionsForPoint('org.visallo.web.dashboard.layout');
                if (layouts.length) {
                    if (layouts.length > 1) {
                        console.warn(layouts.length + ' org.visallo.web.dashboard.layout extensions were found.'
                            + ' Only the first one will be used.');
                    }
                    return createDashboard(layouts[0]);
                } else {
                    return Promise.require('dashboard/defaultLayout')
                        .then(function(items) {
                            return createDashboard(items);
                        });
                }
            }).then(function(dashboard) {
                self.setState({ dashboard: dashboard })
            })
        },
        unlistenForChanges() {
            $(document).off('dashboardItemUpdated', this.dashboardItemUpdated);
        },
        listenForChanges() {
            $(document).on('dashboardItemUpdated', this.dashboardItemUpdated);
        },
        dashboardItemUpdated: function(event, data) {
            var dashboard = this.state.dashboard,
                index = _.findIndex(dashboard.items, function(i) {
                    return i.id === data.dashboardItem.dashboardItemId
                }),
                item;

            if (data.deleted) {
                if (index >= 0) {
                    dashboard.items.splice(index, 1);
                    this.setState({ dashboard: dashboard })
                }
            } else if (index >= 0) {
                item = Object.assign({}, dashboard.items[index], _.pick(data.dashboardItem, 'title', 'configuration'))
                dashboard.items.splice(index, 1, item);
                this.setState({ dashboard: dashboard })
            } else {
                item = _.pick(data.dashboardItem, 'title', 'configuration');
                item.id = data.dashboardItem.dashboardItemId;
                dashboard.items.push(item);
                this.setState({ dashboard: dashboard })
            }
        }
    });

    return Dashboard;


    function request(method, ...args) {
        return dataRequest.apply(null, ['dashboard', method].concat(args));
    }

    function createDashboard(items) {
        return request('dashboardNew', { items: items })
            .then(function(result) {
                if (result.itemIds && result.itemIds.length === items.length) {
                    items.forEach(function(item) {
                        item.id = result.itemIds.shift();
                    })
                }
                return { id: result.id, items: items, title: '' };
            });
    }
});

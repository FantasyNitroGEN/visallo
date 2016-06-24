define([
    'react'
], function(React) {
    'use strict';

    const DetailKeys = 'fileName className projectVersion builtBy builtOn gitRevision'.split(' ');
    const TypeDateKeys = 'builtOn'.split(' ');
    const PropTypes = React.PropTypes;

    const PluginItem = function({item, api}) {
        var details = DetailKeys.map(function(key) {
                    if (_.contains(TypeDateKeys, key) && (key in item)) {
                        item[key] = api.formatters.date.dateTimeString(item[key]);
                    }
                    return { display: formatKeyForDisplay(key), value: item[key] }
                })
                .filter(function(item) {
                    return !!item.value;
                })

        return (
            <li>
                <h1 className="name">{item.name}</h1>
                <h2 className="description">{item.description}</h2>
                <dl>
                {details.map(function({display, value}) {
                    return [<dt>{display}</dt>, <dd title={value}>{value}</dd>]
                })}
                </dl>
            </li>
        )
    };

    const PluginSection = React.createClass({
        getInitialState: function() {
            return { expanded: false }
        },
        toggleCollapsed: function(event) {
            this.setState({ expanded: !this.state.expanded })
        },
        render: function() {
            var api = this.props.api,
                pluginItems = this.props.items.map(function(item) {
                    var key = item.className || item.fileName || item.name;
                    return <PluginItem api={api} key={key} item={item} />
                }),
                sectionClassName = 'collapsible has-badge-number',
                name = formatKeyForDisplay(this.props.name);
            if (this.props.items.length === 0) {
                sectionClassName += ' disabled'
            }
            if (this.state.expanded) {
                sectionClassName += ' expanded';
            }

            return (
                <section onClick={this.toggleCollapsed} className={sectionClassName}>
                    <h1 className="collapsible-header">
                        <span className="badge">{this.props.items.length}</span>
                        <strong title={name}>{name}</strong>
                    </h1>
                    <div>
                        <ol className="inner-list">
                            {pluginItems}
                        </ol>
                    </div>
                </section>
            )
        },
        propTypes: {
            name: PropTypes.string.isRequired,
            items: PropTypes.array.isRequired
        }
    });

    const PluginList = React.createClass({
        getInitialState: function() {
            return {
                loading: true,
                plugins: []
            }
        },
        componentWillUnmount: function() {
            this.request.cancel();
        },
        componentDidMount: function() {
            var self = this;
            this.request = this.props.visalloApi.v1.dataRequest('admin', 'plugins')
                .then(function(plugins) {
                    self.setState({ loading: false, plugins: plugins })
                })
                .catch(function(error) {
                    self.setState({ loading: false, error: error.statusText || error.message })
                })
        },
        render: function() {
            var state = this.state,
                api = this.props.visalloApi.v1;

            if (state.loading) {
                return (
                    <ul className="nav nav-list">
                      <li className="nav-header">Plugins<span className="badge loading"></span></li>
                    </ul>
                )
            } else if (state.error) {
                return (
                    <ul className="nav nav-list">
                      <li className="nav-header">{state.error}</li>
                    </ul>
                )
            }
            var pluginSections = _.sortBy(Object.keys(state.plugins), function(name) {
                    return name.toLowerCase();
                }).map(function(plugin) {
                    return <PluginSection api={api} key={plugin} name={plugin} items={state.plugins[plugin]} />;
                });
            return (
                <div className="admin-plugin-list">
                  {pluginSections}
                </div>
            );
        }
    });

    return PluginList;

    function formatKeyForDisplay(key) {
        if (key.length > 1) {
            return key.substring(0, 1).toUpperCase() + key.substring(1).replace(/[A-Z]/g, function(cap) {
                return ' ' + cap;
            });
        }
        return key;
    }
});


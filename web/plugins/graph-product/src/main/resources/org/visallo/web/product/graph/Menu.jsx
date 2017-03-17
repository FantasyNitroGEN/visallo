define([
    'react',
    'util/withContextMenu',
    'util/formatters',
    './node_modules/cytoscape/src/index',
    'components/RegistryInjectorHOC'
], function(React, withContextMenu, F, cytoscape, RegistryInjectorHOC) {
    'use strict';

    const EXTENSION_EXPORT = 'org.visallo.graph.export';
    const EXTENSION_SELECT = 'org.visallo.graph.selection';
    const EXTENSION_LAYOUT = 'org.visallo.graph.layout';

    const PropTypes = React.PropTypes;
    const Menu = React.createClass({
        propTypes: {
            event: PropTypes.shape({
                originalEvent: PropTypes.shape({
                    pageX: PropTypes.number,
                    pageY: PropTypes.number
                })
            }),
            editable: PropTypes.bool
        },
        componentDidMount() {
            const menu = this.refs.menu;
            const mixin = new withContextMenu();
            mixin.$node = $(menu).parent();
            mixin.node = mixin.$node[0];
            mixin.bindContextMenuClickEvent = function() { };
            mixin.toggleMenu({ positionUsingEvent: this.props.event }, $(this.refs.dropdownMenu));
            this.mixin = mixin;
        },
        componentWillReceiveProps(nextProps) {
            this.mixin.toggleMenu({ positionUsingEvent: nextProps.event }, $(this.refs.dropdownMenu));
        },
        render() {
            const { cy, registry, editable } = this.props;
            const hasSelection = cy.nodes().filter(':selected').length > 0;

            return (
                <div ref="menu" onMouseDown={e => e.stopPropagation()}>
                <ul ref="dropdownMenu" className="dropdown-menu" role="menu">
                    <li className="requires-EDIT"><a onMouseUp={this.props.onEvent} className="has-shortcut" data-func="CreateVertex" tabIndex="-1" href="#">{i18n('graph.contextmenu.create_vertex')}<span className="shortcut">{F.string.shortcut('alt+n')}</span></a></li>

                    <li className="divider requires-EDIT"></li>

                    <li><a onMouseUp={this.props.onEvent} className="has-shortcut" data-func="FitToWindow" tabIndex="-1" href="#">{i18n('graph.contextmenu.fit_to_window')}<span className="shortcut">{F.string.shortcut('alt+f')}</span></a></li>

                    <li className="dropdown-submenu selectors">
                    <a onMouseUp={this.props.onEvent} tabIndex="-1" href="#">{i18n('graph.contextmenu.select')}</a>
                    <ul className="dropdown-menu">
                        <li><a onMouseUp={this.props.onEvent} className="has-shortcut" data-func="Select" data-args='["all"]' tabIndex="-1" href="#">{i18n('graph.contextmenu.select.all')}<span className="shortcut">{F.string.shortcut('meta+a')}</span></a></li>
                        <li><a onMouseUp={this.props.onEvent} className="has-shortcut" data-func="Select" data-args='["none"]' tabIndex="-1" href="#">{i18n('graph.contextmenu.select.none')}<span className="shortcut">{F.string.shortcut('esc')}</span></a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Select" data-args='["invert"]' tabIndex="-1" href="#">{i18n('graph.contextmenu.select.invert')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Select" data-args='["vertices"]' tabIndex="-1" href="#">{i18n('graph.contextmenu.select.vertices')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Select" data-args='["edges"]' tabIndex="-1" href="#">{i18n('graph.contextmenu.select.edges')}</a></li>
                        {registry[EXTENSION_SELECT].length ?
                            _.compact(registry[EXTENSION_SELECT].map(e => {
                                if ((hasSelection && _.contains(['always', 'selected'], e.visibility)) ||
                                    (!hasSelection && _.contains(['always', 'none-selected'], e.visibility))) {
                                        return (
                                            <li key={e.identifier} className="plugin">
                                                <a onMouseUp={this.props.onEvent} href="#" tabIndex="-1" data-func="Select" data-args={JSON.stringify([e.identifier])}>
                                                    {i18n(`graph.selector.${e.identifier}.displayName`)}
                                                </a>
                                            </li>
                                        );
                                    }
                            })) : null
                        }

                    </ul>
                    </li>

                    {editable ? (
                    <li className="dropdown-submenu layouts requires-EDIT">
                    <a onMouseUp={this.props.onEvent} tabIndex="-1" href="#">{i18n('graph.contextmenu.layout')}</a>
                    <ul className="dropdown-menu">
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["circle", {}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.circle')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["bettergrid", {}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.grid')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["breadthfirst", {}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.hierarchical')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["cose", {}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.force_directed')}</a></li>
                        {this.renderLayoutExtensions(false)}
                    </ul>
                    </li>
                    ) : null}

                    {editable && hasSelection ? (
                    <li className="dropdown-submenu layouts-multi requires-EDIT">
                    <a onMouseUp={this.props.onEvent} tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.selection')}</a>
                    <ul className="dropdown-menu">
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["circle",{"onlySelected":true}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.circle')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["bettergrid", {"onlySelected":true}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.grid')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} data-func="Layout" data-args='["cose", {"onlySelected":true}]' tabIndex="-1" href="#">{i18n('graph.contextmenu.layout.force_directed')}</a></li>
                        {this.renderLayoutExtensions(true)}
                    </ul>
                    </li>
                    ) : null}

                    {editable && hasSelection ? (
                            <li><a onMouseUp={this.props.onEvent} data-func="CollapseSelectedVertices" tabIndex="-1" href="#">{i18n('graph.contextmenu.collapse')}</a></li>
                    ) : null}

                    <li className="dropdown-submenu">
                    <a onMouseUp={this.props.onEvent} tabIndex="-1" href="#">{i18n('graph.contextmenu.zoom')}</a>
                    <ul className="dropdown-menu">
                        <li><a onMouseUp={this.props.onEvent} onMouseUp={this.props.onEvent} data-func="Zoom" data-args="[2]" tabIndex="-1">{i18n('graph.contextmenu.zoom.x2')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} onMouseUp={this.props.onEvent} data-func="Zoom" data-args="[1]" tabIndex="-1">{i18n('graph.contextmenu.zoom.x1')}</a></li>
                        <li><a onMouseUp={this.props.onEvent} onMouseUp={this.props.onEvent} data-func="Zoom" data-args="[0.5]" tabIndex="-1">{i18n('graph.contextmenu.zoom.half')}</a></li>
                    </ul>
                    </li>

                    {registry[EXTENSION_EXPORT].length ? (<li className="divider" />) : null}
                    {registry[EXTENSION_EXPORT].length === 1 ? this.renderExportExtensions() : null}
                    {registry[EXTENSION_EXPORT].length > 1 ? (
                        <li className="dropdown-submenu">
                            <a>{i18n('graph.contextmenu.export')}</a>
                            <ul className="dropdown-menu">{this.renderExportExtensions()}</ul>
                        </li>
                    ) : null}

                </ul>
                </div>
            )
        },

        renderExportExtensions() {
            return _.sortBy(this.props.registry[EXTENSION_EXPORT], 'menuItem')
                .map(e => (
                    <li key={e.componentPath} className="exporter">
                        <a onMouseUp={this.props.onEvent}
                           data-func="Export"
                           data-args={JSON.stringify([e.componentPath])}
                           href="#">{e.menuItem}</a>
                    </li>
                ))
        },

        renderLayoutExtensions(onlySelected) {
            const display = e => i18n('graph.layout.' + e.identifier + '.displayName');
            return _.sortBy(this.props.registry[EXTENSION_LAYOUT], display)
                .map(e => {
                    cytoscape('layout', e.identifier, e);
                    return (
                        <li key={e.identifier} className="exporter">
                            <a onMouseUp={this.props.onEvent}
                               data-func="Layout"
                               data-args={JSON.stringify([e.identifier, { onlySelected }])}
                               href="#">{display(e)}</a>
                        </li>
                    );
                });
        }

    });


    return RegistryInjectorHOC(Menu, [EXTENSION_EXPORT, EXTENSION_SELECT, EXTENSION_LAYOUT]);
});

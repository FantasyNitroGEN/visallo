define([
    'react',
    './OpenLayers',
    'components/RegistryInjectorHOC',
    'configuration/plugins/registry',
    'util/vertex/formatters',
    'util/mapConfig'
], function(React, OpenLayers, RegistryInjectorHOC, registry, F, mapConfig) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.map.options',
        'Add components to map options dropdown',
        function(e) {
            return ('identifier' in e) && ('optionComponentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/mapOptions'
    );

    const PropTypes = React.PropTypes;
    const Map = React.createClass({

        propTypes: {
            configProperties: PropTypes.object.isRequired,
            onUpdateViewport: PropTypes.func.isRequired,
            onSelectElements: PropTypes.func.isRequired,
            onVertexMenu: PropTypes.func.isRequired
        },

        getInitialState() {
            return { viewport: this.props.viewport, generatePreview: true }
        },

        render() {
            const { viewport, generatePreview } = this.state;
            return (
                <div style={{height:'100%'}} ref="wrap">
                <OpenLayers
                    features={this.mapElementsToFeatures()}
                    viewport={viewport}
                    tools={this.getTools()}
                    generatePreview={generatePreview}
                    panelPadding={this.props.panelPadding}
                    onPan={this.onViewport}
                    onZoom={this.onViewport}
                    onContextTap={this.onContextTap}
                    onSelectElements={this.props.onSelectElements}
                    onUpdatePreview={this.props.onUpdatePreview.bind(this, this.props.product.id)}
                    {...mapConfig()}
                />
                </div>
            )
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.product.id === this.props.product.id) {
                this.setState({ viewport: {}, generatePreview: false })
            } else {
                this.saveViewport(this.props)
                this.setState({ viewport: nextProps.viewport || {}, generatePreview: true })
            }
        },

        componentDidMount() {
            $(this.refs.wrap).on('selectAll', (event) => {
                this.props.onSelectAll(this.props.product.id);
            })
            $(document).on('elementsCut.org-visallo-map', (event, { vertexIds }) => {
                this.props.onRemoveElementIds({ vertexIds, edgeIds: [] });
            })
            $(document).on('elementsPasted.org-visallo-map', (event, elementIds) => {
                this.props.onDropElementIds(elementIds)
            })
        },

        componentWillUnmount() {
            $(this.refs.wrap).off('selectAll');
            $(document).off('.org-visallo-map');
            this.saveViewport(this.props)
        },

        onContextTap({map, pixel, originalEvent}) {
            const vertexIds = [];
            map.forEachFeatureAtPixel(pixel, cluster => {
                cluster.get('features').forEach(f => {
                    vertexIds.push(f.getId());
                })
            })

            if (vertexIds.length) {
                const { pageX, pageY } = originalEvent;
                this.props.onVertexMenu(
                    originalEvent.target,
                    vertexIds[0],
                    { x: pageX, y: pageY }
                );
            }
        },

        getTools() {
            return this.props.registry['org.visallo.map.options'].map(e => ({
                identifier: e.identifier,
                componentPath: e.optionComponentPath,
                product: this.props.product
            }));
        },

        onViewport(event) {
            const view = event.target;

            var zoom = view.getResolution(), pan = view.getCenter();
            if (!this.currentViewport) this.currentViewport = {};
            this.currentViewport[this.props.product.id] = { zoom, pan: [...pan] };
        },

        saveViewport(props) {
            var productId = props.product.id;
            if (this.currentViewport && productId in this.currentViewport) {
                var viewport = this.currentViewport[productId];
                props.onUpdateViewport(productId, viewport);
            }
        },

        mapElementsToFeatures() {
            const { vertices, edges } = productElementIds;
            const elementsSelectedById = { ..._.indexBy(this.props.selection.vertices), ..._.indexBy(this.props.selection.edges) };
            const productVertices = _.pick(this.props.elements.vertices, _.pluck(vertices, 'id'));
            const productEdges = _.pick(this.props.elements.edges, _.pluck(edges, 'id'));
            const elements = Object.values(productVertices).concat(Object.values(productEdges));
            const geoLocationProperties = _.groupBy(this.props.ontologyProperties, 'dataType').geoLocation;

            return elements.map(el => {
                const geoLocations = geoLocationProperties &&
                    _.chain(geoLocationProperties)
                        .map(function(geoLocationProperty) {
                            return F.vertex.props(el, geoLocationProperty.title);
                        })
                        .compact()
                        .flatten()
                        .filter(function(g) {
                            return g.value && g.value.latitude && g.value.longitude;
                        })
                        .map(function(g) {
                            return [g.value.longitude, g.value.latitude];
                        })
                        .value(),
                    // TODO: check with edges
                    conceptType = F.vertex.prop(el, 'conceptType'),
                    selected = el.id in elementsSelectedById,
                    iconUrl = 'map/marker/image?' + $.param({
                        type: conceptType,
                        scale: this.props.pixelRatio > 1 ? '2' : '1',
                    }),
                    iconUrlSelected = `${iconUrl}&selected=true`;

                return {
                    id: el.id,
                    element: el,
                    selected,
                    iconUrl,
                    iconUrlSelected,
                    iconSize: [22, 40].map(v => v * this.props.pixelRatio),
                    iconAnchor: [0.5, 1.0],
                    pixelRatio: this.props.pixelRatio,
                    geoLocations
                }
            })
        },

        getTilePropsFromConfiguration() {
            const config = {...this.props.configProperties};
            const getOptions = function(providerName) {
                try {
                    var obj,
                        prefix = `map.provider.${providerName}.`,
                        options = _.chain(config)
                        .pick((val, key) => key.indexOf(`map.provider.${providerName}.`) === 0)
                        .tap(o => { obj = o })
                        .pairs()
                        .map(([key, value]) => {
                            if (/^[\d.-]+$/.test(value)) {
                                value = parseFloat(value, 10);
                            } else if ((/^(true|false)$/).test(value)) {
                                value = value === 'true'
                            } else if ((/^\[[^\]]+\]$/).test(value) || (/^\{[^\}]+\}$/).test(value)) {
                                value = JSON.parse(value)
                            }
                            return [key.replace(prefix, ''), value]
                        })
                        .object()
                        .value()
                    return options;
                } catch(e) {
                    console.error(`${prefix} options could not be parsed. input:`, obj)
                    throw e;
                }
            };

            var source = config['map.provider'] || 'osm';
            var sourceOptions;

            if (source === 'google') {
                console.warn('google map.provider is no longer supported, switching to OpenStreetMap provider');
                source = 'osm';
            }

            if (source === 'osm') {
                // Legacy configs accepted csv urls, warn and pick first
                var osmURL = config['map.provider.osm.url'];
                if (osmURL && osmURL.indexOf(',') >= 0) {
                    console.warn('Comma-separated Urls not supported, using first url. Use urls with {a-c} for multiple CDNS');
                    console.warn('For Example: https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png');
                    config['map.provider.osm.url'] = osmURL.split(',')[0].trim().replace(/[$][{]/g, '{');
                }
                sourceOptions = getOptions('osm');
                source = 'OSM';
            } else if (source === 'ArcGIS93Rest') {
                var urlKey = 'map.provider.ArcGIS93Rest.url';
                // New OL3 ArcGIS Source will throw an error if url doesn't end
                // with [Map|Image]Server
                if (config[urlKey]) {
                    config[urlKey] = config[urlKey].replace(/\/export(Image)?\/?\s*$/, '');
                }
                sourceOptions = { params: { layers: 'show:0,1,2' }, ...getOptions(source) };
                source = 'TileArcGISRest'
            } else {
                sourceOptions = getOptions(source)
            }

            return { source, sourceOptions };
        }

    });

    return RegistryInjectorHOC(Map, [
        'org.visallo.map.options'
    ])
});

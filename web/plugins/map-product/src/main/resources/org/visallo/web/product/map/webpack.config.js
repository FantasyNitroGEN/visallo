var path = require('path');
var webpack = require('webpack');
var VisalloAmdExternals = [
    'components/DroppableHOC',
    'components/NavigationControls',
    'components/RegistryInjectorHOC',
    'configuration/plugins/registry',
    'data/web-worker/store/actions',
    'data/web-worker/store/product/actions',
    'data/web-worker/store/product/actions-impl',
    'data/web-worker/store/product/selectors',
    'data/web-worker/store/selection/actions',
    'data/web-worker/store/user/actions-impl',
    'data/web-worker/util/ajax',
    'public/v1/api',
    'util/dnd',
    'util/formatters',
    'util/popovers/fileImport/fileImport',
    'util/vertex/formatters',
    'util/retina',
    'util/withContextMenu',
    'util/mapConfig',
     'openlayers',
    'fast-json-patch',
    'updeep',
    'react',
    'create-react-class',
    'prop-types',
    'react-dom',
    'redux',
    'react-redux'
].map(path => ({ [path]: { amd: path }}));

module.exports = {
  entry: {
    Map: './MapContainer.jsx',
    'actions-impl': './worker/actions-impl.js'
  },
  output: {
    path: './dist',
    filename: '[name].js',
    library: '[name]',
    libraryTarget: 'umd',
  },
  externals: VisalloAmdExternals,
  resolve: {
    extensions: ['', '.js', '.jsx', '.hbs']
  },
  module: {
    loaders: [
        {
            test: /\.jsx?$/,
            exclude: /(node_modules)/,
            loader: 'babel'
        }
    ]
  },
  devtool: 'source-map',
  plugins: [
    new webpack.optimize.UglifyJsPlugin({
        mangle: false,
        compress: {
            drop_debugger: false
        }
    })
  ]
};

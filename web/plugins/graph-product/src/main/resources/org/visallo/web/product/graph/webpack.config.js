var path = require('path');
var webpack = require('webpack');
var VisalloAmdExternals = [
    'components/DroppableHOC',
    'components/NavigationControls',
    'components/RegistryInjectorHOC',
    'components/Attacher',
    'configuration/plugins/registry',
    'data/web-worker/store/actions',
    'data/web-worker/store/product/actions-impl',
    'data/web-worker/store/product/actions',
    'data/web-worker/store/product/selectors',
    'data/web-worker/store/selection/actions',
    'data/web-worker/store/user/actions-impl',
    'data/web-worker/store/element/actions-impl',
    'data/web-worker/store/selection/actions-impl',
    'data/web-worker/util/ajax',
    'data/web-worker/store',
    'public/v1/api',
    'util/formatters',
    'util/vertex/formatters',
    'util/retina',
    'util/dnd',
    'util/withContextMenu',
    'util/withDataRequest',
    'util/withTeardown',
    'util/withFormFieldErrors',
    'detail/dropdowns/propertyForm/justification',
    'util/visibility/edit',
    'flight/lib/component',
    'fast-json-patch',
    'updeep',
    'underscore',
    'colorjs'
].map(path => ({ [path]: { amd: path }}));

module.exports = {
  entry: {
    Graph: './GraphContainer.jsx',
    'actions-impl': './worker/actions-impl.js',
    'plugin-worker': './worker/plugin.js',
    'store-changes': './worker/store-changes.js',
    EdgeLabel: './options/EdgeLabel.jsx',
    SnapToGrid: './options/SnapToGrid.jsx'
  },
  output: {
    path: './dist',
    filename: '[name].js',
    library: '[name]',
    libraryTarget: 'umd',
  },
  externals: VisalloAmdExternals.concat([
    {
      react: {
        root: 'React',
        commonjs2: 'react',
        commonjs: 'react',
        amd: 'react'
      },
    },
    {
      'react-dom': {
        root: 'ReactDOM',
        commonjs2: 'react-dom',
        commonjs: 'react-dom',
        amd: 'react-dom'
      }
    },
    {
      'redux': {
          amd: 'redux'
      }
    },
    {
      'react-redux': {
        amd: 'react-redux'
      }
    }
  ]),
  resolve: {
    extensions: ['', '.js', '.jsx', '.hbs', '.ejs']
  },
  module: {
    loaders: [
        {
            test: /\.ejs$/,
            exclude: /(node_modules)/,
            loader: 'ejs-compiled'
        },
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

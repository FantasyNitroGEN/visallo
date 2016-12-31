var path = require('path');
var webpack = require('webpack');
var VisalloAmdExternals = [
    'components/Attacher',
    'components/RegistryInjectorHOC',
    'configuration/plugins/registry',
    'data/web-worker/store/actions',
    'data/web-worker/util/ajax',
    'updeep',
    'util/promise',
    'util/formatters'
].map(path => ({ [path]: { amd: path }}));

module.exports = {
  entry: {
    Config: './js/S3Container.jsx',
    BasicAuth: './js/auth/BasicAuth.jsx',
    SessionAuth: './js/auth/SessionAuth.jsx',
    'actions-impl': './js/worker/actions-impl.js',
    'plugin-worker': './js/worker/plugin.js'
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
    extensions: ['', '.js', '.jsx']
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

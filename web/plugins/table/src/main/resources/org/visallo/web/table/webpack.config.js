var path = require('path');
var webpack = require('webpack');

module.exports = {
  entry: {
    card: './js/card/Card.jsx'
  },
  output: {
    path: './dist',
    filename: '[name].js',
    library: '[name]',
    libraryTarget: 'umd',
  },
  externals: [
    {
      'public/v1/api': {
        amd: 'public/v1/api'
      }
    },
    {
      'util/popovers/withPopover': {
        amd: 'util/popovers/withPopover'
      }
    },
    {
      'org/visallo/web/table/hbs/columnConfigPopover': {
        amd: 'org/visallo/web/table/hbs/columnConfigPopover'
      }
    },
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
    }
  ],
  resolve: {
    extensions: ['', '.js', '.jsx', '.hbs']
  },
  module: {
    loaders: [{
        test: /\.jsx?$/,
        include: path.join(__dirname, 'js'),
        loaders: ['babel-loader']
      }]
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

var path = require('path');
var webpack = require('webpack');
var VisalloAmdExternals = [
 'classnames',
 'public/v1/api',
 'util/popovers/withPopover',
 'org/visallo/web/table/hbs/columnConfigPopover'
].map(path => ({ [path]: { amd: path }}));

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
    }
  ]),
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

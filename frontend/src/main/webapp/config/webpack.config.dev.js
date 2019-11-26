/* global __dirname */

'use strict'

const webpack = require('webpack')
const merge = require('webpack-merge')
const FriendlyErrorsPlugin = require('friendly-errors-webpack-plugin')
const commonConfig = require('./webpack.config.common')
const path = require('path')

const webpackConfig = merge(commonConfig, {
  mode: 'development',
  devtool: 'cheap-module-eval-source-map',
  output: {
    path: path.join(__dirname, '../../../../target/classes/WEB'),
    publicPath: '/',
    filename: 'js/[name].bundle.js',
    chunkFilename: 'js/[id].chunk.js'
  },
  optimization: {
    runtimeChunk: 'single',
    splitChunks: {
      chunks: 'all'
    }
  },
  plugins: [
    new webpack.EnvironmentPlugin({ NODE_ENV: 'development' }),
    new webpack.HotModuleReplacementPlugin(),
    new FriendlyErrorsPlugin()
  ],
  devServer: {
    compress: true,
    historyApiFallback: true,
    hot: true,
    open: true,
    overlay: true,
    port: 8000,
    stats: {
      normal: true
    }
  }
})

module.exports = webpackConfig

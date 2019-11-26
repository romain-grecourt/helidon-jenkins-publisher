/* global __dirname */

'use strict'

const VueLoaderPlugin = require('vue-loader/lib/plugin')
const HtmlPlugin = require('html-webpack-plugin')
const MiniCSSExtractPlugin = require('mini-css-extract-plugin')
const VuetifyLoaderPlugin = require('vuetify-loader/lib/plugin')
const path = require('path')
const srcDir = path.join(__dirname, '..')
const root = path.join(srcDir, '../../..')
const isDev = process.env.NODE_ENV === 'development'

const webpackConfig = {
  entry: {
    polyfill: '@babel/polyfill',
    main: path.join(srcDir, 'main.js')
  },
  resolve: {
    extensions: ['.js', '.vue'],
    alias: {
      vue$: isDev ? 'vue/dist/vue.runtime.js' : 'vue/dist/vue.runtime.min.js',
      '@': srcDir
    }
  },
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        include: [srcDir]
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: [path.join(root, 'node_modules/vuetify')]
      },
      {
        test: /\.css$/,
        use: [
          isDev ? 'vue-style-loader' : MiniCSSExtractPlugin.loader,
          { loader: 'css-loader', options: { sourceMap: isDev } }
        ]
      },
      {
        test: /\.scss$/,
        use: [
          isDev ? 'vue-style-loader' : MiniCSSExtractPlugin.loader,
          { loader: 'css-loader', options: { sourceMap: isDev } },
          { loader: 'sass-loader', options: { sourceMap: isDev } }
        ]
      },
      {
        test: /\.sass$/,
        use: [
          isDev ? 'vue-style-loader' : MiniCSSExtractPlugin.loader,
          { loader: 'css-loader', options: { sourceMap: isDev } },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: false,
              implementation: require('sass'),
              fiber: require('fibers'),
              indentedSyntax: true // optional
            }
          }
        ]
      },
      {
        test: /\.(gif|png|jpe?g|svg)$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: '[name].[ext]',
              outputPath: 'static/img'
            }
          }
        ]
      }
    ]
  },
  plugins: [
    new VueLoaderPlugin(),
    new VuetifyLoaderPlugin(),
    new HtmlPlugin({ template: 'src/main/webapp/index.html', chunksSortMode: 'dependency' })
  ]
}

module.exports = webpackConfig

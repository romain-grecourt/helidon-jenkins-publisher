module.exports = {
  "transpileDependencies": [
    "vuetify"
  ],
  devServer: {
    proxy: {
      '^/api': {
        pathRewrite: {'^/api' : ''},
        target: 'http://localhost:9191',
        changeOrigin: true
      }
    }
  }
}
/* eslint no-unused-vars: "error" */

import Vue from 'vue'
import VueRouter from 'vue-router'
import vuetify from './plugins/vuetify'
import App from './App.vue'
import routes from './routes'
import './static/favicon.png'
import './static/helidon_logo_white_outline.png'


Vue.use(VueRouter)
Vue.config.productionTip = false

const router = new VueRouter({
  routes,
  linkActiveClass: 'active',
  mode: 'history'
})

/* eslint-disable no-new */
new Vue({
  el: '#app',
  render: h => h(App),
  router,
  vuetify
})

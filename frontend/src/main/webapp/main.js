/* eslint no-unused-vars: "error" */

import Vue from 'vue'
import VueRouter from 'vue-router'
import Vuex from 'vuex'
import axios from 'axios'
import vuetify from './plugins/vuetify'
import App from './App.vue'
import routes from './routes'
import './static/favicon.png'

Vue.use(VueRouter)
Vue.config.productionTip = false

const router = new VueRouter({
  routes,
  linkActiveClass: 'active',
  mode: 'history'
})

Vue.use(Vuex)
const store = new Vuex.Store({
  state: {
    pipelineWindowId: 0
  },
  mutations: {
    'PIPELINE_WINDOW_ID' (state, payload) {
      state.pipelineWindowId = payload
    }
  }
})

const apiUrl = 'http://localhost:9191/api/'
Vue.use({
  install (Vue) {
    Vue.prototype.$apiUrl = apiUrl
    Vue.prototype.$api = axios.create({
      baseURL: apiUrl
    })
  }
})

/* eslint-disable no-new */
new Vue({
  el: '#app',
  render: h => h(App),
  router,
  store,
  vuetify
})

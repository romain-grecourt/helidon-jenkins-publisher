<template>
  <v-app dark>
    <template v-if="dataLoaded">
      <v-navigation-drawer
          v-model="drawerRight"
          app
          right
          temporary
          disable-route-watcher
          >
        <pipelineNotifications />
      </v-navigation-drawer>
      <v-app-bar
          app
          dense
          clipped-left
          >
          <v-app-bar-nav-icon @click.stop="drawerLeft = !drawerLeft" />
          <v-toolbar-title>{{pipeline.title}}</v-toolbar-title>
          <v-spacer />
          <v-btn icon @click.stop="drawerRight = !drawerRight">
            <v-badge left overlap>
              <template v-slot:badge>
                <span>6</span>
              </template>
              <v-icon >mdi-bell</v-icon>
            </v-badge>
          </v-btn>
      </v-app-bar>
      <v-navigation-drawer
        v-model="drawerLeft"
        app
        clipped
        >
        <v-list dense nav flat>
          <v-list-item-group>
            <v-list-item to="/">
              <v-list-item-icon>
                <v-icon>mdi-arrow-left</v-icon>
              </v-list-item-icon>
              <v-list-item-content>
                <v-list-item-title>BACK</v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-list-item-group>
        </v-list>
        <v-divider />
        <pipelineInfo v-bind:pipeline="pipeline"/>
        <v-divider />
        <pipelineMenu />
      </v-navigation-drawer>
      <v-content>
          <router-view></router-view>
      </v-content>
    </template>
  </v-app>
</template>
<style>
</style>
<script>
  import statusColors from '@/statusColors'
  import statusIcons from '@/statusIcons'
  import statusText from '@/statusText'
  import PipelineInfo from './PipelineInfo'
  import PipelineMenu from './PipelineMenu'
  import PipelineNotifications from './PipelineNotifications'
  export default {
    name: 'Pipeline',
    components: {
      PipelineInfo,
      PipelineMenu,
      PipelineNotifications
    },
    data: () => ({
      onLoadedCallbacks: [],
      drawerLeft: null,
      drawerRight: null,
      statusColors: statusColors,
      statusIcons: statusIcons,
      statusText: statusText,
      pipeline: null,
      dataLoaded: false
    }),
    created () {
      this.$api.get(this.$route.params.pipelineid)
        .then((response) => {
          this.pipeline = response.data
          this.dataLoaded = true
      }).catch(error => {
        if (error.response.status === 404) {
          this.$router.push({name: 'NotFound', params: {
              message: 'Pipeline not found: ' + this.$route.params.pipelineid
            }
          })
        } else {
          this.$router.push({name: 'Error', params: {
              message: error.message
            }
          })
        }
      })
    }
  }
</script>

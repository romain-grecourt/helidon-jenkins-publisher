<template>
  <v-app dark>
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
        <v-toolbar-title>{{info.title}}</v-toolbar-title>
        <v-spacer />
        <v-badge left overlap>
          <template v-slot:badge>
            <span>6</span>
          </template>
          <v-icon @click.stop="drawerRight = !drawerRight">mdi-bell</v-icon>
        </v-badge>
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
      <pipelineInfo :info="info"/>
      <v-divider />
      <pipelineMenu :id="info.id" />
    </v-navigation-drawer>
    <v-content>
        <router-view></router-view>
    </v-content>
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
      drawerLeft: null,
      drawerRight: null,
      statusColors: statusColors,
      statusIcons: statusIcons,
      statusText: statusText,
      info: {
        id: "12345",
        title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
        status: "SUCCESS",
        repository: "oracle/helidon",
        repositoryUrl: "https://github.com/oracle/helidon",
        branch: "master",
        branchUrl: "https://github.com/oracle/helidon/tree/master",
        commit: "eb999f5c56e95958272c29d46f673fcddfb41a00",
        commitUrl: "https://github.com/oracle/helidon/commit/eb999f5c56e95958272c29d46f673fcddfb41a00",
        author: "romain-grecourt",
        authorUrl: "https://github.com/romain-grecourt",
        startTime: "Jan 1st, 2020, 1:35 PM PST",
        duration: "1hour 20minutes"
      }
    })
  }
</script>

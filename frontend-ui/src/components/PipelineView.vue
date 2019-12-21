<template>
  <v-app dark>
    <loading v-if="loading" />
    <notFound
      v-else-if="notfound"
      message="Pipeline not found!"
    />
    <notFound
      v-else-if="viewIdNotFound"
    />
    <error
      v-else-if="errored"
      :message="errored"
    />
    <template
      v-else
    >
      <v-navigation-drawer
        v-model="drawerRight"
        app
        right
        temporary
        disable-route-watcher
      >
        <!--<pipelineNotifications />-->
      </v-navigation-drawer>
      <v-app-bar
        app
        dense
        clipped-left
      >
        <v-app-bar-nav-icon
          @click.stop="drawerLeft = !drawerLeft"
        />
        <v-toolbar-title>{{ pipeline.title }}</v-toolbar-title>
        <!--<v-spacer />
        <v-btn
          icon
          disabled
          @click.stop="drawerRight = !drawerRight"
        >
          <v-badge
            left
            overlap
          >
            <template
              v-slot:badge
            >
              <span>0</span>
            </template>
            <v-icon>mdi-bell</v-icon>
          </v-badge>
        </v-btn>-->
      </v-app-bar>
      <v-navigation-drawer
        v-model="drawerLeft"
        app
        clipped
      >
        <v-list
          dense
          nav
          flat
        >
          <v-list-item-group>
            <v-list-item
              to="/"
            >
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
        <pipelineInfo
          :pipeline="pipeline"
        />
        <v-divider />
        <pipelineMenu
          @refresh="refresh"
        />
        <v-divider />
        <v-list
          dense
          nav
          flat
        >
          <v-list-item-group>
            <v-list-item
              :disabled="refreshDisabled"
              @click="refresh"
            >
              <v-list-item-icon>
                <v-icon>mdi-cached</v-icon>
              </v-list-item-icon>
              <v-list-item-content>
                <v-list-item-title>REFRESH</v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-list-item-group>
        </v-list>
      </v-navigation-drawer>
      <v-content>
        <testsView
          v-if="viewid=='tests'"
          :alltests="alltests"
        />
        <artifactsView
          v-else-if="viewid=='artifacts'"
          :allartifacts="allartifacts"
        />
        <pipelineTreeView
          v-else
          ref="treeview"
          :items="pipeline.items"
          :error="pipeline.error"
        />
      </v-content>
    </template>
  </v-app>
</template>
<script>
import statusColors from '@/statusColors'
import statusIcons from '@/statusIcons'
import statusText from '@/statusText'
import PipelineInfo from './PipelineInfo'
import PipelineMenu from './PipelineMenu'
// import PipelineNotifications from './PipelineNotifications'
import PipelineTreeView from './PipelineTreeView'
import TestsView from './TestsView'
import ArtifactsView from './ArtifactsView'
import Error from './Error'
import NotFound from './NotFound'
import Loading from './Loading'
const viewIds = ['view', 'tests', 'artifacts']
export default {
  name: 'PipelineView',
  components: {
    PipelineInfo,
    PipelineMenu,
    // PipelineNotifications,
    Loading,
    Error,
    NotFound,
    PipelineTreeView,
    ArtifactsView,
    TestsView
  },
  props: {
    pipelineid: {
      type: String,
      required: true
    },
    viewid: {
      type: String,
      required: false,
      default: 'view'
    }
  },
  data: () => ({
    drawerLeft: null,
    drawerRight: null,
    statusColors: statusColors,
    statusIcons: statusIcons,
    statusText: statusText,
    pipeline: null,
    loading: true,
    errored: false,
    notfound: false,
    refreshDisabled: false
  }),
  computed: {
    viewIdNotFound () {
      return !viewIds.includes(this.viewid)
    },
    allartifacts () {
      const res = {}
      res.items = []
      res.count = 0
      this.visitPipeline(this.artifactsVisitor, res)
      return res
    },
    alltests () {
      const res = {}
      res.items = []
      res.total = 0
      res.passed = 0
      res.failed = 0
      res.skipped = 0
      this.visitPipeline(this.testsVisitor, res)
      return res
    }
  },
  created () {
    this.refresh()
  },
  methods: {
    refreshData (data) {
      this.pipeline = data
      const treeviewComp = this.$refs.treeview
      if (typeof treeviewComp !== 'undefined' && treeviewComp !== null) {
        const treeComp = treeviewComp.$refs.treeview
        if (typeof treeComp !== 'undefined' && treeComp !== null) {
          treeComp.updateAll(true)
        }
      }
      if (this.pipeline !== null &&
        (this.pipeline.status === 'SUCCESS' ||
        this.pipeline.status === 'FAILURE' ||
        this.pipeline.status === 'UNSTABLE' ||
        this.pipeline.status === 'ABORTED')) {
        this.refreshDisabled = true
      }
    },
    refreshError (error) {
      if (typeof error.response !== 'undefined' &&
              error.response.status === 404) {
        this.notfound = true
      } else {
        this.errored = error.message
      }
    },
    refresh () {
      this.$api.get(this.$route.params.pipelineid)
        .then(response => this.refreshData(response.data))
        .catch(error => this.refreshError(error))
        .finally(() => (this.loading = false))
    },
    visitPipeline (visitor, data) {
      if (this.pipeline === null ||
            typeof this.pipeline.items === 'undefined') {
        return
      }
      // depth first traversal of the pipeline items
      var stack = []
      for (var i = this.pipeline.items.length - 1; i >= 0; i--) {
        stack.push({
          path: '',
          item: this.pipeline.items[i]
        })
      }
      while (stack.length > 0) {
        var elt = stack.pop()
        visitor(elt.item, elt.path + '/' + elt.item.name, data)
        if (typeof elt.item.children !== 'undefined') {
          for (var j = elt.item.children.length - 1; j >= 0; j--) {
            stack.push({
              path: elt.path + '/' + elt.item.name,
              item: elt.item.children[j]
            })
          }
        }
      }
    },
    artifactsVisitor (item, path, data) {
      if (item.artifacts > 0) {
        const copy = {}
        copy.count = item.artifacts
        data.count += copy.count
        copy.path = path
        copy.id = item.id
        data.items.push(copy)
      }
    },
    testsVisitor (item, path, data) {
      if (typeof item.tests !== 'undefined' && item.tests !== null) {
        const copy = {}
        copy.total = item.tests.total
        data.total += copy.total
        copy.passed = item.tests.passed
        data.passed += copy.passed
        copy.failed = item.tests.failed
        data.failed += copy.failed
        copy.skipped = item.tests.skipped
        data.skipped += copy.skipped
        copy.path = path
        copy.id = item.id
        data.items.push(copy)
      }
    }
  }
}
</script>

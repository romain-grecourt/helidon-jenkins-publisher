<template>
  <v-container fluid>
    <h2 class="mb-4">View</h2>
    <v-row class="px-5">
      <v-treeview
          open-all
          dense
          hoverable
          shaped
          style="width:100%"
          v-bind:items="items">
        <template v-slot:prepend="{ item }">
          <v-progress-circular v-if="item.status=='RUNNING'"
                               size="20"
                               width="3"
                               class="mr-5"
                               indeterminate
                               color="primary" />
          <v-icon
              v-else-if="item.status"
              v-bind:color="statusColors[item.status]"
              class="mr-4">{{ statusIcons[item.status] }}</v-icon>
          <v-icon v-else-if="item.type=='SEQUENCE'">mdi-hexagon-outline</v-icon>
          <v-icon v-else-if="item.type=='PARALLEL'">mdi-layers-triple-outline</v-icon>
        </template>
        <template v-slot:label="{ item }">
          <window v-if="item.status"
                  v-bind:id="item.id"
                  type="console"
                  v-bind:title="item.name">
            <template v-slot:prepend>
              <v-icon class="mr-4">mdi-console</v-icon>
            </template>
            <consoleOutput>
              <div class="line">line</div>
            </consoleoutput>
          </window>
          <window v-if="item.tests"
                  v-bind:id="item.id"
                  type="tests"
                  v-bind:title="item.name">
            <template v-slot:prepend>
              <v-icon class="mr-4">mdi-bug</v-icon>
            </template>
            <tests v-bind:tests="item.tests" />
          </window>
          <window v-if="item.artifacts"
                  v-bind:id="item.id"
                  type="artifacts"
                  v-bind:title="item.name">
            <template v-slot:prepend>
              <v-icon class="mr-4">mdi-cube</v-icon>
            </template>
            <artifacts v-bind:artifacts="item.artifacts" />
          </window>
          <div class="node-label-text">{{item.name}}</div>
          <v-chip v-if="item.tests||item.artifacts" class="ml-4">
            <v-btn v-if="item.tests"
                   fab x-small icon
                   @click="openWindow(item.id + '-tests')">
              <v-badge v-if="item.tests.failed > 0"
                       class="small-badge"
                       overlap
                       v-bind:color="statusColors['UNSTABLE']">
                <template v-slot:badge>!</template>
                <v-icon class="px-0">mdi-bug-outline</v-icon>
              </v-badge>
              <v-icon v-else class="px-0">mdi-bug-outline</v-icon>
            </v-btn>
            <v-btn v-if="item.artifacts"
                   fab x-small icon
                   @click="openWindow(item.id + '-artifacts')">
              <v-icon class="px-0">mdi-cube-outline</v-icon>
            </v-btn>
          </v-chip>
          <v-chip v-if="item.status" class="ml-4">
            <v-btn fab x-small icon
                   @click="openWindow(item.id + '-console')">
              <v-icon class="px-0">mdi-console</v-icon>
            </v-btn>
            <v-btn fab x-small icon>
              <v-icon class="px-0">mdi-download</v-icon>
            </v-btn>
          </v-chip>
        </template>
      </v-treeview>
    </v-row>
  </v-container>
</template>
<style>
  .v-treeview-node__label {
    display: flex;
    align-items: center;
  }
  .v-treeview-node--leaf > .v-treeview-node__root {
    background-color: #353434;
  }
  .small-badge > .v-badge__badge {
    font-size: 12px;
    height: 14px;
    min-width: 14px;
    padding: 0px 1px 0px 0px;
    top: -5px;
    right: -8px;
  }
  .node-label-text {
    flex: 1 1 90%;
    width: 50%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
<script>
  import statusColors from '@/statusColors'
  import statusIcons from '@/statusIcons'
  import utils from '@/utils'
  import ConsoleOutput from './ConsoleOutput'
  import Window from './Window'
  import Tests from './Tests'
  import Artifacts from './Artifacts'
  export default {
    name: 'PipelineView',
    components: {
      ConsoleOutput,
      Window,
      Tests,
      Artifacts
    },
    methods: {
      openWindow(id) {
        this.$store.commit('PIPELINE_WINDOW_ID', id)
      }
    },
    computed: {
      items() {
        var p = utils.getParent(this, 'Pipeline')
        if (p === false
                || p.pipeline === null
                || typeof p.pipeline.items === 'undefined') {
          return []
        }
        return p.pipeline.items
      }
    },
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
    }),
  }
</script>

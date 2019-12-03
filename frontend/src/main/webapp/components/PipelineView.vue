<template>
  <v-container
    fluid
    >
    <h2>View</h2>
    <v-row class="px-5">
      <v-treeview
          v-model="tree"
          open-all
          dense
          hoverable
          shaped
          style="width:100%"
          :items="items"
          v-on:update:open="test"
          >
        <template v-slot:prepend="{ item }">
          <v-progress-circular v-if="item.status=='RUNNING'"
                               size="20"
                               width="3"
                               class="mr-5"
                               indeterminate
                               color="primary"
                              ></v-progress-circular>
          <v-icon
              v-else-if="item.status"
              :color="statusColors[item.status]"
              class="mr-4"
              >{{ statusIcons[item.status] }}
          </v-icon>
          <v-icon v-else-if="item.type=='SEQUENCE'">
            mdi-hexagon-outline
          </v-icon>
          <v-icon v-else-if="item.type=='PARALLEL'">
            mdi-layers-triple-outline
          </v-icon>
        </template>
        <template v-slot:label="{ item }">
          {{item.name}}
          <v-chip v-if="item.tests||item.artifacts" class="ml-4">
            <v-btn v-if="item.tests"
                   fab x-small icon
                   :to="toTests(item.id)">
              <v-icon class="px-0">mdi-bug-outline</v-icon>
            </v-btn>
            <v-btn v-if="item.artifacts"
                   fab x-small icon
                   :to="toArtifacts(item.id)">
              <v-icon class="px-0">mdi-cube-outline</v-icon>
            </v-btn>
          </v-chip>
          <v-chip v-if="item.status" class="ml-4">
            <v-btn fab x-small icon
                   @click="logWindow = true">
              <v-icon class="px-0">mdi-console</v-icon>
            </v-btn>
            <v-btn fab x-small icon>
              <v-icon class="px-0">mdi-download</v-icon>
            </v-btn>
          </v-chip>
        </template>
      </v-treeview>
      <consoleWindow v-model="logWindow">
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore</div>
        <div class="line">Last line</div>
      </consoleWindow>
    </v-row>
  </v-container>
</template>
<style>
  .v-treeview-node--leaf > .v-treeview-node__root {
    background-color: #353434;
  }
</style>
<script>
  import statusColors from '@/statusColors'
  import statusIcons from '@/statusIcons'
  import ConsoleWindow from './ConsoleWindow'
  export default {
    name: 'PipelineView',
    components: {
      ConsoleWindow
    },
    methods: {
      test(active) {
        console.log(this.tree, active)
      },
      openLog(id) {
        return id == this.$route.params.stepid
      },
      toTests(id) {
        return '/' + this.$route.params.pipelineid + '/tests/' + id;
      },
      toArtifacts(id) {
        return '/' + this.$route.params.pipelineid + '/artifacts/' + id;
      }
    },
    data: () => ({
      logWindow: false,
      statusColors: statusColors,
      statusIcons: statusIcons,
      tree: [],
      items: [
        {
          id: 1,
          name: 'Build',
          type: 'SEQUENCE',
          tests: true,
          artifacts: true,
          children: [
            {
              id: 2,
              name: 'sh [\'Building :) ${BUILD_ID}\' > build.txt]',
              status: 'SUCCESS'
            }
          ]
        },
        {
          id: 3,
          name: 'Test',
          type: 'SEQUENCE',
          children: [
            {
              id: 4,
              name: 'sh [echo blah]',
              status: 'RUNNING'
            },
            {
              id: 5,
              name: 'sh [echo duh]',
              status: 'SUCCESS'
            },
            {
              id: 7,
              name: 'test1',
              type: 'PARALLEL',
              tests: true,
              artifacts: true,
              children: [
                {
                  id: 8,
                  name: 'sh [echo \'Test1a !!!\' > test1a.txt]',
                  status: 'SUCCESS'
                },
                {
                  id: 9,
                  name: 'sh [echo \'Test1b !!!\' > test1b.txt]',
                  status: 'SUCCESS'
                }
              ]
            },
            {
              id: 10,
              name: 'test2',
              type: 'PARALLEL',
              tests: true,
              artifacts: true,
              children: [
                {
                  id: 11,
                  name: 'sh [echo \'Test2a !!!\' > test2a.txt]',
                  status: 'SUCCESS'
                },
                {
                  id: 12,
                  name: 'sh [echo \'Test2b !!!\' > test2b.txt]',
                  status: 'FAILURE'
                }
              ]
            },
            {
              id: 13,
              name: 'sh [echo duh]',
              status: 'FAILURE'
            },
            {
              id: 14,
              name: 'sh [echo yeah-duh]',
              status: 'RUNNING'
            }
          ]
        }
      ]
    })
  }
</script>

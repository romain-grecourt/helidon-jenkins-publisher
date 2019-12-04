<template>
  <v-container fluid>
    <h2>Tests</h2>
    <v-subheader>2830 tests, 1 failures , 83 skipped</v-subheader>
    <v-row justify="center" class="px-5 mt-4">
      <v-expansion-panels accordion multiple>
        <v-expansion-panel v-for="(item,i) in items" v-bind:key="i">
          <v-expansion-panel-header>
            <v-badge overlap class="mr-4 noflex " :color="statusColors[item.status]">
              <template v-if="item.status=='UNSTABLE'" v-slot:badge>!</template>
              <v-icon v-if="item.type=='SEQUENCE'">mdi-hexagon-outline</v-icon>
              <v-icon v-else-if="item.type=='PARALLEL'">mdi-layers-triple-outline</v-icon>
            </v-badge>
            <span>{{item.path}}</span></v-expansion-panel-header>
          <v-expansion-panel-content>
            <tests :items="item.children" />
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-row>
  </v-container>
</template>
<style>
  .nested-panel.v-expansion-panel::before {
      box-shadow: none
  }
</style>
<script>
  import statusColors from '@/statusColors'
  import ConsoleOutput from './ConsoleOutput'
  import Tests from './Tests'
  export default {
    name: 'TestsView',
    components: {
      Tests
    },
    data: () => ({
      statusColors: statusColors,
      items:[
        {
          path: '/Build',
          type: 'SEQUENCE',
          status: 'UNSTABLE',
          children: [
            {
              name: 'io.helidon.Test.testFOO',
              status: 'SUCCESS'
            },
            {
              name: 'io.helidon.Test.testBAR',
              output: '<div class="line">yo bruh</div><div class="line">whassup</div>',
              status: 'FAILURE'
            },
            {
              name: 'io.helidon.Test.testBOB',
              status: 'SUCCESS'
            }
          ]
        },
        {
          path: '/Test/test1',
          type: 'PARALLEL',
          status: 'UNSTABLE',
          children: [
            {
              name: 'io.helidon.Test.testFOO',
              status: 'SUCCESS'
            },
            {
              name: 'io.helidon.Test.testBAR',
              output: '<div class="line"><span>yo bruh</span></div><div class="line">whassup</div>',
              status: 'FAILURE'
            },
            {
              name: 'io.helidon.Test.testBOB',
              output: '<div class="line">yo bruh</div><div class="line">whassup</div>',
              status: 'FAILURE'
            }
          ]
        },
        {
          path: '/Test/test2',
          type: 'PARALLEL',
          status: 'SUCCESS',
          children: [
            {
              name: 'io.helidon.Test.testFOO',
              status: 'SUCCESS'
            },
            {
              name: 'io.helidon.Test.testBOB',
              status: 'SUCCESS'
            }
          ]
        }
      ]
    })
}
</script>

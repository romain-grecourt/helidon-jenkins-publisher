<template>
  <v-container
    fluid
    >
    <h2>Tests</h2>
    <v-subheader>2830 tests, 1 failures , 83 skipped</v-subheader>
    <v-row justify="center" class="px-5 mt-4">
      <v-expansion-panels accordion multiple>
        <v-expansion-panel v-for="item in items">
          <v-expansion-panel-header>
            <v-badge overlap
                     class="mr-4 noflex "
                     :color="statusColors[item.status]">
              <template
                  v-if="item.status=='UNSTABLE'"
                  v-slot:badge
                  >!</template>
              <v-icon v-if="item.type=='SEQUENCE'">
                mdi-hexagon-outline
              </v-icon>
              <v-icon v-else-if="item.type=='PARALLEL'">
                mdi-layers-triple-outline
              </v-icon>
            </v-badge>
            <span>{{item.path}}</span></v-expansion-panel-header>
          <v-expansion-panel-content>
          <v-expansion-panels accordion multiple>
                  <v-expansion-panel class="nested-panel" v-for="child in item.children" :readonly="!child.output">
                    <v-expansion-panel-header hide-actions>
                      <v-icon class="noflex mr-2" :color="statusColors[child.status]">{{statusIcons[child.status]}}</v-icon>
                      <span>{{child.name}}</span>
                    </v-expansion-panel-header>
                    <v-expansion-panel-content class="output" v-html="child.output" />
                  </v-expansion-panel>
          </v-expansion-panels>
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
  .output > .v-expansion-panel-content__wrap {
    padding: 0px;
  }
  .output {
    background-color: #212121;
    counter-reset: log;
    margin: 0px 0px 10px 0px;
    padding: 10px 0px 10px 3em;
    text-decoration: none;
    white-space: pre;
    font: 13px "Source Code Pro", Menlo, Monaco, Consolas, "Courier New", monospace;
    display: block
  }
  div.line {
    color: #eee;
    position: relative;
    display: block;
    padding: 0px 0 0 3em;
  }
  div.line:before {
    counter-increment: log;
    content: counter(log);
    min-width: 2em;
    position: absolute;
    display: inline-block;
    text-align: right;
    padding-left: 1em;
    margin-left: -3em;
    color: #777777;
  }
  div.line > span {
    color: #E0E0E0;
    position: relative;
    display: inline-block
  }
  .noflex {
    flex: none !important;
  }
</style>
<script>
  import statusIcons from '@/statusIcons'
  import statusColors from '@/statusColors'
  export default {
    name: 'PipelineTests',
    data: () => ({
      statusIcons: statusIcons,
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
              output: '<div class="line"><span>yo bruh</span></div><div class="line"><span>whassup</span></div>',
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
              output: '<div class="line"><span>yo bruh</span></div><div class="line"><span>whassup</span></div>',
              status: 'FAILURE'
            },
            {
              name: 'io.helidon.Test.testBOB',
              output: '<div class="line"><span>yo bruh</span></div><div class="line"><span>whassup</span></div>',
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

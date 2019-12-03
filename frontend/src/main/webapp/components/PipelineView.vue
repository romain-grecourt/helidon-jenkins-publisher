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
          openOnClick
          style="width:100%"
          :items="items"
          >
        <template v-slot:prepend="{ item }">
          <v-badge v-if="item.type && item.type=='TESTS'"
                   overlap
                   class="mr-4"
                   :color="statusColors[item.status]">
            <template
                v-if="item.status=='UNSTABLE'"
                v-slot:badge
                >!
            </template>
            <v-icon>mdi-bug</v-icon>
          </v-badge>
          <v-progress-circular v-else-if="item.status=='RUNNING'"
                               size="20"
                               width="3"
                               class="mr-4"
                               indeterminate
                               color="primary"
                              ></v-progress-circular>
          <v-icon
              v-else-if="item.status"
              :color="statusColors[item.status]"
              class="mr-4"
              >
            {{ statusIcons[item.status] }}
          </v-icon>
          <v-icon v-else-if="item.type=='ARTIFACTS'" class="mr-4">
            mdi-cube
          </v-icon>
          <v-icon v-else-if="item.type=='SEQUENCE'">
            mdi-hexagon-outline
          </v-icon>
          <v-icon v-else-if="item.type=='PARALLEL'">
            mdi-layers-triple-outline
          </v-icon>
        </template>
      </v-treeview>
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
  export default {
    name: 'PipelineView',
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
      tree: [],
      items: [
        {
          id: 1,
          name: 'Build',
          type: 'SEQUENCE',
          children: [
            {
              id: 2,
              name: 'sh [\'Building :) ${BUILD_ID}\' > build.txt]',
              status: 'SUCCESS'
            },
            {
              id: 21,
              name: 'Test results',
              type: 'TESTS',
              status: 'SUCCESS'
            },
            {
              id: 22,
              name: 'Artifacts',
              type: 'ARTIFACTS'
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
                },
                {
                  id: 71,
                  name: 'Test results',
                  type: 'TESTS',
                  status: 'SUCCESS'
                }
              ]
            },
            {
              id: 10,
              name: 'test2',
              type: 'PARALLEL',
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
                },
                {
                  id: 101,
                  name: 'Test results',
                  type: 'TESTS',
                  status: 'UNSTABLE'
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

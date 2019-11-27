<template>
  <v-app dark>
    <v-navigation-drawer
        v-model="drawerRight"
        app
        right
        temporary
        disable-route-watcher
        >
      <v-list dense nav>
        <v-subheader>MORE PIPELINES</v-subheader>
        <v-list-item-group>
        <v-list-item three-line>
          <v-list-item-icon>
              <v-icon color="green">mdi-checkbox-marked-circle</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-subtitle>
              Pull Request #1148: WIP: MP Reactive Messaging POC
            </v-list-item-subtitle>
            <v-list-item-subtitle>
              <a href="https://github.com/oracle/helidon">oracle/helidon</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-divider />
        <v-list-item three-line>
          <v-list-item-icon>
              <v-icon color="red">mdi-close-circle</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-subtitle>
              Pull Request #1148: WIP: MP Reactive Messaging POC
            </v-list-item-subtitle>
            <v-list-item-subtitle>
              <a href="https://github.com/oracle/helidon">oracle/helidon</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-divider />
        <v-list-item three-line>
          <v-list-item-icon>
              <v-icon color="orange">mdi-alert-circle</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-subtitle>
              Pull Request #1148: WIP: MP Reactive Messaging POC
            </v-list-item-subtitle>
            <v-list-item-subtitle>
              <a href="https://github.com/oracle/helidon">oracle/helidon</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-divider />
        <v-list-item three-line>
          <v-list-item-icon>
              <v-icon color="blue">mdi-cached</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-subtitle>
              Pull Request #1148: WIP: MP Reactive Messaging POC
            </v-list-item-subtitle>
            <v-list-item-subtitle>
              <a href="https://github.com/oracle/helidon">oracle/helidon</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        </v-list-item-group>
      </v-list>
    </v-navigation-drawer>
    <v-app-bar
        app
        dense
        clipped-left
        >
        <v-app-bar-nav-icon @click.stop="drawerLeft = !drawerLeft" />
        <v-toolbar-title>Pull Request #1148: WIP: MP Reactive Messaging POC</v-toolbar-title>
        <v-spacer />
          <v-icon @click.stop="drawerRight = !drawerRight">mdi-factory</v-icon>
    </v-app-bar>

    <v-navigation-drawer
      v-model="drawerLeft"
      app
      clipped
      disable-route-watcher
      >
      <v-list dense nav flat>
        <v-subheader>DETAILS</v-subheader>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Status</v-list-item-title>
            <v-list-item-subtitle>
              <v-chip
                color="green"
                small
                label
                text-color="white"
              >
                <v-icon small left>mdi-checkbox-marked-circle</v-icon>
                Passed
              </v-chip>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Repository</v-list-item-title>
            <v-list-item-subtitle>
              <v-icon>mdi-git</v-icon>
              <a href="https://github.com/oracle/helidon">oracle/helidon</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Branch</v-list-item-title>
            <v-list-item-subtitle>
              <v-icon>mdi-source-branch</v-icon>
              <a href="#">master</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Commit</v-list-item-title>
            <v-list-item-subtitle>
              <v-icon>mdi-source-commit</v-icon>
              <a href="#">c398c14c85a9e77f415e5203c471d11e116ce120</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Author</v-list-item-title>
            <v-list-item-subtitle>
              <v-icon>mdi-account</v-icon>
              <a href="#">john_doe</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Date</v-list-item-title>
            <v-list-item-subtitle>
              <v-icon>mdi-calendar</v-icon>
              01/01/2020
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Duration</v-list-item-title>
            <v-list-item-subtitle>
              <v-icon>mdi-timelapse</v-icon>
              1hour 20minutes</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <v-divider />
      <v-list rounded dense nav>
        <v-list-item-group>
          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-bug-check</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>Test results</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-cube</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>Artifacts</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list-item-group>
      </v-list>
    </v-navigation-drawer>
    <v-content>
      <v-container
        fluid
      >
        <v-row>
          <v-treeview
              class="px-5"
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
              <v-icon v-if="item.status" :color="statusColors[item.status]">
                {{ statusIcons[item.status] }}
              </v-icon>
              <v-icon v-else-if="item.type">
                {{ typeIcons[item.type] }}
              </v-icon>
            </template>
          </v-treeview>
        </v-row>
      </v-container>
    </v-content>
  </v-app>
</template>
<style>
  .v-treeview-node--leaf > .v-treeview-node__root {
    background-color: #353434;
  }
</style>
<script>
  export default {
    props: {
      source: String,
    },
    data: () => ({
      drawerLeft: null,
      drawerRight: null,
      right: false,
      left: false,
      typeIcons: {
        SEQUENCE: 'mdi-hexagon-outline',
        PARALLEL: 'mdi-layers-triple-outline',
        TEST_RESULTS: 'mdi-bug-check',
        ARTIFACTS: 'mdi-cube'
      },
      statusColors: {
        SUCCESS: 'green',
        FAILURE: 'red',
        UNSTABLE: 'orange',
        RUNNING: 'blue'
      },
      statusIcons: {
        SUCCESS: 'mdi-checkbox-marked-circle',
        FAILURE: 'mdi-close-circle',
        UNSTABLE: 'mdi-alert-circle',
        RUNNING: 'mdi-cached'
      },
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
              type: 'TEST_RESULTS'
            },
            {
              id: 22,
              name: 'Artifacts',
              type: 'ARTIFACTS'
            },
          ],
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
                  status: 'UNSTABLE'
                },
                {
                  id: 9,
                  name: 'sh [echo \'Test1b !!!\' > test1b.txt]',
                  status: 'SUCCESS'
                },
                {
                  id: 71,
                  name: 'Test results',
                  type: 'TEST_RESULTS'
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
                  status: 'UNSTABLE'
                },
                {
                  id: 101,
                  name: 'Test results',
                  type: 'TEST_RESULTS'
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

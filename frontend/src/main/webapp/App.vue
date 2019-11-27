<template>
  <v-app dark>
    <v-navigation-drawer
        v-model="drawerRight"
        app
        right
        temporary
        disable-route-watcher
        >
      <v-list dense nav flat>
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
              <v-icon color="blue">mdi-dots-horizontal-circle</v-icon>
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
              <a href="https://github.com/oracle/helidon">oracle/helidon</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Branch</v-list-item-title>
            <v-list-item-subtitle>master</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Commit</v-list-item-title>
            <v-list-item-subtitle>
              <a href="#">c398c14c85a9e77f415e5203c471d11e116ce120</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Author</v-list-item-title>
            <v-list-item-subtitle>
              <a href="#">
                <v-avatar size="24">
                  <img
                      src="https://github.com/github.png?size=24"
                      alt="john_doe"
                      width="24"
                      >
                </v-avatar>
              </a>
              <a href="#">john_doe</a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Date</v-list-item-title>
            <v-list-item-subtitle>01/01/2020</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Date</v-list-item-title>
            <v-list-item-subtitle>01/01/2020</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Duration</v-list-item-title>
            <v-list-item-subtitle>1hour 20minutes</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <v-divider />
      <v-list rounded dense nav flat>
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
        class="fill-height"
        fluid
      >
        <v-row
          justify="center"
          align="center"
        >
          <div>
            <p>Title: "status label" "GitHub avatar" "pipeline title"</p>
            <p>Pipeline diagram (separate component)</p>
            <p>Steps: /stage[Test]/1</p>
            <p>Logs as expansion panels (log viewer as a separate component)</p>
            <p>Test results as expansion panel (test result as a separate component)</p>
            <p>Artifacts as expansion panel (artifacts as a separate component)</p>
          </div>
        </v-row>
      </v-container>
    </v-content>
  </v-app>
</template>
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
    }),
  }
</script>

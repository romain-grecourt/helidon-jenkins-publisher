<template>
  <v-container fluid>
    <h2
      class="mb-4"
    >
      View
    </h2>
    <v-alert
      v-if="error"
      text
      colored-border
      dense
      dismissible
      border="left"
      type="error"
    >
      {{ error }}
    </v-alert>
    <v-row class="px-5-md-and-up px-1-sm-and-down mb-10-sm-and-down">
      <v-treeview
        ref="treeview"
        open-all
        dense
        hoverable
        shaped
        open-on-click
        class="pipeline-tree-view "
        :items="items"
      >
        <template
          v-slot:prepend="{ item }"
        >
          <template
            v-if="item.type == 'STEP'"
          >
            <v-progress-circular
              v-if="item.status=='RUNNING'"
              size="20"
              width="3"
              class="mr-5"
              indeterminate
              color="primary"
            />
            <v-icon
              v-else
              :color="statusColors(item.status)"
              class="mr-4"
            >
              {{ statusIcons(item.status) }}
            </v-icon>
          </template>
          <v-icon
            v-else-if="item.type=='SEQUENCE'"
          >
            mdi-hexagon-outline
          </v-icon>
          <v-icon
            v-else-if="item.type=='PARALLEL'"
          >
            mdi-layers-triple-outline
          </v-icon>
          <v-icon
            v-else-if="item.type=='STEPS'"
          >
            mdi-circle-double
          </v-icon>
        </template>
        <template
          v-slot:label="{ item }"
        >
          <consoleOutputWindow
            v-if="item.type=='STEP'"
            :id="item.id"
            :title="itemTitle(item)"
          />
          <testsWindow
            v-else-if="hasTests(item)"
            :id="item.id"
            :title="itemTitle(item)"
            :testsinfo="item.tests"
          />
          <artifactsWindow
            v-if="item.artifacts > 0"
            :id="item.id"
            :title="itemTitle(item)"
            :artifacts="item.artifacts"
          />
          <div
            class="node-label-text"
          >
            {{ itemTitle(item) }}
          </div>
          <v-chip
            v-if="hasTestsOrArtifacts(item)"
            class="ml-4"
          >
            <v-btn
              v-if="hasTests(item)"
              fab
              x-small
              icon
              @click.stop="openWindow(item.id + '-tests')"
            >
              <v-badge
                v-if="item.tests.failed > 0"
                class="small-badge"
                overlap
                :color="statusColors('UNSTABLE')"
              >
                <template
                  v-slot:badge
                >
                  !
                </template>
                <v-icon
                  class="px-0"
                >
                  mdi-bug-outline
                </v-icon>
              </v-badge>
              <v-icon
                v-else
                class="px-0"
              >
                mdi-bug-outline
              </v-icon>
            </v-btn>
            <v-btn
              v-if="item.artifacts > 0"
              fab
              x-small
              icon
              @click.stop="openWindow(item.id + '-artifacts')"
            >
              <v-icon
                class="px-0"
              >
                mdi-cube-outline
              </v-icon>
            </v-btn>
          </v-chip>
          <div
            v-if="item.type=='STEP'"
            class="hidden-xs-only"
          >
            {{ duration(item.date, item.duration) }}
          </div>
          <v-chip
            v-if="item.type=='STEP'"
            class="ml-4"
          >
            <v-btn
              fab
              x-small
              icon
              @click.stop="openWindow(item.id + '-console')"
            >
              <v-icon
                class="px-0"
              >
                mdi-console
              </v-icon>
            </v-btn>
            <a
              :href="link(item, true)"
              target="new"
              x-small
              class="link-icon"
            >
              <v-btn
                fab
                x-small
                icon
              >
                <v-icon
                  class="px-0"
                >
                  mdi-open-in-new
                </v-icon>
              </v-btn>
            </a>
            <a
              :href="link(item, false)"
              target="new"
              class="link-icon"
            >
              <v-btn
                fab
                x-small
                icon
              >
                <v-icon
                  class="px-0"
                >
                  mdi-download
                </v-icon>
              </v-btn>
            </a>
          </v-chip>
        </template>
      </v-treeview>
    </v-row>
  </v-container>
</template>
<style>
@media screen and (min-width: 960px) {
  .pipeline-tree-view .v-treeview-node {
    margin-left: 15px;
  }
}
@media screen and (max-width: 959px) {
  .pipeline-tree-view .v-treeview-node {
    margin-left: 5px;
  }
}
.pipeline-tree-view {
  width:100%;
}
.pipeline-tree-view .v-treeview-node__label {
  display: flex;
  align-items: center;
}
.pipeline-tree-view .v-treeview-node--leaf > .v-treeview-node__root {
  background-color: #353434;
}
.pipeline-tree-view .v-chip {
  overflow: visible !important;
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
.link-icon {
  text-decoration: none
}
</style>
<script>
import statusColors from '@/statusColors'
import statusIcons from '@/statusIcons'
import ConsoleOutputWindow from './ConsoleOutputWindow'
import TestsWindow from './TestsWindow'
import ArtifactsWindow from './ArtifactsWindow'
import moment from 'moment'
export default {
  name: 'PipelineTreeView',
  components: {
    ConsoleOutputWindow,
    TestsWindow,
    ArtifactsWindow
  },
  props: {
    items: {
      type: Array,
      required: true
    },
    error: {
      type: String,
      default: null
    }
  },
  methods: {
    statusColors: statusColors,
    statusIcons: statusIcons,
    openWindow (id) {
      this.$store.commit('PIPELINE_WINDOW_ID', id)
    },
    hasTests (item) {
      return typeof item.tests !== 'undefined' && item.tests !== null
    },
    hasTestsOrArtifacts (item) {
      return this.hasTests(item) || item.artifacts > 0
    },
    link (item, html) {
      var link = this.$apiUrl + this.$route.params.pipelineid + '/output/' + item.id
      if (html) {
        return link + '?html'
      } else {
        return link
      }
    },
    itemTitle (item) {
      if (item.type === 'STEP') {
        return item.name + ' [ ' + item.args + ' ]'
      } else {
        return item.name
      }
    },
    duration (date, duration) {
      var m
      if (duration === 0) {
        m = moment.duration(moment().diff(moment(date)))
      } else {
        m = moment.duration(duration, 'seconds')
      }
      const hours = m.asHours()
      const minutes = m.minutes()
      const seconds = m.seconds()
      let res = ''
      if (hours >= 1) {
        res = parseInt(hours) + ' h'
      }
      if (minutes > 0) {
        if (res.length > 0) {
          res += ' '
        }
        res += minutes + ' min'
      }
      if (seconds > 0) {
        if (res.length > 0) {
          res += ' '
        }
        res += seconds + ' s'
      }
      return res
    }
  }
}
</script>

<template>
  <v-list
    dense
    nav
    flat
  >
    <v-subheader>DETAILS</v-subheader>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Status</v-list-item-title>
        <v-list-item-subtitle>
          <v-chip
            :color="statusColors(pipeline.state, pipeline.result)"
            small
            label
            text-color="white"
          >
            <v-icon
              small
              left
            >
              {{ statusIcons(pipeline.state, pipeline.result) }}
            </v-icon>
            <span>{{ statusText(pipeline.state, pipeline.result) }}</span>
          </v-chip>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Repository</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
            style="float:left"
          >
            mdi-git
          </v-icon>
          <div class="repoUrl">
            <a
              :href="pipeline.gitRepositoryUrl"
              target="new"
            >
              {{ pipeline.gitRepositoryUrl }}
            </a>
          </div>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Branch/Tag</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
          >
            mdi-source-branch
          </v-icon>
          <a
            v-if="pipeline.gitHeadUrl"
            :href="pipeline.gitHeadUrl"
            target="new"
          >
            {{ pipeline.gitHead }}
          </a>
          <span v-else>
            {{ pipeline.gitHead }}
          </span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Commit</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
          >
            mdi-source-commit
          </v-icon>
          <a
            v-if="pipeline.commitUrl"
            :href="pipeline.commitUrl"
            target="new"
          >
            {{ pipeline.gitCommit }}
          </a>
          <span v-else>
            {{ pipeline.gitCommit }}
          </span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Author</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
          >
            mdi-account
          </v-icon>
          <span v-if="!pipeline.author">unknown</span>
          <a
            v-else-if="pipeline.authorUrl"
            :href="pipeline.authorUrl"
            target="new"
          >
            {{ pipeline.author }}
          </a>
          <span v-else>
            {{ pipeline.author }}
          </span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Date</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
          >
            mdi-calendar
          </v-icon>
          <span>{{ displayDate(pipeline.date) }}</span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Duration</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
          >
            mdi-timelapse
          </v-icon>
          <span>{{ displayDuration(pipeline.duration) }}</span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
  </v-list>
</template>
<style>
  .repoUrl {
    display: inline-block;
    width: 188px;
    overflow: hidden;
    text-overflow: ellipsis;
    direction: rtl;
    text-align: left;
    line-height: 1.5;
  }
</style>
<script>
import statusColors from '@/statusColors'
import statusIcons from '@/statusIcons'
import statusText from '@/statusText'
import moment from 'moment'
export default {
  name: 'PipelineInfo',
  props: {
    pipeline: {
      type: Object,
      required: true
    }
  },
  methods: {
    statusColors: statusColors,
    statusIcons: statusIcons,
    statusText: statusText,
    displayDate (date) {
      return moment(date).format('ddd, MMM Do YYYY, h:mm A')
    },
    displayDuration (duration) {
      return moment.duration(duration, 'seconds').humanize(true)
    }
  }
}
</script>

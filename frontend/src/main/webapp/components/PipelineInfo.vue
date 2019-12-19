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
            :color="statusColors(pipeline.status)"
            small
            label
            text-color="white"
          >
            <v-icon
              small
              left
            >
              {{ statusIcons(pipeline.status) }}
            </v-icon>
            <span>{{ statusText(pipeline.status) }}</span>
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
          <div class="repourl">
            <a
              :href="pipeline.repositoryUrl"
              target="new"
            >
              {{ pipeline.repositoryUrl }}
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
            v-if="pipeline.headRefUrl"
            :href="pipeline.headRefUrl"
            target="new"
          >
            {{ pipeline.headRef }}
          </a>
          <span v-else>
            {{ pipeline.headRef }}
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
            {{ pipeline.commit }}
          </a>
          <span v-else>
            {{ pipeline.commit }}
          </span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item v-if="pipeline.mergeCommit">
      <v-list-item-content>
        <v-list-item-title>Merged with</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon
            class="mr-2"
          >
            mdi-source-merge
          </v-icon>
          <a
            v-if="pipeline.mergeCommitUrl"
            :href="pipeline.mergeCommitUrl"
            target="new"
          >
            {{ pipeline.mergeCommit }}
          </a>
          <span v-else>
            {{ pipeline.mergeCommit }}
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
          <span v-if="!pipeline.user">unknown</span>
          <a
            v-else-if="pipeline.userUrl"
            :href="pipeline.userUrl"
            target="new"
          >
            {{ pipeline.user }}
          </a>
          <span v-else>
            {{ pipeline.user }}
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
.repourl {
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
      return moment.duration(duration, 'seconds').humanize()
    }
  }
}
</script>

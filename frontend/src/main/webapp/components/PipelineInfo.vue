<template>
  <v-list dense nav flat>
    <v-subheader>DETAILS</v-subheader>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Status</v-list-item-title>
        <v-list-item-subtitle>
          <v-chip
            :color="statusColors[info.status]"
            small
            label
            text-color="white"
          >
            <v-icon small left>{{statusIcons[info.status]}}</v-icon>
            <span>{{statusText[info.status]}}</span>
          </v-chip>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Repository</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-git</v-icon><a :href="info.repositoryUrl">{{info.repository}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Branch</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-source-branch</v-icon><a :href="info.branchUrl">{{info.branch}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Commit</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-source-commit</v-icon><a :href="info.commitUrl">{{info.commit}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Author</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-account</v-icon><a :href="info.authorUrl">{{info.author}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Date</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-calendar</v-icon>
          <span>{{info.startTime}}</span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Duration</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-timelapse</v-icon>
          <span>{{info.duration}}</span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
  </v-list>
</template>
<script>
  import statusColors from '@/statusColors'
  import statusIcons from '@/statusIcons'
  import statusText from '@/statusText'
  export default {
    name: 'PipelineInfo',
    props: {
      info: Object
    },
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
      statusText: statusText
    })
  }
</script>

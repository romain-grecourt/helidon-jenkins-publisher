<template>
  <v-list dense nav flat>
    <v-subheader>DETAILS</v-subheader>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Status</v-list-item-title>
        <v-list-item-subtitle>
          <v-chip
            v-bind:color="statusColors[pipeline.status]"
            small
            label
            text-color="white">
            <v-icon small left>{{statusIcons[pipeline.status]}}</v-icon>
            <span>{{statusText[pipeline.status]}}</span>
          </v-chip>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Repository</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-git</v-icon>
          <a v-bind:href="pipeline.repositoryUrl" target="new">{{pipeline.repository}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Branch</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-source-branch</v-icon>
          <a v-bind:href="pipeline.branchUrl" target="new">{{pipeline.branch}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Commit</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-source-commit</v-icon>
          <a v-bind:href="pipeline.commitUrl" target="new">{{pipeline.commit}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Author</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-account</v-icon>
          <a v-bind:href="pipeline.authorUrl" target="new">{{pipeline.author}}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Date</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-calendar</v-icon>
          <span>{{pipeline.startTime}}</span>
        </v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title>Duration</v-list-item-title>
        <v-list-item-subtitle>
          <v-icon class="mr-2">mdi-timelapse</v-icon>
          <span>{{pipeline.duration}}</span>
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
      pipeline: Object
    },
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
      statusText: statusText
    })
  }
</script>

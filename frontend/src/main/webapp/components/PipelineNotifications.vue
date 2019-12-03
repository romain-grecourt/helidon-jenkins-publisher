<template>
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
</template>
<script>
  import statusColors from '@/statusColors'
  import statusIcons from '@/statusIcons'
  import statusText from '@/statusText'
  export default {
    name: 'PipelineNotifications',
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
      statusText: statusText,
    })
  }
</script>

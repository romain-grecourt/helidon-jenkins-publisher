<template>
  <v-app dark>
    <v-content>
      <v-container
        class="fill-height"
        fluid
      >
        <v-row
          align="center"
          justify="center"
          class="mx-0">
          <v-col cols="12" sm="12" md="10" lg="8">
            <h1>Pipelines</h1>
            <v-simple-table class="mt-4 mb-4">
              <template v-slot:default>
                <thead>
                  <tr>
                    <th class="text-left">Status</th>
                    <th class="text-left">Title</th>
                    <th class="text-left hidden-xs-only"><v-icon class="mr-2">mdi-git</v-icon>Repository</th>
                    <th class="text-left hidden-xs-only"><v-icon class="mr-2">mdi-source-branch</v-icon>Branch/Tag</th>
                    <th class="text-left"><v-icon class="mr-2">mdi-calendar-clock</v-icon>When</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in pipelines" :key="item.id" v-on:click="go(item.id)" class="table-link">
                    <td>
                      <v-progress-circular v-if="item.status=='RUNNING'"
                               size="19"
                               width="3"
                               indeterminate
                               color="primary"
                              ></v-progress-circular>
                      <v-icon v-else :color="statusColors[item.status]">{{ statusIcons[item.status] }}</v-icon>
                    </td>
                    <td>{{ item.title }}</td>
                    <td class="hidden-xs-only">{{item.repository}}</td>
                    <td class="hidden-xs-only">{{item.branch}}</td>
                    <td>{{ item.when }}</td>
                  </tr>
                </tbody>
              </template>
            </v-simple-table>
            <v-pagination
                v-model="page"
                :length="6"
                ></v-pagination>
          </v-col>
        </v-row>
      </v-container>
    </v-content>
  </v-app>
</template>
<style>
  .table-link {
    cursor: pointer;
    user-select: none;
  }
</style>
<script>
  import statusColors from '@/statusColors'
  import statusIcons from '@/statusIcons'
  export default {
    name: 'Pipelines',
    methods: {
      go: function(id) {
        this.$router.push({ path: '/' + id})
      }
    },
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
      pipelines: [
        {
          id: '13456',
          status: 'RUNNING',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 minutes ago'
        },
        {
          id: '134562',
          status: 'RUNNING',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '5 minutes ago'
        },
        {
          id: '134563',
          status: 'RUNNING',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '20 minutes ago'
        },
        {
          id: '134564',
          status: 'UNSTABLE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '134565',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '134561',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '10 minutes ago'
        },
        {
          id: '1345621',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '1 hour ago'
        },
        {
          id: '1345631',
          status: 'FAILURE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '1345641',
          status: 'UNSTABLE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '1345651',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '13456',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '10 minutes ago'
        },
        {
          id: '134562',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '1 hour ago'
        },
        {
          id: '134563',
          status: 'FAILURE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '134564',
          status: 'UNSTABLE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '1345652',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '1345612',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '10 minutes ago'
        },
        {
          id: '13456212',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '1 hour ago'
        },
        {
          id: '13456312',
          status: 'FAILURE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '13456412',
          status: 'UNSTABLE',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        },
        {
          id: '13456512',
          status: 'SUCCESS',
          title: "Pull Request #1148: WIP: MP Reactive Messaging POC",
          repository: "oracle/helidon",
          repositoryUrl: "https://github.com/oracle/helidon",
          branch: "master",
          branchUrl: "https://github.com/oracle/helidon/tree/master",
          when: '2 days ago'
        }
    ]
  })
}
</script>

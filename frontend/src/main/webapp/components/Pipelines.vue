<template>
  <v-app dark>
    <v-content v-if="dataLoaded">
      <v-container
        class="fill-height"
        fluid>
        <v-row
          align="center"
          justify="center"
          class="mx-0">
          <v-col cols="12" sm="12" md="10" lg="8">
            <h1 class="mb-2">Pipelines<v-btn class="ml-4" @click="refresh"><v-icon>mdi-cached</v-icon></v-btn></h1>
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
                  <tr v-for="item in pipelines.pipelines"
                      v-bind:key="item.id"
                      v-on:click="goToPipeline(item.id)"
                      class="table-link">
                    <td>
                      <v-progress-circular v-if="item.status=='RUNNING'"
                               size="19"
                               width="3"
                               indeterminate
                               color="primary"
                              ></v-progress-circular>
                      <v-icon v-else
                              v-bind:color="statusColors[item.status]">
                        {{ statusIcons[item.status] }}</v-icon>
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
                v-model="pipelines.page"
                v-bind:length="pipelines.numpages"
                v-on:input="onPageChange"/>
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
      goToPipeline: function(id) {
        this.$router.push({ path: '/' + id})
      },
      onPageChange: function(pageId) {
        this.$api.get("?page=" + pageId)
          .then(response => (this.pipelines = response.data))
      },
      refresh: function() {
        this.$api.get(this.pipelines.page === 0 ? "" : "?page=" + this.pipelines.page)
          .then(response => {
            this.pipelines = response.data
            this.dataLoaded = true
        })
      }
    },
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons,
      dataLoaded: false,
      pipelines: {
        page: 0,
        numpages: 0,
      }
    }),
    mounted () {
      this.refresh()
    }
  }
</script>

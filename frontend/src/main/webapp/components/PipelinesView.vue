<template>
  <v-app dark>
    <loading v-if="loading" />
    <error
      v-else-if="errored"
      :message="errored"
    />
    <v-content
      v-else
    >
      <v-container
        class="fill-height"
        fluid
      >
        <v-row
          align="center"
          justify="center"
          class="mx-0"
        >
          <v-col
            cols="12"
            sm="12"
            md="10"
            lg="8"
          >
            <h1
              class="mb-2"
            >
              Pipelines
              <v-btn
                class="ml-4"
                @click="refresh"
              >
                <v-icon>mdi-cached</v-icon>
              </v-btn>
            </h1>
            <v-simple-table
              class="mt-4 mb-4"
            >
              <template
                v-slot:default
              >
                <thead>
                  <tr>
                    <th
                      class="text-left"
                    >
                      Status
                    </th>
                    <th
                      class="text-left"
                    >
                      Message
                    </th>
                    <th
                      class="text-left hidden-xs-only"
                    >
                      <v-icon
                        class="mr-2"
                      >
                        mdi-git
                      </v-icon>
                      Repository
                    </th>
                    <th
                      class="text-left hidden-xs-only"
                    >
                      <v-icon
                        class="mr-2"
                      >
                        mdi-source-branch
                      </v-icon>
                      Branch/Tag
                    </th>
                    <th class="text-left">
                      <v-icon
                        class="mr-2"
                      >
                        mdi-calendar-clock
                      </v-icon>
                      When
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="item in pipelineInfos.items"
                    :key="item.id"
                    class="table-link"
                    @click="goToPipeline(item.id)"
                  >
                    <td>
                      <v-progress-circular
                        v-if="item.status=='RUNNING'"
                        size="19"
                        width="3"
                        indeterminate
                        color="primary"
                      />
                      <v-icon
                        v-else
                        :color="statusColors[item.status]"
                      >
                        {{ statusIcons(item.state, item.result) }}
                      </v-icon>
                    </td>
                    <td>{{ item.name }}</td>
                    <td
                      class="hidden-xs-only"
                    >
                      {{ item.gitRepositoryUrl }}
                    </td>
                    <td
                      class="hidden-xs-only"
                    >
                      {{ item.gitHead }}
                    </td>
                    <td>{{ when(item.date) }}</td>
                  </tr>
                </tbody>
              </template>
            </v-simple-table>
            <v-pagination
              v-model="pipelineInfos.pagenum"
              :length="pipelineInfos.totalpages"
              @input="onPageChange"
            />
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
import moment from 'moment'
import Error from './Error'
import Loading from './Loading'
export default {
  name: 'PipelinesView',
  components: {
    Error,
    Loading
  },
  data: () => ({
    loading: true,
    errored: false,
    pipelineInfos: {
      pagenum: 1,
      totalpages: 0
    }
  }),
  mounted () {
    this.refresh()
  },
  methods: {
    statusColors: statusColors,
    statusIcons: statusIcons,
    when (date) {
      return moment.duration(moment(date).diff(moment())).humanize(true)
    },
    goToPipeline (id) {
      this.$router.push({ path: '/' + id })
    },
    onPageChange (pagenum) {
      this.$api.get('?pagenum=' + pagenum)
        .then(response => (this.pipelineInfos = response.data))
        .catch(error => (this.errored = error.message))
        .finally(() => (this.loading = false))
    },
    refresh () {
      this.onPageChange(this.pipelineInfos.pagenum)
    }
  }
}
</script>

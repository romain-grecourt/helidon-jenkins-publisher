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
        class="fill-height px-1-sm-and-down"
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
              class="mt-4 mb-4 pipelines-list"
            >
              <template
                v-slot:default
              >
                <thead>
                  <tr>
                    <th
                      class="text-left"
                      style="width: 65px"
                    >
                      Status
                    </th>
                    <th
                      class="text-left"
                    >
                      Title
                    </th>
                    <th
                      class="text-left hidden-sm-and-down"
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
                      style="width: 110px"
                    >
                      <v-icon
                        class="mr-2"
                      >
                        mdi-source-branch
                      </v-icon>
                      Ref
                    </th>
                    <th
                      class="text-left hidden-xs-only"
                      style="width: 130px"
                    >
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
                        :color="statusColors(item.status)"
                      >
                        {{ statusIcons(item.status) }}
                      </v-icon>
                    </td>
                    <td>
                      <div class="right-ellipsis">
                        {{ item.title }}
                      </div>
                    </td>
                    <td
                      class="hidden-sm-and-down"
                    >
                      <div class="left-ellipsis">
                        {{ item.repositoryUrl }}
                      </div>
                    </td>
                    <td
                      class="hidden-xs-only"
                    >
                      <div class="right-ellipsis">
                        {{ item.headRef }}
                      </div>
                    </td>
                    <td
                      class="hidden-xs-only"
                    >
                      {{ when(item.date) }}
                    </td>
                  </tr>
                </tbody>
              </template>
            </v-simple-table>
            <v-pagination
              v-model="pipelineInfos.pagenum"
              :length="pipelineInfos.totalpages"
              :total-visible="10"
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
.pipelines-list table {
  table-layout:fixed;
}
.right-ellipsis {
  width: 98%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.left-ellipsis {
  width: 98%;
  overflow: hidden;
  text-overflow: ellipsis;
  direction: rtl;
  text-align: left;
  white-space: nowrap;
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
      this.$api.get('?pagenum=' + pagenum + '&numitems=20')
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

<template>
  <div>
    <v-subheader>{{ testsinfo.passed }} passed, {{ testsinfo.failed }} failed , {{ testsinfo.skipped }} skipped</v-subheader>
    <loading
      v-if="loading"
      :width="5"
      :size="50"
    />
    <v-expansion-panels
      v-else
      accordion
      focusable
    >
      <v-expansion-panel
        v-for="(result,i) in results"
        :key="i"
        class="nested-panel"
      >
        <v-expansion-panel-header
          hide-actions
        >
          <v-icon
            class="noflex mr-2"
            :color="statusColors(result.failed > 0 ? 'UNSTABLE' : 'PASSED')"
          >
            {{ statusIcons(result.failed > 0 ? 'UNSTABLE' : 'PASSED') }}
          </v-icon>
          <span>{{ result.name }}</span>
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-expansion-panels
            accordion
            focusable
          >
            <v-expansion-panel
              v-for="(test,j) in result.tests"
              :key="j"
              :disabled="!test.output"
              class="nested-panel"
            >
              <v-expansion-panel-header
                hide-actions
              >
                <v-icon
                  class="noflex mr-2"
                  :color="statusColors(test.status)"
                >
                  {{ statusIcons(test.status) }}
                </v-icon>
                <span>{{ test.name }}</span>
              </v-expansion-panel-header>
              <v-expansion-panel-content
                class="test-output"
              >
                <consoleOutput
                  v-html="test.output"
                />
              </v-expansion-panel-content>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>
<style>
.test-output > .v-expansion-panel-content__wrap {
  padding: 0 0 10px 0;
}
.test-output > .v-expansion-panel-content__wrap > .output {
  padding: 10px 10px 10px 20px;
  font-size: 0.9em;
  color: #E0E0E0;
}
.noflex {
  flex: none !important;
}
.loading-container {
  width: 100%;
  text-align: center;
  padding: 20px;
}
</style>
<script>
import statusIcons from '@/statusIcons'
import statusColors from '@/statusColors'
import ConsoleOutput from './ConsoleOutput'
import Loading from './Loading'
export default {
  name: 'Tests',
  components: {
    ConsoleOutput,
    Loading
  },
  props: {
    id: {
      type: String,
      required: true
    },
    testsinfo: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    loading: true,
    results: []
  }),
  created () {
    this.$api.get(this.$route.params.pipelineid + '/tests/' + this.id)
      .then((response) => (this.results = response.data))
      .finally(() => (this.loading = false))
  },
  methods: {
    statusColors: statusColors,
    statusIcons: statusIcons
  }
}
</script>

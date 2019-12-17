<template>
  <div>
    <v-subheader>{{ testsinfo.passed }} passed, {{ testsinfo.failed }} failed , {{ testsinfo.skipped }} skipped</v-subheader>
    <div
      v-if="loading"
      class="loading-container"
    >
      <v-progress-circular
        :width="5"
        :size="50"
        color="primary"
        indeterminate
      />
    </div>
    <v-expansion-panels
      v-else
      accordion
      multiple
      focusable
    >
      <template
        v-for="(result) in results"
      >
        <v-expansion-panel
          v-for="(test,i) in result.tests"
          :key="i"
          :disabled="!test.output"
          class="nested-panel"
        >
          <v-expansion-panel-header
            hide-actions
          >
            <v-icon
              class="noflex mr-2"
              :color="statusColors(null, test.status)"
            >
              {{ statusIcons(null, test.status) }}
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
      </template>
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
export default {
  name: 'Tests',
  components: {
    ConsoleOutput
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
      .then((response) => (this.results = response.data.items))
      .finally(() => {
        this.loading = false
      })
  },
  methods: {
    statusColors: statusColors,
    statusIcons: statusIcons
  }
}
</script>

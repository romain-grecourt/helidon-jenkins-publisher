<template>
  <v-container
    fluid
  >
    <h2>Tests</h2>
    <v-subheader>{{ aggregatedtests.passed }} passed, {{ aggregatedtests.failed }} failed , {{ aggregatedtests.skipped }} skipped</v-subheader>
    <v-row
      justify="center"
      class="px-5 mt-4"
    >
      <v-expansion-panels
        accordion
        multiple
      >
        <v-expansion-panel
          v-for="(testsinfo,i) in aggregatedtests.items"
          :key="i"
        >
          <v-expansion-panel-header>
            <v-badge
              overlap
              class="mr-4 noflex "
              :color="statusColors(null, testsinfo.failed > 0 ? 'UNSTABLE' : 'PASSED')"
            >
              <template
                v-if="testsinfo.failed > 0"
                v-slot:badge
              >
                !
              </template>
              <v-icon>mdi-hexagon-outline</v-icon>
            </v-badge>
            <span>{{ testsinfo.path }}</span>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <tests
              :id="testsinfo.id"
              :testsinfo="testsinfo"
            />
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-row>
  </v-container>
</template>
<style>
.nested-panel.v-expansion-panel::before {
    box-shadow: none
}
</style>
<script>
import statusColors from '@/statusColors'
import Tests from './Tests'
export default {
  name: 'TestsView',
  components: {
    Tests
  },
  props: {
    aggregatedtests: {
      type: Object,
      required: true
    }
  },
  methods: {
    statusColors: statusColors
  }
}
</script>

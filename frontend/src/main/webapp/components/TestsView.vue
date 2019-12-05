<template>
  <v-container
    fluid
  >
    <h2>Tests</h2>
    <v-subheader>{{ tests.passed }} passed, {{ tests.failed }} failed , {{ tests.skipped }} skipped</v-subheader>
    <v-row
      justify="center"
      class="px-5 mt-4"
    >
      <v-expansion-panels
        accordion
        multiple
      >
        <v-expansion-panel
          v-for="(item,i) in tests.items"
          :key="i"
        >
          <v-expansion-panel-header>
            <v-badge
              overlap
              class="mr-4 noflex "
              :color="statusColors[item.status]"
            >
              <template
                v-if="item.status=='UNSTABLE'"
                v-slot:badge
              >
                !
              </template>
              <v-icon
                v-if="item.type=='SEQUENCE'"
              >
                mdi-hexagon-outline
              </v-icon>
              <v-icon
                v-else-if="item.type=='PARALLEL'"
              >
                mdi-layers-triple-outline
              </v-icon>
            </v-badge>
            <span>{{ item.path }}</span>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <tests
              :tests="item"
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
    tests: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    statusColors: statusColors
  })
}
</script>

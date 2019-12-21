<template>
  <v-container fluid>
    <h2>Artifacts</h2>
    <v-subheader>{{ allartifacts.count }} files.</v-subheader>
    <v-row
      justify="center"
      class="px-5 mt-4"
    >
      <v-expansion-panels
        multiple
        popout
      >
        <v-expansion-panel
          v-for="(artifactsInfo,i) in allartifacts.items"
          :key="i"
        >
          <v-expansion-panel-header>
            <v-badge
              overlap
              class="mr-4 noflex "
            >
              <template
                v-slot:badge
              >
                {{ artifactsInfo.count }}
              </template>
              <v-icon> mdi-hexagon-outline</v-icon>
            </v-badge>
            <span>{{ artifactsInfo.path }}</span>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <artifacts
              :id="artifactsInfo.id"
            />
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-row>
  </v-container>
</template>
<style>
.noflex {
  flex: none !important;
}
</style>
<script>
import Artifacts from './Artifacts'
export default {
  name: 'ArtifactsView',
  components: {
    Artifacts
  },
  props: {
    allartifacts: {
      type: Object,
      required: true
    }
  }
}
</script>

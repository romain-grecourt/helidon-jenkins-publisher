<template>
  <v-list rounded dense nav>
    <v-subheader>PIPELINE</v-subheader>
    <v-list-item-group>
      <v-list-item :to="toView">
        <v-list-item-icon>
          <v-icon>mdi-tournament</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>View</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item :to="toTests">
        <v-list-item-icon>
          <v-icon>mdi-bug</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Tests</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item :to="toArtifacts">
        <v-list-item-icon>
          <v-icon>mdi-cube</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Artifacts</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </v-list-item-group>
  </v-list>
</template>
<script>
  export default {
    name: 'PipelineMenu',
    props: {
      id: String
    },
    computed: {
      toView(){
        return '/' + this.id + '/view'
      },
      toTests(){
        return '/' + this.id + '/tests'
      },
      toArtifacts(){
        return '/' + this.id + '/artifacts'
      },
    }
  }
</script>

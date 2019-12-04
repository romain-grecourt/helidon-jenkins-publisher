<template>
  <v-container
    fluid
    >
    <h2>Artifacts</h2>
    <v-subheader>9 files.</v-subheader>
    <v-row justify="center" class="px-5 mt-4">
      <v-expansion-panels accordion multiple>
        <v-expansion-panel v-for="(item,i) in items" v-bind:key="i">
          <v-expansion-panel-header>
            <v-badge overlap class="mr-4 noflex ">
              <template v-slot:badge>{{item.count}}</template>
              <v-icon v-if="item.type=='SEQUENCE'">mdi-hexagon-outline</v-icon>
              <v-icon v-else-if="item.type=='PARALLEL'">mdi-layers-triple-outline</v-icon>
            </v-badge>
            <span>{{item.path}}</span>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <artifacts :items="item.files" />
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
    data: () => ({
      items:[
        {
          path: '/Build',
          id: 1,
          type: 'SEQUENCE',
          count: 5,
          files: [
            {
              name: 'public',
              children: [
                {
                  name: 'static',
                  children: [{
                    name: 'logo.png',
                    file: 'png'
                  }]
                },
                {
                  name: 'favicon.ico',
                  file: 'png'
                },
                {
                  name: 'index.html',
                  file: 'html'
                }
              ]
            },
            {
              name: '.gitignore',
              file: 'txt'
            },
            {
              name: 'README.md',
              file: 'md'
            }
          ]
        },
        {
          path: '/Test/test1',
          type: 'PARALLEL',
          count: 3,
          files: [
            {
              name: '.gitignore',
              file: 'txt'
            },
            {
              name: 'pom.xml',
              file: 'xml'
            },
            {
              name: 'README.md',
              file: 'md'
            }
          ]
        },
        {
          path: '/Test/test2',
          type: 'PARALLEL',
          count: 1,
          files: [
            {
              name: 'public',
              children: [
                {
                  name: 'static',
                  children: [{
                    name: 'logo.png',
                    file: 'png'
                  }]
                }
              ]
            }
          ]
        }
      ]
  })
}
</script>

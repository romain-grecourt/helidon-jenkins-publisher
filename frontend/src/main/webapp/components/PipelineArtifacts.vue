<template>
  <v-container
    fluid
    >
    <h2>Artifacts</h2>
    <v-subheader>9 files.</v-subheader>
    <v-row justify="center" class="px-5 mt-4">
      <v-expansion-panels accordion multiple :value="panel">
        <v-expansion-panel v-for="(item,i) in items" v-bind:key="i">
          <v-expansion-panel-header>
            <v-badge overlap
                     class="mr-4 noflex ">
              <template v-slot:badge
                >{{item.count}}</template>
              <v-icon v-if="item.type=='SEQUENCE'">
                mdi-hexagon-outline
              </v-icon>
              <v-icon v-else-if="item.type=='PARALLEL'">
                mdi-layers-triple-outline
              </v-icon>
            </v-badge>
            <span>{{item.path}}</span></v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-treeview
              v-model="tree"
              dense
              :items="item.children"
              item-key="name"
              open-on-click
            >
              <template v-slot:prepend="{ item, open }">
                <v-icon v-if="!item.file">
                  {{ open ? 'mdi-folder-open' : 'mdi-folder' }}
                </v-icon>
                <v-icon v-else>
                  {{ files[item.file] }}
                </v-icon>
              </template>
            </v-treeview>
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
  .files{
    background-color: #353434;
    margin-left: 20px;
  }
  .v-treeview {
    background-color: #353434;
  }
</style>
<script>
  export default {
    name: 'PipelineArtifacts',
    computed: {
      panel(){
        return this.$route.params.stageid ? [ 0 ] : [];
      }
    },
    data: () => ({
      files: {
        html: 'mdi-language-html5',
        js: 'mdi-nodejs',
        json: 'mdi-json',
        xml: 'mdi-xml',
        md: 'mdi-markdown',
        pdf: 'mdi-file-pdf',
        png: 'mdi-file-image',
        txt: 'mdi-file-document-outline',
        xls: 'mdi-file-excel'
      },
      tree: [],
      items:[
        {
          path: '/Build',
          type: 'SEQUENCE',
          count: 5,
          children: [
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
          children: [
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
          children: [
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

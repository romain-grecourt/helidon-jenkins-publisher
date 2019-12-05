<template>
  <v-treeview
    v-model="tree"
    dense
    open-all
    v-bind:items="artifacts.items"
    item-key="path"
    open-on-click>
    <template v-slot:prepend="{ item, open }">
      <v-icon v-if="!item.file">
        {{ open ? 'mdi-folder-open' : 'mdi-folder' }}
      </v-icon>
      <v-icon v-else>
        {{ fileIcons[item.file] }}
      </v-icon>
    </template>
  </v-treeview>
</template>
<style>
  .v-treeview {
    background-color: #353434;
  }
</style>
<script>
  export default {
    name: 'Artifacts',
    props: {
      artifacts: {
        type: Object,
        required: true
      }
    },
    data: () => ({
      fileIcons: {
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
      tree: []
    })
  }
</script>


<template>
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
  <v-treeview
    v-else
    v-model="tree"
    dense
    open-all
    :items="artifacts"
    class="artifacts-tree-view"
    item-key="path"
    open-on-click
  >
    <template v-slot:prepend="{ item, open }">
      <v-icon v-if="!item.type">
        {{ open ? 'mdi-folder-open' : 'mdi-folder' }}
      </v-icon>
      <v-icon v-else>
        {{ fileIcons[item.type] }}
      </v-icon>
    </template>
    <template
      v-slot:label="{ item }"
    >
      <div
        class="node-label-text"
      >
        {{ item.name }}
      </div>
      <v-chip
        v-if="item.type"
        class="ml-4"
        color="#353434"
      >
        <a
          :href="link(item, false)"
          target="new"
          class="link-icon"
        >
          <v-btn
            fab
            x-small
            icon
          >
            <v-icon
              class="px-0"
            >
              mdi-open-in-new
            </v-icon>
          </v-btn>
        </a>
      </v-chip>
    </template>
  </v-treeview>
</template>
<style>
  .artifacts-tree-view {
    width: 100%;
  }
.artifacts-tree-view .v-treeview-node__label {
  display: flex;
  align-items: center;
}
.loading-container {
  width: 100%;
  text-align: center;
  padding: 20px;
}
.node-label-text {
  flex: 1 1 90%;
  width: 50%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
<script>
export default {
  name: 'Artifacts',
  props: {
    id: {
      type: String,
      required: true
    }
  },
  data: () => ({
    loading: true,
    artifacts: [],
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
      // TODO complete this
    },
    tree: []
  }),
  created () {
    this.$api.get(this.$route.params.pipelineid + '/artifacts/' + this.id)
      .then((response) => (this.artifacts = response.data.items))
      .finally(() => {
        this.loading = false
      })
  },
  methods: {
    link (item, download) {
      var link = 'http://localhost:9191/api/' + this.$route.params.pipelineid + '/artifacts/' + this.id + '/' + item.path
      if (download) {
        return link + '?download'
      } else {
        return link
      }
    }
  }
}
</script>

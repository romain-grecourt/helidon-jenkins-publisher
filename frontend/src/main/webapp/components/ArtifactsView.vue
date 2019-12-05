<template>
  <v-container fluid>
    <h2>Artifacts</h2>
    <v-subheader>{{artifacts.count}} files.</v-subheader>
    <v-row justify="center" class="px-5 mt-4">
      <v-expansion-panels accordion multiple>
        <v-expansion-panel v-for="(item,i) in artifacts.items" v-bind:key="i">
          <v-expansion-panel-header>
            <v-badge overlap class="mr-4 noflex ">
              <template v-slot:badge>{{item.count}}</template>
              <v-icon v-if="item.type=='SEQUENCE'">mdi-hexagon-outline</v-icon>
              <v-icon v-else-if="item.type=='PARALLEL'">mdi-layers-triple-outline</v-icon>
            </v-badge>
            <span>{{item.path}}</span>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <artifacts v-bind:artifacts="item" />
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
  import utils from '@/utils'
  export default {
    name: 'ArtifactsView',
    components: {
      Artifacts
    },
    computed: {
      artifacts() {
        let p = utils.getParent(this, 'Pipeline')
        if (p === false) {
          return {}
        }
        if (typeof p.pipeline.items === 'undefined') {
          return {}
        }
        let res = {}
        res.items = []
        res.count = 0

        // depth first traversal of the pipeline items
        var stack = []
        for (var i=p.pipeline.items.length -1 ; i >= 0 ; i--) {
          stack.push({
            path: '',
            item: p.pipeline.items[i]
          })
        }
        while (stack.length > 0) {
          var elt = stack.pop()
          if (typeof elt.item.artifacts !== 'undefined') {
            let copy = {}
            copy.count = elt.item.artifacts.count
            res.count += copy.count
            copy.items = elt.item.artifacts.items
            copy.path = elt.path + '/' + elt.item.name
            copy.type = elt.item.type
            res.items.push(copy)
          }
          if (typeof elt.item.children !== 'undefined') {
            for (var i=elt.item.children.length -1 ; i >= 0 ; i--) {
              stack.push({
                path: elt.path + '/' + elt.item.name,
                item: elt.item.children[i]
              })
            }
          }
        }
        return res
      }
    }
  }
</script>

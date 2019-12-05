<template>
  <v-container fluid>
    <h2>Tests</h2>
    <v-subheader>{{tests.passed}} passed, {{tests.failed}} failed , {{tests.skipped}} skipped</v-subheader>
    <v-row justify="center"
           class="px-5 mt-4">
      <v-expansion-panels accordion multiple>
        <v-expansion-panel v-for="(item,i) in tests.items"
                           v-bind:key="i">
          <v-expansion-panel-header>
            <v-badge overlap
                     class="mr-4 noflex "
                     v-bind:color="statusColors[item.status]">
              <template v-if="item.status=='UNSTABLE'"
                        v-slot:badge>!</template>
              <v-icon v-if="item.type=='SEQUENCE'">mdi-hexagon-outline</v-icon>
              <v-icon v-else-if="item.type=='PARALLEL'">mdi-layers-triple-outline</v-icon>
            </v-badge>
            <span>{{item.path}}</span></v-expansion-panel-header>
          <v-expansion-panel-content>
            <tests v-bind:tests="item" />
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
  import ConsoleOutput from './ConsoleOutput'
  import utils from '@/utils'
  import Tests from './Tests'
  export default {
    name: 'TestsView',
    components: {
      Tests
    },
    computed: {
      tests() {
        let p = utils.getParent(this, 'Pipeline')
        if (p === false) {
          return {}
        }
        if (typeof p.pipeline.items === 'undefined') {
          return {}
        }
        let res = {}
        res.items = []
        res.passed = 0
        res.failed = 0
        res.skipped = 0

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
          if (typeof elt.item.tests !== 'undefined') {
            let copy = {}
            copy.passed = elt.item.tests.passed
            res.passed += copy.passed
            copy.failed = elt.item.tests.failed
            res.failed += copy.failed
            copy.skipped = elt.item.tests.skipped
            res.skipped += copy.skipped
            copy.status = copy.failed === 0 ? 'SUCCESS' : 'UNSTABLE'
            copy.items = elt.item.tests.items
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
    },
    data: () => ({
      statusColors: statusColors
    })
}
</script>

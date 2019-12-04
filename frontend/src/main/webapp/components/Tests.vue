<template>
  <v-expansion-panels accordion multiple>
    <v-expansion-panel v-for="(item,i) in items" v-bind:key="i" :readonly="!item.output" class="nested-panel">
      <v-expansion-panel-header hide-actions>
        <v-icon class="noflex mr-2" :color="statusColors[item.status]">{{statusIcons[item.status]}}</v-icon>
        <span>{{item.name}}</span>
      </v-expansion-panel-header>
      <v-expansion-panel-content class="test-output">
        <consoleOutput v-html="item.output" />
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
</template>
<style>
  .test-output > .v-expansion-panel-content__wrap {
    padding: 0 0 10px 0;
  }
  .test-output > .v-expansion-panel-content__wrap > .output {
    background-color: #212121;
    padding: 10px 10px 10px 20px;
  }
  .noflex {
    flex: none !important;
  }
</style>
<script>
  import statusIcons from '@/statusIcons'
  import statusColors from '@/statusColors'
  import ConsoleOutput from './ConsoleOutput'
  export default {
    name: 'Tests',
    props: {
      items: Array
    },
    components: {
      ConsoleOutput
    },
    data: () => ({
      statusColors: statusColors,
      statusIcons: statusIcons
    })
  }
</script>

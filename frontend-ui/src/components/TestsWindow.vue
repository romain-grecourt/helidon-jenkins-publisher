<template>
  <window
    :id="windowId"
    :title="title"
  >
    <template
      v-slot:prepend
    >
      <v-icon
        class="mr-4"
      >
        mdi-bug
      </v-icon>
    </template>
    <tests
      :id="id"
      :testsinfo="testsinfo"
    />
  </window>
</template>
<style>
  .output {
    margin: 10px 5px 10px 5px;
  }
  .output-controls {
    padding: 5px 20px 5px 20px;
  }
</style>
<script>
import Window from './Window'
import Tests from './Tests'
export default {
  name: 'TestsWindow',
  components: {
    Window,
    Tests
  },
  props: {
    id: {
      type: String,
      required: true
    },
    title: {
      type: String,
      required: true
    },
    testsinfo: {
      type: Object,
      required: true
    }
  },
  computed: {
    windowId () {
      return this.id + '-tests'
    }
  }
}
</script>

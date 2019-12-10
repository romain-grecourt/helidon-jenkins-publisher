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
      :tests="tests"
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
  computed: {
    windowId() {
      return this.id + "-tests"
    }
  },
  props: {
    id: {
      type: Number,
      required: true
    },
    title: {
      type: String,
      required: true
    },
    tests: {
      type: Object,
      required: true
    }
  },
}
</script>

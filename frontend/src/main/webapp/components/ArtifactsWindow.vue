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
        mdi-cube
      </v-icon>
    </template>
    <artifacts
      :artifacts="artifacts"
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
import Artifacts from './Artifacts'
export default {
  name: 'TestsWindow',
  components: {
    Window,
    Artifacts
  },
  computed: {
    windowId() {
      return this.id + "-artifacts"
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
    artifacts: {
      type: Object,
      required: true
    }
  },
}
</script>

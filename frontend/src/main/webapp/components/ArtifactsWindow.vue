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
      :id="id"
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
  props: {
    id: {
      type: String,
      required: true
    },
    title: {
      type: String,
      required: true
    }
  },
  computed: {
    windowId () {
      return this.id + '-artifacts'
    }
  }
}
</script>

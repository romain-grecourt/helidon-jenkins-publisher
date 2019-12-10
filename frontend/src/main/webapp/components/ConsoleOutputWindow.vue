<template>
  <window
    ref="window"
    :id="windowId"
    :title="title"
    @opened="onWindowOpened"
    @closed="onWindowClosed"
  >
    <template
      v-slot:prepend
    >
      <v-icon
        class="mr-4"
      >
        mdi-console
      </v-icon>
    </template>
    <template
      v-slot:append
    >
      <v-progress-linear
        height="4"
        style="min-height: 4px"
        :buffer-value="bufferValue"
        :value="progress"
        :indeterminate="progressIndeterminate"
        :query="true"
        stream
      />
    </template>
    <error v-if="errored">
      <v-btn
        class="mt-5"
        @click="retry"
      >
        RETRY
      </v-btn>
    </error>
    <consoleOutput
      v-else
      ref="output"
    >
    </consoleoutput>
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
import ConsoleOutput from './ConsoleOutput'
import Error from './Error'
export default {
  name: 'ConsoleOutputWindow',
  components: {
    Window,
    ConsoleOutput,
    Error
  },
  computed: {
    bufferValue() {
      return this.readonly ? 100 : 90
    },
    windowId() {
      return this.id + "-console"
    }
  },
  methods: {
    retry() {
      // TODO
    },
    onWindowOpened() {
      this.active = true
      this.loadOutput(null)
    },
    calcProgress(scale, progress, total, value) {
      // scale is how much percent the progress represents (e.g. 90% or 100%)
      // progress is a value between [0,scale]
      // recalculate the total based on the current progress value
      var total = progress === 0 ? total : total  / ((scale - progress) / scale)
      // calculate progress
      return scale - ((value / total) * scale)
    },
    loadOutput(position) {
      if (!this.active) {
        return;
      }
      if (this.loading && !(this.remaining === 0 && this.position > 0)) {
        let uri = 'test' + '/output/' + 1
        uri += '?tail&wrap_html'
        if (this.readonly) {
          uri += '&lines_only'
        }
        if (position !== null) {
          uri += '&position=' + position
        }
        this.$api.get(uri)
          .then((response) => {
            if (this.active) {
              this.position = parseInt(response.headers['vnd.io.helidon.publisher.position'])
              this.remaining = parseInt(response.headers['vnd.io.helidon.publisher.remaining'])
              this.progress = this.calcProgress(this.readonly ? 100 : 90, this.progress, this.position, this.remaining)
              this.progressIndeterminate = false
              let outputContainer = this.$refs.output.$refs.container;
              outputContainer.innerHTML = response.data + outputContainer.innerHTML
              let windowContent = this.$refs.window.$refs.content;
              windowContent.scrollTop = windowContent.scrollHeight;
              this.loadOutput(this.remaining)
            }
          })
          .catch(error => {
            this.errored = true
            console.warn(error)
          })
      }
    },
    onWindowClosed() {
      this.active = false
    },
  },
  props: {
    id: {
      type: Number,
      required: true
    },
    title: {
      type: String,
      required: true
    }
  },
  data: () => ({
    active: false, // window active
    loading: true, // progress bar
    readonly: true, // output is not going to change
    errored: false, // error when fetching output
    progressIndeterminate: true, // indeterminate progress bar
    progress: 0, // progress bar value
    position: 0, // output raw position index
    remaining: 0 // output raw remaining
  }),
  watch: {
    opened () {
      var windowId = this.id + '-' + this.type
      var curState = this.$store.state.pipelineWindowId === windowId
      if (!this.opened && curState) {
        this.$store.commit('PIPELINE_WINDOW_ID', 0)
      }
    }
  }
}
</script>
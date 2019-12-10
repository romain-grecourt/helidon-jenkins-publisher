<template>
  <window
    :id="windowId"
    ref="window"
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
        :value="100"
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
      :dohtml="false"
    />
  </window>
</template>
<style>
  .output {
    margin: 10px;
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
  computed: {
    windowId () {
      return this.id + '-console'
    }
  },
  watch: {
    opened () {
      var curState = this.$store.state.pipelineWindowId === this.windowId
      if (!this.opened && curState) {
        this.$store.commit('PIPELINE_WINDOW_ID', 0)
      }
    }
  },
  methods: {
    retry () {
      const outputContainer = this.$refs.output.$refs.container
      outputContainer.innerHTML = ''
      this.progressIndeterminate = true
      this.loadOutput(0, 0, true, this.readonly, 1000)
    },
    onWindowOpened () {
      this.active = true
      this.loadOutput(0, 0, true, this.readonly, 1000)
    },
    renderOutput (position, remaining, tail, linesOnly, lines, output) {
      this.remaining = remaining
      this.position = position
      if (remaining === 0) {
        this.progressIndeterminate = false
      }
      const outputContainer = this.$refs.output.$refs.container
      if (tail) {
        outputContainer.innerHTML = output + outputContainer.innerHTML
      } else {
        outputContainer.innerHTML = outputContainer.innerHTML + output
      }
      const windowContent = this.$refs.window.$refs.content
      windowContent.scrollTop = windowContent.scrollHeight
      if (this.active) {
        if (tail) {
          this.loadOutput(remaining, position, tail, linesOnly, lines)
        } else {
          this.loadOutput(position, remaining, tail, linesOnly, lines)
        }
      }
    },
    loadOutput (position, remaining, tail, linesOnly, lines) {
      if ((tail && position === 0 && remaining > 0) ||
              (!tail && remaining === 0 && position > 0)) {
        return
      }
      let uri = 'test' + '/output/' + 1 // TODO this is hard-coded
      uri += '?position=' + position
      uri += '&lines=' + lines
      if (tail) {
        uri += '&tail'
      }
      if (linesOnly) {
        uri += '&lines_only'
      }
      this.$api.get(uri)
        .then((response) => {
          position = parseInt(response.headers['vnd.io.helidon.publisher.position'])
          remaining = parseInt(response.headers['vnd.io.helidon.publisher.remaining'])
          this.renderOutput(position, remaining, tail, linesOnly, lines, response.data)
        })
        .catch(error => {
          this.errored = true
          console.warn(error)
        })
    },
    onWindowClosed () {
      this.active = false
    }
  }
}
</script>

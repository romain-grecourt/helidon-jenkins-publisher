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
        :style="progressBarActive"
        :active="loading"
        background-color="#b3aaaa"
        indeterminate
        stream
      />
    </template>
    <template v-slot:footer>
      <v-system-bar
        lights-out
      >
        <span>{{ size }}</span>
        <v-spacer />
        <v-btn
          x-small
          icon
          class="mx-1"
          @click="scrollTop"
        >
          <v-icon>mdi-arrow-up</v-icon>
        </v-btn>
        <v-btn
          x-small
          icon
          class="mx-1"
          @click="scrollBottom"
        >
          <v-icon>mdi-arrow-down</v-icon>
        </v-btn>
        <v-btn
          x-small
          icon
          class="mx-1"
          @click="clear"
        >
          <v-icon>mdi-cancel</v-icon>
        </v-btn>
        <v-btn
          x-small
          icon
          class="mx-1"
          @click="stop"
        >
          <v-icon>mdi-stop</v-icon>
        </v-btn>
        <v-btn
          x-small
          icon
          class="mx-1"
          @click="refresh"
        >
          <v-icon>mdi-cached</v-icon>
        </v-btn>
      </v-system-bar>
    </template>
    <consoleOutput
      ref="output"
    />
  </window>
</template>
<style>
.output {
  margin: 10px;
}
.output-error .message {
  font-size: 1em;
}
</style>
<script>
import Window from './Window'
import ConsoleOutput from './ConsoleOutput'
export default {
  name: 'ConsoleOutputWindow',
  components: {
    Window,
    ConsoleOutput
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
  data: () => ({
    active: false, // window active
    loading: false, // progress bar
    readonly: true, // output is not going to change
    position: 0, // output raw position index
    remaining: 0, // output raw remaining
    bytes: 0, // total bytes loaded
    size: '0 bytes'
  }),
  computed: {
    windowId () {
      return this.id + '-console'
    },
    progressBarActive () {
      return this.loading ? { minHeight: '4px' } : {}
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
    refreshSize () {
      if (this.bytes < 1024) {
        this.size = this.bytes + ' bytes'
      } else if (this.bytes < 1048576) {
        this.size = (this.bytes / 1024).toFixed(1) + ' kib'
      } else {
        // mib is the max unit displayed
        this.size = (this.bytes / 1048576).toFixed(1) + ' mib'
      }
    },
    scrollTop () {
      const windowContent = this.$refs.window.$refs.content
      windowContent.scrollTop = 0
    },
    scrollBottom () {
      const windowContent = this.$refs.window.$refs.content
      windowContent.scrollTop = windowContent.scrollHeight
    },
    stop () {
      this.active = false
    },
    refresh () {
      if (!this.loading) {
        this.active = true
        if (this.position === 0 && this.remaining === 0) {
          // load backward from the end
          this.loadOutput(this.position, this.remaining, true, this.readonly, 400)
        } else {
          // incremental forward load
          this.loadOutput(this.bytes, 1, false, this.readonly, 400)
        }
      }
    },
    clear () {
      const output = this.$refs.output
      if (typeof output !== 'undefined' && output != null) {
        const outputContainer = this.$refs.output.$refs.container
        if (typeof outputContainer !== 'undefined' && outputContainer != null) {
          outputContainer.innerHTML = ''
        }
      }
      this.position = 0
      this.remaining = 0
      this.bytes = 0
      this.size = '0 bytes'
    },
    onWindowOpened () {
      this.active = true
      this.refresh()
    },
    renderOutput (position, remaining, backward, linesOnly, lines, output) {
      if (output.length === 0) {
        // no data, stop here
        this.loading = false
        return
      }
      this.remaining = remaining
      this.position = position
      if (remaining === 0) {
        // no more data available for now
        this.loading = false
      }
      if (linesOnly && output[output.length - 1] !== '\n') {
        // current data needs a newline char
        output += '\n'
      }
      const outputContainer = this.$refs.output.$refs.container
      if (backward) {
        outputContainer.innerHTML = output + outputContainer.innerHTML
        if (this.bytes === 0) {
          // if going backwards the total size if the position
          // after the first request
          this.bytes = this.position
          this.refreshSize()
        }
      } else {
        outputContainer.innerHTML = outputContainer.innerHTML + output
        this.bytes = this.position
        this.refreshSize()
      }
      // TODO listen to scrolls and maintain a state to determine
      // if we should auto scroll
      this.scrollBottom()
      // keep loading, there is more
      if (this.active) {
        if (backward) {
          this.loadOutput(remaining, position, backward, linesOnly, lines)
        } else {
          this.loadOutput(position, remaining, backward, linesOnly, lines)
        }
      }
    },
    loadOutput (position, remaining, backward, linesOnly, lines) {
      if ((backward && position === 0 && remaining > 0) ||
              (!backward && remaining === 0 && position > 0)) {
        // nothing to do.
        return
      }
      let uri = this.$route.params.pipelineid + '/output/' + this.id
      uri += '?position=' + position
      uri += '&lines=' + lines
      if (backward) {
        uri += '&backward=true'
      }
      if (linesOnly) {
        uri += '&lines_only=true'
      }
      this.loading = true
      this.$api.get(uri)
        .then((response) => {
          position = parseInt(response.headers['vnd.io.helidon.publisher.position'])
          remaining = parseInt(response.headers['vnd.io.helidon.publisher.remaining'])
          this.renderOutput(position, remaining, backward, linesOnly, lines, response.data)
        })
        .catch(error => {
          this.loading = false
          console.warn(error)
        })
    },
    onWindowClosed () {
      this.active = false
    }
  }
}
</script>

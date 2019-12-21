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
    active: false, // flag to control continuous loading
    loading: false, // flag to display the progress bar
    readonly: true, // is the output readonly ? (i.e no more appends)
    position: 0, // raw position index
    remaining: 0, // raw remaining bytes
    bytes: 0, // total bytes (current known end of file)
    size: '0 bytes' // human readable size displayed
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
    scrollTop () {
      const windowContent = this.$refs.window.$refs.content
      windowContent.scrollTop = 0
    },
    scrollBottom () {
      const windowContent = this.$refs.window.$refs.content
      windowContent.scrollTop = windowContent.scrollHeight
    },
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
    onWindowOpened () {
      this.active = true
      this.refresh()
    },
    stop () {
      this.active = false
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
    refresh () {
      if (!this.loading) {
        this.active = true
        if (this.position === 0 && this.remaining === 0) {
          // first load
          this.loadBackward(this.position, this.remaining, this.readonly, 400)
        } else {
          // incremental
          this.loadForward(this.bytes, 1, this.readonly, 400)
        }
      }
    },
    loadForward (position, remaining, linesOnly, lines) {
      if (remaining === 0 && position > 0) {
        this.loading = false
        return
      }
      let uri = this.$route.params.pipelineid + '/output/' + this.id
      uri += '?backward=false&position=' + position
      uri += '&lines=' + lines
      if (linesOnly) {
        uri += '&lines_only=true'
      }
      this.loading = true
      this.$api.get(uri)
        .then((response) => {
          position = parseInt(response.headers['vnd.io.helidon.publisher.position'])
          remaining = parseInt(response.headers['vnd.io.helidon.publisher.remaining'])
          if (response.data.length > 0) {
            this.remaining = remaining
            this.position = position
            var output = response.data
            if (linesOnly && output[output.length - 1] !== '\n') {
              // current data needs a newline char
              output += '\n'
            }
            const outputContainer = this.$refs.output.$refs.container
            outputContainer.innerHTML = outputContainer.innerHTML + output
            this.bytes = this.position
            this.refreshSize()
            this.scrollBottom() // TODO detect user scroll
            if (this.active && !(remaining === 0 && position > 0)) {
              // more data to fetch
              this.loadForward(position, remaining, linesOnly, lines)
            } else {
              // no data, stop here
              this.loading = false
            }
          } else {
            // no data, stop here
            this.loading = false
          }
        })
        .catch(() => (this.loading = false))
    },
    loadBackward (position, remaining, linesOnly, lines) {
      if (position === 0 && remaining > 0) {
        this.loading = false
        // no data to fetch
        return
      }
      let uri = this.$route.params.pipelineid + '/output/' + this.id
      uri += '?backward=true&position=' + position
      uri += '&lines=' + lines
      if (linesOnly) {
        uri += '&lines_only=true'
      }
      this.loading = true
      this.$api.get(uri)
        .then((response) => {
          position = parseInt(response.headers['vnd.io.helidon.publisher.position'])
          remaining = parseInt(response.headers['vnd.io.helidon.publisher.remaining'])
          if (response.data.length > 0) {
            this.remaining = remaining
            this.position = position
            var output = response.data
            if (linesOnly && output[output.length - 1] !== '\n') {
              // current data needs a newline char
              output += '\n'
            }
            const outputContainer = this.$refs.output.$refs.container
            outputContainer.innerHTML = output + outputContainer.innerHTML
            if (this.bytes === 0) {
              // total size is the position return by the first request
              this.bytes = this.position
              this.refreshSize()
            }
            this.scrollBottom() // TODO detect user scroll
            if (this.active && !(remaining === 0 && position > 0)) {
              // more data to fetch
              this.loadBackward(remaining, position, linesOnly, lines)
            } else {
              // no more data to fetch or inactive
              this.loading = false
            }
          } else {
            // empty data
            this.loading = false
          }
        })
        .catch(() => (this.loading = false))
    },
    onWindowClosed () {
      this.active = false
    }
  }
}
</script>

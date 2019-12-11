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
    position: 0, // output raw position index
    remaining: 0 // output raw remaining
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
    retry () {
      const outputContainer = this.$refs.output.$refs.container
      outputContainer.innerHTML = ''
      this.loading = true
      this.loadOutput(0, 0, true, this.readonly, 400)
    },
    onWindowOpened () {
      this.active = true
      this.loadOutput(0, 0, true, this.readonly, 400)
    },
    renderOutput (position, remaining, backward, linesOnly, lines, output) {
      this.remaining = remaining
      this.position = position
      if (remaining === 0) {
        this.loading = false
      }
      const outputContainer = this.$refs.output.$refs.container
      if (backward) {
        outputContainer.innerHTML = output + outputContainer.innerHTML
        const windowContent = this.$refs.window.$refs.content
        windowContent.scrollTop = windowContent.scrollHeight
      } else {
        outputContainer.innerHTML = outputContainer.innerHTML + output
      }
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
        return
      }
      let uri = 'test' + '/output/' + 1 // TODO this is hard-coded
      uri += '?position=' + position
      uri += '&lines=' + lines
      if (backward) {
        uri += '&backward=true'
      }
      if (linesOnly) {
        uri += '&lines_only=true'
      }
      this.$api.get(uri)
        .then((response) => {
          position = parseInt(response.headers['vnd.io.helidon.publisher.position'])
          remaining = parseInt(response.headers['vnd.io.helidon.publisher.remaining'])
          this.renderOutput(position, remaining, backward, linesOnly, lines, response.data)
        })
        .catch(error => {
          this.errored = true
        })
    },
    onWindowClosed () {
      this.active = false
    }
  }
}
</script>

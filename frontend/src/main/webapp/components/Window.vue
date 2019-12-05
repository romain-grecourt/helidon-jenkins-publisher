<template>
  <v-dialog
    scrollable
    v-model="opened">
    <v-card>
      <v-card-title
        class="headline grey lighten-2"
        primary-title>
        <div class="window-title"><slot name="prepend"></slot>{{title}}</div>
        <v-spacer></v-spacer>
        <v-btn icon @click="close()">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-text class="pa-0">
        <slot></slot>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>
<style>
  .window-title {
    flex: 1 1 90%;
    width: 90%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
<script>
  export default {
    name: 'Window',
    props: {
      title: {
        type: String,
        required: true
      },
      id: {
        type: Number,
        required: true
      },
      type: {
        type: String,
        required: true
      }
    },
    created() {
      var windowId = this.id + '-' + this.type;
      this.opened = this.$store.state.pipelineWindowId === windowId
      this.unsubscribe = this.$store.subscribe((mutation, state) => {
        var newState = mutation.payload === windowId;
        if (!this.opened && newState) {
          this.opened = true
        } else if (this.opened && !newState) {
          this.opened = false
        }
      })
    },
    data: () => ({
      opened: false
    }),
    destroyed() {
      this.unsubscribe()
    },
    watch: {
      opened() {
        var windowId = this.id + '-' + this.type;
        var curState = this.$store.state.pipelineWindowId === windowId
        if (!this.opened && curState) {
          this.$store.commit('PIPELINE_WINDOW_ID', 0)
        }
      }
    },
    methods: {
      close() {
        if (this.opened) {
          this.opened = false
          this.$store.commit('PIPELINE_WINDOW_ID', 0)
        }
      }
    },
  }
</script>

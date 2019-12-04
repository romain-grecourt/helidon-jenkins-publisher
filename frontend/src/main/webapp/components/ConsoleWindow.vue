<template>
  <v-dialog
    scrollable
    v-model="dialog"
  >
    <v-card>
      <v-card-title
        class="headline grey lighten-2"
        primary-title
      >
        <v-icon class="mr-4">mdi-console</v-icon>
        <div class="window-title">/Build/sh['Build :) ${BUILD_ID}' > build.txt]</div>
        <v-spacer></v-spacer>
        <v-btn icon @click="dialog = false"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>
      <v-card-text class="pa-0">
        <consoleOutput>
          <slot></slot>
        </consoleOutput>
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
  import ConsoleOutput from './ConsoleOutput'
  export default {
    name: 'ConsoleWindow',
    props: {
      value: {
        type: Boolean,
        default: false
      }
    },
    components: {
      ConsoleOutput
    },
    computed: {
      dialog: {
        get() {return this.value},
        set(dialog) { this.$emit('input', dialog)}
      }
    },
  }
</script>

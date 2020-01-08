<template>
  <v-container
    class="__fill-height"
    fluid
  >
    <v-row>
      <v-col
        align="center"
        justify="center"
        class="mx-0 loading"
      >
        <v-progress-circular
          :width="width"
          :size="size"
          color="primary"
          indeterminate
        />
      </v-col>
    </v-row>
  </v-container>
</template>
<style>
.loading > .v-progress-circular {
  overflow: hidden
}
</style>
<script>
export default {
  name: 'Loading',
  props: {
    size: {
      type: Number,
      default: 150
    },
    width: {
      type: Number,
      default: 10
    }
  }
}
</script>

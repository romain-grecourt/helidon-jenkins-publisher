<template>
  <v-container
    class="fill-height"
    fluid
  >
    <v-row>
      <v-col
        align="center"
        justify="center"
        class="mx-0"
      >
        <v-icon
          :size="size"
        >
          mdi-emoticon-dead
        </v-icon>
        <div
          class="message"
        >
          {{ message }}
        </div>
        <div style="display:block">
          <slot />
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>
<style>
.message {
  margin-top: 10px;
  font-size: 1.5em;
  font-weight: bold;
  color: #E0E0E0;
}
</style>
<script>
export default {
  name: 'Error',
  props: {
    message: {
      type: String,
      default: 'Error !'
    },
    size: {
      type: Number,
      default: 128
    }
  }
}
</script>

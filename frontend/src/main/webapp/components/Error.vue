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
          :size="big? 128 : 96"
        >
          mdi-emoticon-dead
        </v-icon>
        <h1
          v-if="message && big"
          class="mt-2"
        >
          {{ message }}
        </h1>
        <h3 v-else-if="message">
          {{ message }}
        </h3>
        <div style="display:block">
          <slot />
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>
<script>
export default {
  name: 'Error',
  props: {
    message: {
      type: String,
      default: 'Error !'
    },
    big: {
      type: Boolean,
      default: true
    }
  }
}
</script>

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
          size="128"
        >
          mdi-emoticon-dead
        </v-icon>
        <h1
          v-if="message"
          class="mt-2"
        >
          {{ message }}
        </h1>
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
    }
  }
}
</script>

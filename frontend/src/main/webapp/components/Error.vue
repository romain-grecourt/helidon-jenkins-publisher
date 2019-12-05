<template>
  <v-content>
    <v-container
      class="fill-height"
      fluid
    >
      <v-row>
        <v-content
          align="center"
          justify="center"
          class="mx-0"
        >
          <v-icon
            size="128"
          >
            mdi-emoticon-dead
          </v-icon>
          <h1>{{ message }}</h1>
        </v-content>
      </v-row>
    </v-container>
  </v-content>
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

<template>
    <v-container
      class="fill-height"
      fluid
      >
      <v-row>
        <v-col
          align="center"
          justify="center"
          class="mx-0">
          <v-icon
            size="128"
          >
            mdi-emoticon-sad
          </v-icon>
          <h1>{{ message }}</h1>
          <v-btn
            class="mt-6"
            to="/"
          >
            GO TO PIPELINES
          </v-btn>
        </v-col>
      </v-row>
    </v-container>
  </v-content>
</template>
<style>
</style>
<script>
export default {
  name: 'NotFound',
  props: {
    message: {
      type: String,
      default: 'Not found !'
    }
  }
}
</script>

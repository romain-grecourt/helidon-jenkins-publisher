<template>
  <v-app dark>
    <v-content>
      <v-container
        class="fill-height"
        fluid>
        <v-row>
          <v-content
            align="center"
            justify="center"
            class="mx-0">
            <v-icon size="128">mdi-emoticon-dead</v-icon>
            <h1>{{message}}</h1>
            <v-btn class="mt-6" to="/">GO TO PIPELINES</v-btn>
          </v-content>
        </v-row>
      </v-container>
    </v-content>
  </v-app>
</template>
<style>
</style>
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

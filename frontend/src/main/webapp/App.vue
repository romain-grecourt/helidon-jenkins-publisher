<template>
  <v-app>
    <v-app-bar
      app
      color="white"
    >
      <div class="d-flex align-center">
        <v-img
          alt="Helidon Builds Logo"
          class="shrink mr-2"
          contain
          src="static/img/helidon_builds_logo.png"
          transition="scale-transition"
          width="50"
        />
      </div>
    </v-app-bar>

<!--    <v-content>
      <HelloWorld />
    </v-content>-->
  </v-app>
</template>

<script>
import HelloWorld from './components/HelloWorld'

export default {
  name: 'App',

  components: {
    HelloWorld
  },

  data: () => ({
    // put some data
  })
}
</script>

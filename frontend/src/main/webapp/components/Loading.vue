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
          <v-progress-circular
            :width="10"
            :size="150"
            color="primary"
            indeterminate
          ></v-progress-circular>
        </v-content>
      </v-row>
    </v-container>
  </v-content>
</template>
<script>
export default {
  name: 'Loading',
}
</script>
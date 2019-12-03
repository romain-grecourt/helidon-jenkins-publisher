<template>
    <router-view />
</template>
<script>
export default {
    mounted (){
      console.log(this.$route.path)
    }
}
</script>

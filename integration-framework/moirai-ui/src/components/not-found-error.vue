<style scoped>
h1 {
  text-align: center;
}
</style>

<template>
  <div class="NotFound">
    <h1>404: PAGE NOT FOUND</h1>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

@Component
export default class NotFound extends Vue {}
</script>

<script  lang="ts">
import VisContainer from './components/vis-container/vis-container.vue'
export default {
  components :{
    VisContainer: VisContainer
  }
}
</script>

<template>
  <v-app class="v-app-css">
    <v-main id="app">
      <vis-container></vis-container>
    </v-main>
  </v-app>
</template>
<!--App initialized by main.ts -->
<style>
/* --tableBackground will refer to whatever --tableBackground is set to in the component
 where this is applicable */
.v-data-table tbody tr:hover {
  background: var(--tableBackground) !important;
}

.basicanchor:visited,
.basicanchor:hover,
.basicanchor:active {
  color: #29b6f6 !important;
}

#nprogress .bar {
  background: #9c27b0 !important;
}

#nprogress .spinner-icon {
  border-top-color: #9c27b0 !important;
  border-left-color: #9c27b0 !important;
}
</style>

<style scoped>
.v-app-css {
  background: var(--v-backdrops-base) !important;
}

.btn-right {
  float: right !important;
  margin-top: -6px;
  margin-right: -5px;
  margin-bottom: -8px;
}
</style>
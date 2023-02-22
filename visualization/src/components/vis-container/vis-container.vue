<template>
  <simple-vis :jsonInput="inputJson"></simple-vis>
</template>

<script lang="ts">
  import "../../assets/visStyle.css";

  import { Component, Mixins, Vue } from "vue-property-decorator";

  import SimpleVis from "../simple-vis/simple-vis.vue";

  @Component({ 
    components: { 
      SimpleVis 
    } 
  })
  export default class VisContainer extends Vue {
    inputJson: any = [];

    mounted() {
    }
  }
</script>

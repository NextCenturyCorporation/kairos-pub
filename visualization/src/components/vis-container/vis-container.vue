<template>
  <simple-vis :jsonInput="inputJson"></simple-vis>
</template>

<script lang="ts">
  import "../../assets/visStyle.css";

  import { defineComponent } from 'vue';
  import SimpleVis from "../simple-vis/simple-vis.vue";

  export default defineComponent({
    name: 'VisContainer',
    components: { SimpleVis },
    props: {
      inputJson: { type: String, required: false, default: "" },
    }
  });
</script>

<style scoped>
.v-image--expand {
  width: 1500px;
}
/* code {
  background: var(vuetify.theme.themes[theme].backdrops.lighten1);
  color: blue;
} */
.v-application code {
  background: var(--v-backdrops-lighten1);
  color: var(--v-primary-base);
  padding: 5px;
  margin: 10px;
}

.view-wrapper {
  margin: 10px 10% 2em 10%;
}
</style>

<template src="./html/admin-feature-flags.html"></template>

<script lang="ts">
import { Component, Vue, Watch, Mixins } from "vue-property-decorator";
import ModifyFeatureFlags from "../components/modify-feature-flags/modify-feature-flags.vue";
import SystemSettings from "../components/system-settings/system-settings.vue";
import BaseComponent from "./BaseComponent.vue";
@Component({
  components: {
    ModifyFeatureFlags,
    SystemSettings
  }
})
export default class AdminFeatureFlags extends Mixins(BaseComponent) {
  // Sets which tab is defaulted open
  private window: Number = 0;
}
</script>

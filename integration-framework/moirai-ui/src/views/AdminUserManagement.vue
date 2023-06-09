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

<template src="./html/admin-user-management.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import EditUserData from "../components/edit-user-data/edit-user-data.vue";
import ModifyRoleDisplay from "../components/modify-role-display/modify-role-display.vue";
import BaseComponent from "../views/BaseComponent.vue";
@Component({
  components: {
    EditUserData,
    ModifyRoleDisplay
  }
})
export default class AdminUserManagement extends Mixins(BaseComponent) {
  // Sets which tab is defaulted open
  private window: Number = 0;
}
</script>

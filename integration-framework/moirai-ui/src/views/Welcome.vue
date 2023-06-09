<style scoped>
.text--underline {
  text-decoration: underline;
}

.text-subtitle-1 {
  line-height: 1.5em !important;
}

.reduced {
  margin-left: -16px;
  margin-right: -16px;
}

.extra-x-padding {
  padding-left: 10% !important;
  padding-right: 10% !important;
}
</style>

<template src="./html/welcome.html"> </template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import BaseComponent from "../views/BaseComponent.vue";
import { getToken } from "@/utils/cookie-utils";

@Component
export default class Welcome extends Mixins(BaseComponent) {
  mounted() {
    let currentUser = this.$store.getters["user/currentUser"] || {};
    if (currentUser && currentUser.passwordExpiration) {
      var expiringIn = new Date(currentUser.passwordExpiration); // Password Expiration
      var showModel = new Date(new Date().setDate(new Date().getDate() + 21)); // Show Message date offset of 14 days
      if (showModel >= expiringIn) {
        this.$store.dispatch(
          "showErrorAppSnackbarMessage",
          "Your Password is expiring on " +
            expiringIn +
            ". Please update your password under the 'Settings' menu."
        );
      }
    }
  }
  get programAnnouncementBackgroundColor(): string {
    return this.$vuetify.theme.dark ? "backdrops lighten-2" : "grey lighten-3";
  }
  get headerBackgroundColor(): string {
    return this.$vuetify.theme.dark
      ? "blue--text text--lighten-2"
      : "headerBar--text text--lighten-1";
  }
}
</script>

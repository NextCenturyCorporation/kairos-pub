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

<template src="./index.html"></template>

<script>
import {
  setTokenInCookie,
  clearToken,
  hasToken,
  getAdminToken,
  clearAdminToken
} from "@/utils/cookie-utils";
import store from "./store";
import Vue from "vue";
import VueSimpleAlert from "vue-simple-alert";

Vue.use(VueSimpleAlert);

if (process.env.NODE_ENV === "production") {
  console.log = function () { }; //NOSONAR
}

const standardWindowWidthThreshold = 765;

export default {
  mounted() {
    window.addEventListener(
      "keypress",
      function (e) {
        this.activeUser = true;
      }.bind(this)
    );
    window.addEventListener(
      "focus",
      function (e) {
        this.activeUser = true;
      }.bind(this)
    );
    window.addEventListener(
      "click",
      function (e) {
        this.activeUser = true;
      }.bind(this)
    );
  },
  data() {
    return {
      drawer: false,
      snackbar: false,
      errorSnackbar: false,
      shrunkWindow: false,
      inactivitySnackbar: false,
      activeUser: false,
      tokenTimer: Function(),
      activityTimer: Function(),
      keyStrokeInterval: Function(),
      navLinks: []
    };
  },
  created: function () {
    this.$store.subscribe((mutation, state) => {
      console.log("  <mutation> " + mutation.type);
      if (mutation.type === "showAppSnackbar") {
        this.snackbar = true;
      } else if (mutation.type === "showErrorAppSnackbar") {
        this.errorSnackbar = true;
      } else if (mutation.type === "user/invokeLoginExpiryState") {
        this.logout();
      } else if (mutation.type === "user/invokeLoginAlmostExpiredState") {
        this.inactivitySnackbar = true;
      } else if (mutation.type === "user/setLoginTimeout") {
        this.inactivitySnackbar = false;
      } else if (mutation.type === "user/setToken") {
        setTokenInCookie(
          store.getters["user/token"],
          this.$tokenTimerLength * this.$millisInMinute
        );
        this.setTokenTimer();
      } else if (mutation.type === "user/setTokenWithoutRenewal") {
        setTokenInCookie(store.getters["user/token"]);
      } else if (mutation.type === "user/invokeActivityTimer") {
        this.setActivityTimer();
      }
    });
    this.$store.commit("setAppStarted");
    //On refresh if we have a token already, go ahead and attempt to renew, if renew works set the activity timer
    if (hasToken()) {
      let expectedRoute = this.$store.getters["user/rejectedPath"];
      expectedRoute = expectedRoute == "/" ? "welcome" : expectedRoute;

      this.$store.dispatch("user/renewAuthentication").then(() => {
        this.$router.push(expectedRoute);
        this.setActivityTimer();
      });
    } else {
      console.log("Created: no token");
    }
    if (window.innerWidth < standardWindowWidthThreshold) {
      this.shrunkWindow = true;
    }
    // Start interval
    this.setKeyStrokeTimer();
  },
  computed: {
    alternateText() {
      return this.$vuetify.theme.dark ? "primary--text text--darken-1" : "primary--text";
    },
    homePageRoute: function () {
      if (this.isLoggedIn()) {
        return "/welcome";
      } else {
        return "/";
      }
    },
    currentTheme() {
      return this.$vuetify.theme.themes[this.$vuetify.theme.dark ? "dark" : "light"];
    }
  },
  methods: {
    assumed() {
      return getAdminToken();
    },
    backToPreviousUser() {
      this.$store.dispatch("user/previousUser").then(() => {
        this.$store.dispatch("user/renewAuthentication");
        this.$router.push("welcome");
      });
    },
    name() {
      return this.$store.getters["user/currentUser"].username;
    },
    getNavLinks() {
      this.drawer = !this.drawer;
      this.navLinks = [
        {
          icon: "fas fa-user",
          text: "Administrative",
          route: "/",
          class: "override-padding-cst-l " + this.getClassBasedOnIsAdmin(),
          show: this.getClassBasedOnIsAdmin(),
          subLinks: [
            { text: "User Managment", route: "/admin-usermanagement" },
            { text: "Evaluations", route: "/evaluations" },
            { text: "Feature Flags", route: "/admin-featureflags" },
            { text: "Support", route: "/adminFAQs" }
          ]
        },
        { icon: "fas fa-flag", text: "Validation", route: "/validation", class: "", show: true },
        { icon: "fas fa-eye", text: "Visualization", route: "/complex-list", class: "", show: true },
        {
          icon: "fas fa-database",
          text: "File Submissions",
          route: "/FileSubmissions",
          class: "",
          show: true
        },
        {
          icon: "fas fa-database",
          text: "Services",
          route: "/services",
          class: this.getClassBasedOnIsAdmin(),
          show: true
        },
        { icon: "fab fa-docker", text: "Docker", route: "/docker", class: "", show: true },
        { icon: "question_answer", text: "Support", route: "/support", class: "", show: true },
        {
          icon: "settings_applications",
          text: "Settings",
          route: "/settings",
          class: "",
          show: true
        },
        { icon: "info", text: "About", route: "/about", class: "", show: true }
      ];
    },
    onResize() {
      this.shrunkWindow = window.innerWidth < standardWindowWidthThreshold;
    },
    refreshLogin() {
      this.inactivitySnackbar = false;
      this.setActivityTimer();
    },
    getCurrentUser() {
      return this.$store.getters["user/currentUser"] || {};
    },
    getClassBasedOnIsAdmin() {
      return this.$store.getters["user/isAdmin"];
    },
    isLoggedIn() {
      return this.$store.getters["user/loggedIn"];
    },
    logout() {
      console.error("LOG OUT~~~~~~~~~~~~~~~~~~~~");
      clearInterval(this.tokenTimer);
      clearInterval(this.setKeyStrokeTimer);
      clearTimeout(this.activityTimer);
      this.inactivitySnackbar = false;

      clearToken();
      clearAdminToken();
      this.$store.dispatch("user/clearAuthentication").then(() => {
        this.$store.dispatch("showAppSnackbarMessage", "Logout Successful!");
      });
      this.$router.push("/");
    },
    setTokenTimer() {
      clearTimeout(this.tokenTimer);
      if (this.$store.getters["user/token"]) {
        this.tokenTimer = setTimeout(() => {
          if (this.$store.getters["user/token"]) {
            this.$store.dispatch("user/renewAuthentication");
          }
        }, this.$tokenTimerLength * this.$millisInMinute);
      } else {
        logout();
      }
    },
    setActivityTimer() {
      clearTimeout(this.activityTimer);
      this.inactivitySnackbar = false;
      this.activityTimer = setTimeout(() => {
        this.setLogoutTimer();
      }, this.$inactivityTimerLength * this.$millisInMinute);
    },
    setKeyStrokeTimer() {
      this.keyStrokeTimer = setInterval(() => {
        if (this.activeUser) {
          this.activeUser = false;
          this.setActivityTimer();
        }
      }, this.$millisInMinute);
    },
    setLogoutTimer() {
      clearTimeout(this.activityTimer);
      this.inactivitySnackbar = true;
      this.activityTimer = setTimeout(() => {
        this.logout();
      }, this.$logoutTimerLength * this.$millisInMinute);
    }
  }
};
</script>

import Vue from "vue";
import VueRouter, { Route } from "vue-router";
import VModal from "vue-js-modal";
import MoiraiLogin from "../components/moirai-login/moirai-login.vue";

import AdminUserManagement from "../views/AdminUserManagement.vue";
import Evaluations from "../views/Evaluations.vue";
import AdminFeatureFlags from "../views/AdminFeatureFlags.vue";
import AdminFAQs from "../components/modify-faqs/modify-faqs.vue";

import Services from "../views/Services.vue";
import Settings from "../views/Settings.vue";
import ComplexList from "../views/ComplexList.vue";
import Docker from "../views/Docker.vue";
import FileSubmissions from "../views/FileSubmissions.vue";
import Support from "../views/Support.vue";
import Validation from "../views/Validation.vue";
import store from "../store";
import NotFound from "../components/not-found-error.vue";
import About from "../components/About.vue";
import Welcome from "../views/Welcome.vue";
import "../../node_modules/nprogress/nprogress.css";
import "vue-js-modal/dist/styles.css";
import NProgress from "nprogress";

Vue.use(VueRouter);
const ensureLoggedInBeforeRoute = function(to: Route, from: Route, next: any): void {
  let appStarted: boolean = store.getters["appStarted"];
  // If the app isnt started then we go to '/' and store the path if the original destination was not '/'
  if (!appStarted) {
    if (to.path !== "/") {
      store.commit("user/setRejectedPath", to.path);
      next("/");
    } else {
      next();
    }
    return;
  }
  console.log("Router: from " + from.path + ", to " + to.path);
  console.log("Router: has token: " + store.getters["user/hasToken"]);
  if (!store.getters["user/hasToken"]) {
    // If we don't have a token we automatically head back to the login page
    // if (to.path !== "/" && to.path !== "/welcome") {
    // }
    to.path === "/" ? next() : next("/");
  } else {
    // If we have a token lets check to see if we actually have the user stored in memory
    if (!store.getters["user/loggedIn"]) {
      store
        .dispatch("user/updateUserAccount")
        .then(() => {
          // if we are logged in go to welcome instead of login
          to.path === "/" ? next("welcome") : next();
        })
        // if we are not logged in always go to login.
        .catch(() => {
          next("/");
        });
    } else {
      next();
    }
  }
};

const handleRouterApiCallFailure = function(errorStatus: number, next: any): void {
  let errorMessage =
    errorStatus === 401
      ? "Login expired; Please try again"
      : "Unexpected Error; Please contact support";
  store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  next("/");
};

const routes = [
  {
    path: "/",
    name: "Login",
    component: MoiraiLogin,
    beforeEnter: (to: Route, from: Route, next: any) => {
      next();
    }
  },
  {
    path: "/welcome",
    name: "Welcome",
    component: Welcome
  },
  {
    path: "/about",
    name: "About",
    component: About
  },
  {
    path: "/admin-usermanagement",
    name: "User Management",
    component: AdminUserManagement
  },
  {
    path: "/evaluations",
    name: "Evaluations",
    component: Evaluations
  },
  {
    path: "/admin-featureflags",
    name: "Feature Flags",
    component: AdminFeatureFlags
  },
  {
    path: "/services",
    name: "Services",
    component: Services,
    beforeEnter(to: Route, from: Route, next: any): void {
      store
        .dispatch("updateServicesFromBackend")
        .then(function() {
          next();
        })
        .catch(function(errorStatus: number) {
          handleRouterApiCallFailure(errorStatus, next);
        });
    }
  },
  {
    path: "/docker",
    name: "Docker",
    component: Docker
  },
  {
    path: "/settings",
    name: "Settings",
    component: Settings
  },
  {
    path: "/validation",
    name: "Validation",
    component: Validation
  },
  {
    path: "/complex-list",
    name: "Complex Event List",
    component: ComplexList
  },
  { path: "/404", component: NotFound },
  { path: "*", redirect: "/404" },
  {
    path: "/support",
    name: "Support",
    component: Support,
    beforeEnter(to: Route, from: Route, next: any): void {
      store
        .dispatch("faq/retrieveFAQs")
        .then(() => {
          next();
        })
        .catch(function(errorStatus: number) {
          handleRouterApiCallFailure(errorStatus, next);
        });
    }
  },
  {
    path: "/adminFAQs",
    name: "Support",
    component: AdminFAQs,
    beforeEnter(to: Route, from: Route, next: any): void {
      store
        .dispatch("faq/retrieveFAQs")
        .then(() => {
          next();
        })
        .catch(function(errorStatus: number) {
          handleRouterApiCallFailure(errorStatus, next);
        });
    }
  },
  {
    path: "/FileSubmissions",
    name: "File Submissions",
    component: FileSubmissions,
    beforeEnter(to: Route, from: Route, next: any): void {
      store
        .dispatch("ui/retrieveFeatureFlags")
        .then(function() {
          next();
        })
        .catch(function(errorStatus: number) {
          handleRouterApiCallFailure(errorStatus, next);
        });
    }
  }
];

Vue.component("loading", { template: "<div>Loading!</div>" });
Vue.use(VModal, {
  dynamicDefaults: {
    draggable: true,
    resizable: true,
    height: "auto"
  }
});

const router = new VueRouter({
  mode: "history",
  base: process.env.BASE_URL,
  routes
});

router.beforeEach((to: Route, from: Route, next: any) => {
  if (to.name) {
    NProgress.set(0.3);
  }
  ensureLoggedInBeforeRoute(to, from, next);
});

router.afterEach(() => {
  NProgress.done();
});

export default router;

import Vue from "vue";
import App from "./App.vue";
import "./registerServiceWorker";
import router from "./router";
import store from "./store";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;
Vue.prototype.$millisInSecond = 1000;
Vue.prototype.$millisInMinute = 60 * 1000;
Vue.prototype.$tokenTimerLength = 23;
Vue.prototype.$inactivityTimerLength = 15;
Vue.prototype.$logoutTimerLength = 5;

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount("#app");

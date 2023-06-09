import Vue from "vue";
import Vuex from "vuex";
import { ClothoServiceDtoDatabaseTypeEnum, UsersApi, Service, ServicesApi } from "zeus-api";
import * as actions from "./actions";
import content from "./modules/content";
import user from "./modules/user";
import admin from "./modules/admin";
import docker from "./modules/docker";
import faq from "./modules/faq";
import files from "./modules/files";
import ui from "./modules/ui";
import visualization from "./modules/visualization";
import evaluations from "./modules/evaluation";
import experiment from "./modules/experiment";
import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    appStarted: false,
    loginPageSnackbarMessage: "",
    loginSnackbarVisible: false,
    loginErrorSnackbarVisible: false,
    usersApi: new UsersApi(JWTAuthInterceptor.Instance),
    servicesApi: new ServicesApi(JWTAuthInterceptor.Instance),
    performerGroupOptions: ["TA1", "TA2", "TA3", "TA4"],
    typeOptions: ["clotho"],
    subTypeOptions: Object.values(ClothoServiceDtoDatabaseTypeEnum),
    services: [] as Array<Service>
  },
  getters: {
    services(state): Array<Service> {
      return state.services;
    },
    appStarted(state): boolean {
      return state.appStarted;
    }
  },
  mutations: {
    replaceServicesData(vuexStoreState, services): void {
      vuexStoreState.services = services;
    },
    showAppSnackbar(vuexStoreContext, message): void {
      vuexStoreContext.loginPageSnackbarMessage = message;
      vuexStoreContext.loginSnackbarVisible = true;
    },
    showErrorAppSnackbar(vuexStoreContext, message): void {
      vuexStoreContext.loginPageSnackbarMessage = message;
      vuexStoreContext.loginSnackbarVisible = true;
    },
    setAppStarted(vuexStoreContext): void {
      vuexStoreContext.appStarted = true;
    }
  },
  //just set up call to api here - try it
  actions,
  modules: {
    user: user,
    admin: admin,
    content: content,
    docker: docker,
    faq: faq,
    files: files,
    ui: ui,
    visualization: visualization,
    experiment: experiment,
    evaluations: evaluations
  }
});

import { FeatureFlag, UIApi } from "zeus-ui-api";
import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";

const state = () => ({
  featureFlags: [] as Array<FeatureFlag>,
  uiApi: new UIApi(<any>JWTAuthInterceptor.Instance)
});

const getters = {
  getFlagMap(vuexStoreState: any): Map<string, boolean> {
    let flags = new Map<string, boolean>();
    vuexStoreState.featureFlags.forEach(function(flag: FeatureFlag) {
      flags.set(flag.name, flag.enabled);
    });
    return flags;
  }
};

const mutations = {
  setFeatureFlags(vuexStoreState: any, featureFlags: Array<FeatureFlag>): void {
    vuexStoreState.featureFlags = featureFlags;
  }
};

const actions = {
  retrieveFeatureFlags(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.uiApi as UIApi).getActiveFeatureFlags().subscribe({
        next: (response: Array<FeatureFlag>) => {
          vuexStoreContext.commit("setFeatureFlags", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting featureFlags: " + errorInfo);
          reject();
        }
      });
    });
  }
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
};

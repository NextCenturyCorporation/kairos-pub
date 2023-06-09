import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";

import { ContentApi, RetrieveDropdownByKeyRequest, DropdownDao } from "zeus-api";

const state = () => ({
  contentApi: new ContentApi(JWTAuthInterceptor.Instance),
  dropdowns: [] as Array<DropdownDao>
});

const getters = {
  dropdowns(vuexStoreState: any): Array<any> {
    return vuexStoreState.dropdowns;
  }
};

const mutations = {
  setDropDowns(vuexStoreState: any, dropdowns: Array<DropdownDao>): void {
    vuexStoreState.dropdowns = dropdowns;
  }
};

const actions = {
  retrieveDropDownValues(vuexStoreContext: any, requestParamaters: any) {
    return new Promise((resolve, reject) => {
      let request: RetrieveDropdownByKeyRequest = {
        key: requestParamaters.key,
        selectOne: requestParamaters.selectOne
      };
      (vuexStoreContext.state.contentApi as ContentApi).retrieveDropdownByKey(request).subscribe({
        next: (response: Array<DropdownDao>) => {
          vuexStoreContext.commit("setDropDowns", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting Users: " + errorInfo);
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

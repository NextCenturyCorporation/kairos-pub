import { JWTClothoAuthInterceptor } from "@/classes/jwt-clotho-auth-intercepter";

import { QueryApi, GetEventTreeRequest, GetEventListRequest ,SaveOrUpdateRequest } from "clotho-api";

const state = () => ({
  clothoApi: new QueryApi(JWTClothoAuthInterceptor.Instance),
  graphNames: "" as String,
  eventComplexList: [] as Array<any>,
  eventComplex: {} as any
});

const getters = {
  graphNames(vuexStoreState: any): String {
    return vuexStoreState.graphNames;
  },
  eventComplexList(vuexStoreState: any): Array<any> {
    return vuexStoreState.eventComplexList;
  },
  eventComplex(vuexStoreState: any): any {
    return vuexStoreState.eventComplex;
  }
};

const mutations = {
  setGraphNames(vuexStoreState: any, graphNames: string) {
    vuexStoreState.graphNames = graphNames;
  },
  setEventComplexList(vuexStoreState: any, eventComplexList: Array<any>) {
    vuexStoreState.eventComplexList = eventComplexList;
  },
  setEventComplex(vuexStoreState: any, eventComplex: any) {
    vuexStoreState.eventComplex = eventComplex;
  }
};

const actions = {
  retrieveGraphNames(vuexStoreContext: any) {
    return new Promise<void>((resolve, reject) => {
      (vuexStoreContext.state.clothoApi as QueryApi).getNamedGraphs().subscribe({
        next: (response: String) => {
          vuexStoreContext.commit("setGraphNames", response);
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  retrieveEventComplexList(vuexStoreContext: any, graphName: string) {
    return new Promise<void>((resolve, reject) => {
      let request: GetEventListRequest = {
        namedGraph: graphName
      };
      (vuexStoreContext.state.clothoApi as QueryApi).getEventList(request).subscribe({
        next: (response: String) => {
          vuexStoreContext.commit("setEventComplexList", response);
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  retrieveEventComplex(vuexStoreContext: any, request: GetEventTreeRequest) {
    return new Promise<void>((resolve, reject) => {
      (vuexStoreContext.state.clothoApi as QueryApi).getEventTree(request).subscribe({
        next: (response: String) => {
          vuexStoreContext.commit("setEventComplex", response);
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  saveOrUpdateNode(vuexStoreContext: any, request: SaveOrUpdateRequest) {
    return new Promise<void>((resolve, reject) => {
      (vuexStoreContext.state.clothoApi as QueryApi).saveOrUpdate(request).subscribe({
        next: (response: String) => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
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

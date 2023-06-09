import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";

import {
  ExperimentApi,
  CreateEnclaveRequest,
  CreateNodeGroupsRequest,
  DeleteExperimentRequest,
  TerminateNodeGroupsRequest
} from "evaluation-api";

const state = () => ({
  experimentApi: new ExperimentApi(<any>JWTAuthInterceptor.Instance),
  k8sStatus: "" as string
});

const getters = {
  k8sStatus(vuexStoreState: any): string {
    return vuexStoreState.k8sStatus;
  }
};

const mutations = {
  setK8sStatus(vuexStoreState: any, k8sStatus: any): void {
    vuexStoreState.k8sStatus = k8sStatus;
  }
};

const actions = {
  retrieveK8sStatus(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.experimentApi as ExperimentApi).getK8sStatus().subscribe({
        next: (response: any) => {
          vuexStoreContext.commit("setK8sStatus", response.value);
          resolve();
        },
        error: (err: any) => {
          reject();
        }
      });
    });
  },
  createK8sEnv(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.experimentApi as any).createK8sEnv().subscribe({
        next: (response: any) => {
          resolve();
        },
        error: (err: any) => {
          reject();
        }
      });
    });
  },
  destroyK8sEnv(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.experimentApi as ExperimentApi).terminateK8sEnv().subscribe({
        next: (response: any) => {
          resolve();
        },
        error: (err: any) => {
          reject();
        }
      });
    });
  },
  runExperiment(vuexStoreContext: any, createEnclaveRequest: CreateEnclaveRequest) {
    return new Promise((resolve, reject) => {
      let request: CreateNodeGroupsRequest = { createEnclaveRequest };
      (vuexStoreContext.state.experimentApi as ExperimentApi).createNodeGroups(request).subscribe({
        next: (response: any) => {
          resolve();
        },
        error: (err: any) => {
          reject();
        }
      });
    });
  },
  stopExperiment(vuexStoreContext: any, createEnclaveRequest: CreateEnclaveRequest) {
    return new Promise((resolve, reject) => {
      let request: TerminateNodeGroupsRequest = { createEnclaveRequest };
      (vuexStoreContext.state.experimentApi as ExperimentApi)
        .terminateNodeGroups(request)
        .subscribe({
          next: (response: any) => {
            resolve();
          },
          error: (err: any) => {
            reject();
          }
        });
    });
  },
  deleteExperiment(vuexStoreContext: any, deleteExperimentRequest: DeleteExperimentRequest) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.experimentApi as ExperimentApi)
        .deleteExperiment(deleteExperimentRequest)
        .subscribe({
          next: (response: any) => {
            resolve();
          },
          error: (err: any) => {
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

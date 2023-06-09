import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";
import {
  DockerApi,
  DockerRegistry,
  DestroyDockerUploadsRequest,
  CreateDockerRegistryRequest,
  ResetDockerRegistryPasswordRequest
} from "zeus-api";

const state = () => ({
  dockerApi: new DockerApi(JWTAuthInterceptor.Instance),
  registry: null
});
const getters = {
  registry(vuexStoreState: any): DockerRegistry {
    return vuexStoreState.registry;
  }
};
const mutations = {
  setRegistry(vuexStoreState: any, newRegistry: DockerRegistry): void {
    vuexStoreState.registry = newRegistry;
  }
};
const actions = {
  getDockerRegistry(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.dockerApi as DockerApi).getDockerRegistry().subscribe({
        next: response => {
          vuexStoreContext.commit("setRegistry", response);
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("getDockerRegistry failed: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  deleteImage(vuexStoreContext: any, destroyDockerUploadsRequest: DestroyDockerUploadsRequest) {
    return new Promise((resolve, reject) => {
      let request: DestroyDockerUploadsRequest = destroyDockerUploadsRequest;
      (vuexStoreContext.state.dockerApi as DockerApi).destroyDockerUploads(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("deleteImage: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  refreshRegistry(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.dockerApi as DockerApi).refreshDockerRegistry().subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("refreshRegistry: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  createRegistry(vuexStoreContext: any, password: string) {
    return new Promise((resolve, reject) => {
      let request: CreateDockerRegistryRequest = {
        stringRequest: {
          value: password
        }
      };
      (vuexStoreContext.state.dockerApi as DockerApi).createDockerRegistry(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("createRegistry: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  resetRegistryPassword(vuexStoreContext: any, password: string) {
    return new Promise((resolve, reject) => {
      let request: ResetDockerRegistryPasswordRequest = {
        stringRequest: {
          value: password
        }
      };
      (vuexStoreContext.state.dockerApi as DockerApi)
        .resetDockerRegistryPassword(request)
        .subscribe({
          next: () => {
            resolve();
          },
          error: ({ response, status }) => {
            let consoleErrorMessage = response && response.message ? response.message : status;
            console.error("resetRegistryPassword: " + consoleErrorMessage);
            reject(status);
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

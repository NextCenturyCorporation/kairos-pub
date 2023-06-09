import {
  ListServicesRequest,
  ServicesApi,
  Service,
  ProvisionClothoRequest,
  TerminateServiceInstanceRequest,
  UpdateServiceInstanceRequest
} from "zeus-api";

export const showAppSnackbarMessage = function(vuexStoreContext: any, message: string) {
  vuexStoreContext.commit("showAppSnackbar", message);
};
export const showErrorAppSnackbarMessage = function(vuexStoreContext: any, message: string) {
  vuexStoreContext.commit("showErrorAppSnackbar", message);
};
export const updateServicesFromBackend = function(vuexStoreContext: any) {
  return new Promise((resolve, reject) => {
    let request: ListServicesRequest = {};
    (vuexStoreContext.state.servicesApi as ServicesApi).listServices(request).subscribe({
      next: (response: Array<Service>) => {
        response = response ? response : [];
        response = response.filter(function(service) {
          return service.status !== "Terminated";
        });
        response
          ? vuexStoreContext.commit("replaceServicesData", response)
          : vuexStoreContext.commit("replaceServicesData", []);
        resolve();
      },
      error: ({ response, status }) => {
        let errorInfo = response && response.message ? response.message : status;
        console.error("Error from updating services from backend: " + errorInfo);
        vuexStoreContext.commit("replaceServicesData", []);
        reject(status);
      }
    });
  });
};

export const persistNewService = function(vuexStoreContext: any, serviceInfo: any) {
  return new Promise((resolve, reject) => {
    let request: ProvisionClothoRequest = {
      clothoServiceDto: serviceInfo
    };
    (vuexStoreContext.state.servicesApi as ServicesApi).provisionClotho(request).subscribe({
      next: () => {
        resolve();
      },
      error: ({ response, status }) => {
        let errorInfo = response && response.message ? response.message : status;
        console.error("Error persisting new service to the backend: " + errorInfo);
        reject(status);
      }
    });
  });
};
export const persistServiceDeletion = function(vuexStoreContext: any, service: Service) {
  return new Promise((resolve, reject) => {
    let request: TerminateServiceInstanceRequest = { service: service };
    (vuexStoreContext.state.servicesApi as ServicesApi)
      .terminateServiceInstance(request)
      .subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("Error persisting settings to the backend: " + consoleErrorMessage);
          reject(status);
        }
      });
  });
};
export const persistServiceUpdate = function(vuexStoreContext: any, service: Service) {
  return new Promise((resolve, reject) => {
    let request: UpdateServiceInstanceRequest = { service: service };
    (vuexStoreContext.state.servicesApi as ServicesApi).updateServiceInstance(request).subscribe({
      next: () => {
        resolve();
      },
      error: ({ response, status }) => {
        let consoleErrorMessage = response && response.message ? response.message : status;
        console.error("Error persisting settings to the backend: " + consoleErrorMessage);
        reject(status);
      }
    });
  });
};

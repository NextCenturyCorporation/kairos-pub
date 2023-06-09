import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";

import {
  AdminApi,
  JwtUser,
  JwtPermission,
  JwtRole,
  UserDataDto,
  UpdateUserAccountRequest,
  CreateOrUpdateRoleRequest,
  DeleteRolesRequest,
  SettingsApi,
  ZeusSetting,
  PostZeusSettingsRequest,
  DeleteZeusSettingRequest
} from "zeus-api";

import {
  Experiment,
  ExperimentApi,
  DeleteExperimentRequest,
  SaveOrUpdateExperimentRequest,
  NodeTypeResponse
} from "evaluation-api";

import {
  UIApi,
  FeatureFlag,
  UpdateFeatureFlagRequest,
  NewFeatureFlagRequest,
  DeleteFeatureFlagRequest
} from "zeus-ui-api";

import { AssumeUserRequest, StringRequest } from "zeus-api";
import { setTokenInCookie, clearToken, stacheAdminToken, getToken } from "@/utils/cookie-utils";

const state = () => ({
  adminApi: new AdminApi(JWTAuthInterceptor.Instance),
  experimentApi: new ExperimentApi(<any>JWTAuthInterceptor.Instance),
  uiApi: new UIApi(<any>JWTAuthInterceptor.Instance),
  settingsApi: new SettingsApi(JWTAuthInterceptor.Instance),
  users: [] as Array<JwtUser>,
  roles: [] as Array<JwtRole>,
  permissions: [] as Array<JwtPermission>,
  featureFlags: [] as Array<FeatureFlag>,
  experiments: [] as Array<Experiment>,
  settings: [] as Array<ZeusSetting>,
  nodeTypes: {} as NodeTypeResponse
});

const getters = {
  users(vuexStoreState: any): Array<any> {
    return vuexStoreState.users;
  },
  roles(vuexStoreState: any): Array<any> {
    return vuexStoreState.roles;
  },
  permissions(vuexStoreState: any): Array<any> {
    return vuexStoreState.permissions;
  },
  featureFlags(vuexStoreState: any): Array<any> {
    return vuexStoreState.featureFlags;
  },
  experiments(vuexStoreState: any): Array<any> {
    return vuexStoreState.experiments;
  },
  settings(vuexStoreState: any): Array<any> {
    return vuexStoreState.settings;
  },
  nodeTypes(vuexStoreState: any): any {
    return vuexStoreState.nodeTypes;
  }
};

const mutations = {
  setUsers(vuexStoreState: any, users: Array<JwtUser>): void {
    vuexStoreState.users = users;
  },
  setRoles(vuexStoreState: any, roles: Array<JwtRole>): void {
    vuexStoreState.roles = roles;
  },
  setPermissions(vuexStoreState: any, permissions: Array<JwtPermission>): void {
    vuexStoreState.permissions = permissions;
  },
  setFeatureFlags(vuexStoreState: any, featureFlags: Array<FeatureFlag>): void {
    vuexStoreState.featureFlags = featureFlags;
  },
  setExperiments(vuexStoreState: any, experiments: Array<Experiment>): void {
    vuexStoreState.experiments = experiments;
  },
  setSettings(vuexStoreState: any, settings: Array<any>) {
    vuexStoreState.settings = settings;
  },
  setNodeTypes(vuexStoreState: any, nodes: NodeTypeResponse): void {
    vuexStoreState.nodeTypes = nodes;
  }
};

const actions = {
  retrieveUsers(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.adminApi as AdminApi).listUsers().subscribe({
        next: (response: Array<JwtUser>) => {
          vuexStoreContext.commit("setUsers", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting Users: " + errorInfo);
          reject();
        }
      });
    });
  },
  assumeUser(vuexStoreContext: any, userName: string) {
    let request: AssumeUserRequest = {
      stringRequest: {
        value: userName
      }
    };

    const millisInMinute = 60 * 1000;
    const tokenTimerLength = 23;

    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.adminApi as AdminApi).assumeUser(request).subscribe({
        next: response => {
          stacheAdminToken(getToken(), tokenTimerLength * millisInMinute);
          clearToken();
          setTokenInCookie(response.value as string, tokenTimerLength * millisInMinute);
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("Login failed: " + consoleErrorMessage);
          console.error(response);
          reject(status);
        }
      });
    });
  },
  retrieveAllRoles(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.adminApi as AdminApi).listRoles().subscribe({
        next: (response: Array<JwtRole>) => {
          vuexStoreContext.commit("setRoles", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting Roles: " + errorInfo);
          reject(status);
        }
      });
    });
  },
  retrieveAllPermissions(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.adminApi as AdminApi).listPermissions().subscribe({
        next: (response: Array<JwtPermission>) => {
          vuexStoreContext.commit("setPermissions", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting Permissions: " + errorInfo);
          reject();
        }
      });
    });
  },
  retrieveAllFeatureFlags(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.uiApi as UIApi).getFeatureFlags().subscribe({
        next: (response: Array<FeatureFlag>) => {
          vuexStoreContext.commit("setFeatureFlags", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting Feature Flags: " + errorInfo);
          reject();
        }
      });
    });
  },
  retrieveExperiments(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.experimentApi as ExperimentApi).retrieveExperiments().subscribe({
        next: (response: Array<Experiment>) => {
          vuexStoreContext.commit("setExperiments", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting Experiments: " + errorInfo);
          reject();
        }
      });
    });
  },
  retrieveNodeTypes(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.experimentApi as ExperimentApi).retrieveK8sNodeTypes().subscribe({
        next: (response: NodeTypeResponse) => {
          vuexStoreContext.commit("setNodeTypes", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting K8s Node Types: " + errorInfo);
          reject();
        }
      });
    });
  },
  persistExperimentState(vuexStoreContext: any, experiment: Experiment) {
    return new Promise((resolve, reject) => {
      let request: SaveOrUpdateExperimentRequest = { experiment: experiment };
      (vuexStoreContext.state.experimentApi as ExperimentApi)
        .saveOrUpdateExperiment(request)
        .subscribe({
          next: () => {
            resolve();
          },
          error: (err: any) => {
            reject(err);
          }
        });
    });
  },
  deleteExperiment(vuexStoreContext: any, id: string) {
    return new Promise((resolve, reject) => {
      let request: DeleteExperimentRequest = { id: id };
      (vuexStoreContext.state.experimentApi as ExperimentApi).deleteExperiment(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  persistUserSettingsState(vuexStoreContext: any, user: UserDataDto) {
    return new Promise((resolve, reject) => {
      let request: UpdateUserAccountRequest = { userDataDto: user };
      (vuexStoreContext.state.adminApi as AdminApi).updateUserAccount(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  persistRoleState(vuexStoreContext: any, role: JwtRole) {
    return new Promise((resolve, reject) => {
      let request: CreateOrUpdateRoleRequest = { jwtRole: [role] };
      (vuexStoreContext.state.adminApi as AdminApi).createOrUpdateRole(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  deleteRole(vuexStoreContext: any, role: JwtRole) {
    return new Promise((resolve, reject) => {
      let request: DeleteRolesRequest = { jwtRole: role };
      (vuexStoreContext.state.adminApi as AdminApi).deleteRoles(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  createFeatureFlag(vuexStoreContext: any, flag: FeatureFlag) {
    return new Promise((resolve, reject) => {
      let request: NewFeatureFlagRequest = { featureFlag: flag };
      (vuexStoreContext.state.uiApi as UIApi).newFeatureFlag(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  updateFeatureFlag(vuexStoreContext: any, flag: FeatureFlag) {
    return new Promise((resolve, reject) => {
      let request: UpdateFeatureFlagRequest = { id: flag.id, featureFlag: flag };
      (vuexStoreContext.state.uiApi as UIApi).updateFeatureFlag(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  deleteFeatureFlag(vuexStoreContext: any, flag: FeatureFlag) {
    return new Promise((resolve, reject) => {
      let request: DeleteFeatureFlagRequest = { featureFlag: flag };
      (vuexStoreContext.state.uiApi as UIApi).deleteFeatureFlag(request).subscribe({
        next: () => {
          resolve();
        },
        error: (err: any) => {
          reject(err);
        }
      });
    });
  },
  retrieveSettings(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.settingsApi as SettingsApi).getZeusSettings().subscribe({
        next: (response: Array<ZeusSetting>) => {
          vuexStoreContext.commit("setSettings", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting settings: " + errorInfo);
          reject();
        }
      });
    });
  },
  postSetting(vuexStoreContext: any, setting: ZeusSetting) {
    return new Promise((resolve, reject) => {
      let request: PostZeusSettingsRequest = {
        zeusSetting: setting
      };
      (vuexStoreContext.state.settingsApi as SettingsApi).postZeusSettings(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  deleteSetting(vuexStoreContext: any, temp: string) {
    return new Promise((resolve, reject) => {
      let request: DeleteZeusSettingRequest = {
        id: temp
      };
      (vuexStoreContext.state.settingsApi as SettingsApi).deleteZeusSetting(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
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

import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";
import {
  UsersApi,
  SecurityApi,
  AuthenticateRequest,
  RegisterRequest,
  ForgotUsernameRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  SetPasswordRequest,
  StringResponse,
  UpdateUserSettingsRequest,
  JwtUser,
  ZeusLoginRequest,
  SettingsDto
} from "zeus-api";

import { deepClone } from "@/utils/otherFunctions";
import { getToken, clearToken, clearAdminToken, getAdminToken } from "@/utils/cookie-utils";

const millisInSecond: number = 1000;
const millisInMinute: number = 60 * millisInSecond;

const state = () => ({
  usersApi: new UsersApi(JWTAuthInterceptor.Instance),
  SecurityApi: new SecurityApi(JWTAuthInterceptor.Instance),
  currentUser: {
    darkMode: true
  },
  token: "",
  rejectedPath: "/"
});
export const getters = {
  loggedIn(vuexStoreState: any): boolean {
    return vuexStoreState.currentUser && vuexStoreState.currentUser.id ? true : false;
  },
  currentUser(vuexStoreState: any): JwtUser {
    return vuexStoreState.currentUser;
  },
  token(vuexStoreState: any): string {
    return vuexStoreState.token;
  },
  hasToken(vuexStoreState: any): boolean {
    return vuexStoreState.token != null && vuexStoreState.token != "" ? true : false;
  },
  rejectedPath(vuexStoreState: any): string {
    return vuexStoreState.rejectedPath;
  },
  isAdmin(vuexStoreState: any): boolean {
    if (vuexStoreState.currentUser.roles != null) {
      for (let element of vuexStoreState.currentUser.roles) {
        if (element.name == "ADMIN") {
          return true;
        }
      }
    }
    return false;
  }
};
export const mutations = {
  setUserAccountData(vuexStoreState: any, newUserInfo: JwtUser): void {
    vuexStoreState.currentUser = newUserInfo;
  },
  setCurrentUserSettings(vuexStoreState: any, settings: any): void {
    let clonedSettings = deepClone(settings);
    vuexStoreState.currentUser = {
      ...vuexStoreState.currentUser,
      ...clonedSettings,
      password: ""
    };
  },
  clearCurrentUserSettings(vuexStoreState: any): void {
    vuexStoreState.currentUser = {};
  },
  invokeActivityTimer(vuexStoreState: any): void {},
  invokeLoginExpiryState(vuexStoreState: any): void {},
  setToken(vuexStoreState: any, newToken: string): void {
    vuexStoreState.token = newToken;
    console.log(vuexStoreState);
  },
  setTokenWithoutRenewal(vuexStoreState: any, newToken: string): void {
    vuexStoreState.token = newToken;
  },
  clearToken(vuexStoreState: any): void {
    vuexStoreState.token = null;
  },
  setRejectedPath(vuexStoreState: any, path: string) {
    vuexStoreState.rejectedPath = path;
  },
  clearPrevToken(vuexStoreState: any) {
    clearAdminToken();
  }
};
const actions = {
  authenticate(vuexStoreContext: any, zeusLoginRequest: ZeusLoginRequest) {
    return new Promise((resolve, reject) => {
      vuexStoreContext.commit("clearCurrentUserSettings");
      let request: AuthenticateRequest = { zeusLoginRequest: zeusLoginRequest };
      (vuexStoreContext.state.usersApi as UsersApi).authenticate(request).subscribe({
        next: response => {
          vuexStoreContext.commit("setToken", response.value);
          vuexStoreContext.commit("invokeActivityTimer", response.value);
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("Login failed: " + consoleErrorMessage);
          vuexStoreContext.commit("invokeLoginExpiryState");
          reject(status);
        }
      });
    });
  },
  changeUser(vuexStoreContext: any) {
    vuexStoreContext.commit("setToken", getToken());
    vuexStoreContext.commit("invokeActivityTimer", getToken());
  },
  previousUser(vuexStoreContext: any) {
    clearToken();
    vuexStoreContext.commit("setToken", getAdminToken());
    vuexStoreContext.dispatch("updateUserAccount");
    vuexStoreContext.commit("invokeActivityTimer", getAdminToken());
    vuexStoreContext.commit("clearPrevToken");
  },
  renewAuthentication(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.SecurityApi as SecurityApi).requestAuthTokenRenewal().subscribe({
        next: response => {
          vuexStoreContext.commit("setToken", response.value);
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("Renew failed: " + consoleErrorMessage);
          vuexStoreContext.commit("invokeLoginExpiryState");
          reject(status);
        }
      });
    });
  },
  clearAuthentication(vuexStoreContext: any) {
    vuexStoreContext.commit("clearCurrentUserSettings");
    vuexStoreContext.commit("clearToken");
  },
  register(vuexStoreContext: any, request: RegisterRequest) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.usersApi as UsersApi).register(request).subscribe({
        next: (response: StringResponse) => {
          resolve(response);
        },
        error: (err: any) => {
          reject(err);
        },
        complete: () => {
          resolve();
        }
      });
    });
  },
  updateUserAccount(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.usersApi as UsersApi).getUserAccount().subscribe({
        next: (response: JwtUser) => {
          vuexStoreContext.commit("setUserAccountData", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error from updating user account from backend: " + errorInfo);
          vuexStoreContext.dispatch("clearAuthentication");
          reject(status);
        }
      });
    });
  },
  recoverUsername(vuexStoreContext: any, emailAddress: string) {
    let request: ForgotUsernameRequest = {
      stringRequest: {
        value: emailAddress
      }
    };

    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.usersApi as UsersApi).forgotUsername(request).subscribe({
        next: (response: StringResponse) => {
          resolve(response);
        },
        error: (err: any) => {
          reject(err);
        },
        complete: () => {
          resolve();
        }
      });
    });
  },
  recoverPasswordStep1(vuexStoreContext: any, username: string) {
    let request: ForgotPasswordRequest = {
      stringRequest: {
        value: username
      }
    };

    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.usersApi as UsersApi).forgotPassword(request).subscribe({
        next: (response: StringResponse) => {
          resolve(response);
        },
        error: (err: any) => {
          reject(err);
        },
        complete: () => {
          resolve();
        }
      });
    });
  },
  recoverPasswordStep2(vuexStoreContext: any, accessCode: string) {
    let request: ResetPasswordRequest = {
      stringRequest: {
        value: accessCode
      }
    };

    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.usersApi as UsersApi).resetPassword(request).subscribe({
        next: (response: StringResponse) => {
          vuexStoreContext.commit("setTokenWithoutRenewal", response.value);
          resolve(response);
        },
        error: (err: any) => {
          reject(err);
        },
        complete: () => {
          resolve();
        }
      });
    });
  },
  recoverPasswordStep3(vuexStoreContext: any, password: string) {
    let request: SetPasswordRequest = {
      stringRequest: {
        value: password
      }
    };

    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.usersApi as UsersApi).setPassword(request).subscribe({
        next: (response: StringResponse) => {
          resolve(response);
        },
        error: (err: any) => {
          reject(err);
        },
        complete: () => {
          resolve();
        }
      });
    });
  },
  persistUserSettingsState(vuexStoreContext: any, settings: SettingsDto) {
    return new Promise((resolve, reject) => {
      let request: UpdateUserSettingsRequest = { settingsDto: settings };
      (vuexStoreContext.state.usersApi as UsersApi).updateUserSettings(request).subscribe({
        next: () => {
          vuexStoreContext.commit("setCurrentUserSettings", settings);
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

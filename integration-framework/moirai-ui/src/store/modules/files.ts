import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";
import {
  FilesApi,
  StoredFile,
  FileDisplayInfo,
  GetFilesRequest,
  GetFilesByExperimentRequest,
  GetFilesByEvaluationRequest,
  DisplayFilesRequest,
  DeleteFileRequest,
  DownloadFileRequest,
  UpdateFileRequest,
  FileUploadRequest,
  HandleUploadRequest,
  ValidationRequest,
  ValidationResponse,
  ValidateInputRequest,
  StringResponse
} from "zeus-files-api";
import { SettingsApi, GetZeusSettingRequest, ZeusSetting } from "zeus-api";
import { saveFile } from "@/utils/otherFunctions";

const state = () => ({
  categoryOptions: [] as Array<string>,
  filesApi: new FilesApi(<any>JWTAuthInterceptor.Instance),
  settingsApi: new SettingsApi(JWTAuthInterceptor.Instance),
  myStoredFiles: [] as Array<StoredFile>,
  publicStoredFiles: [] as Array<StoredFile>,
  evaluationFiles: [] as Array<StoredFile>,
  submissions: [] as Array<string>,
  defaultSubmission: "" as String,
  fileDisplayInfoMap: new Map(),
  validationOutput: {} as ValidationResponse
});

const getters = {
  categoryOptions(vuexStoreState: any): Array<string> {
    return vuexStoreState.categoryOptions;
  },
  myStoredFiles(vuexStoreState: any): Array<any> {
    return vuexStoreState.myStoredFiles;
  },
  publicStoredFiles(vuexStoreState: any): Array<any> {
    return vuexStoreState.publicStoredFiles;
  },
  getEvaluationFiles(vuexStoreState: any): Array<any> {
    return vuexStoreState.evaluationFiles;
  },
  fileDisplayInfoMap(vuexStoreState: any): Map<string, FileDisplayInfo> {
    return vuexStoreState.fileDisplayInfoMap;
  },
  getSubmissions(vuexStoreState: any): ZeusSetting {
    return vuexStoreState.submissions;
  },
  getDefaultSubmission(vuexStoreState: any): String {
    return vuexStoreState.defaultSubmission;
  },
  getValidationOutput(vuexStoreState: any): ValidationResponse {
    return vuexStoreState.validationOutput;
  }
};

const mutations = {
  setCategoryOptions(vuexStoreState: any, categoryOptions: Array<string>): void {
    vuexStoreState.categoryOptions = categoryOptions;
  },
  setMyStoredFiles(vuexStoreState: any, myStoredFiles: Array<StoredFile>): void {
    vuexStoreState.myStoredFiles = myStoredFiles;
  },
  setPublicStoredFiles(vuexStoreState: any, publicStoredFiles: Array<StoredFile>): void {
    vuexStoreState.publicStoredFiles = publicStoredFiles;
  },
  setEvaluationFiles(vuexStoreState: any, evaluationFiles: Array<StoredFile>): void {
    vuexStoreState.evaluationFiles = evaluationFiles;
  },
  setFileDisplayInfo(vuexStoreState: any, fileDisplayInfo: FileDisplayInfo): void {
    vuexStoreState.fileDisplayInfoMap.set(fileDisplayInfo.fileId, fileDisplayInfo);
  },
  setSubmissions(vuexStoreState: any, submissions: Array<string>) {
    vuexStoreState.submissions = submissions;
  },
  setDefaultSubmission(vuexStoreState: any, submission: string): void {
    vuexStoreState.defaultSubmission = submission;
  },
  setValidationOutput(vuexStoreState: any, validationResponse: ValidationResponse): void {
    vuexStoreState.validationOutput = validationResponse;
  }
};

const actions = {
  retrieveSubmissionType(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      let request: GetZeusSettingRequest = {
        id: "file.groups"
      };

      (vuexStoreContext.state.settingsApi as SettingsApi).getZeusSetting(request).subscribe({
        next: (response: ZeusSetting) => {
          vuexStoreContext.commit("setSubmissions", response.value.split(","));
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting fileDisplay info: " + errorInfo);
          reject();
        }
      });
    });
  },

  retrieveDefaultSubmissionType(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      let request: GetZeusSettingRequest = {
        id: "file.defaultgroup"
      };

      (vuexStoreContext.state.settingsApi as SettingsApi).getZeusSetting(request).subscribe({
        next: (response: ZeusSetting) => {
          vuexStoreContext.commit("setDefaultSubmission", response.value);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting fileDisplay info: " + errorInfo);
          reject();
        }
      });
    });
  },

  retrieveFileDisplayInfo(vuexStoreContext: any, id: string) {
    return new Promise((resolve, reject) => {
      let request: DisplayFilesRequest = {
        id: id
      };

      (vuexStoreContext.state.filesApi as FilesApi).displayFiles(request).subscribe({
        next: (response: FileDisplayInfo) => {
          vuexStoreContext.commit("setFileDisplayInfo", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting fileDisplay info: " + errorInfo);
          reject();
        }
      });
    });
  },
  retrieveMyFiles(vuexStoreContext: any, experiment: string) {
    return new Promise((resolve, reject) => {
      let request: GetFilesRequest = {
        experiment: experiment
      };
      (vuexStoreContext.state.filesApi as FilesApi).getFiles(request).subscribe({
        next: (response: Array<StoredFile>) => {
          vuexStoreContext.commit("setMyStoredFiles", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting files: " + errorInfo);
          reject();
        }
      });
    });
  },
  retrievePublicFiles(vuexStoreContext: any, experiment: string) {
    return new Promise((resolve, reject) => {
      let request: GetFilesByExperimentRequest = {
        experiment: experiment
      };

      (vuexStoreContext.state.filesApi as FilesApi).getFilesByExperiment(request).subscribe({
        next: (response: Array<StoredFile>) => {
          vuexStoreContext.commit("setPublicStoredFiles", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting files: " + errorInfo);
          reject();
        }
      });
    });
  },
  retrieveEvaluationFiles(vuexStoreContext: any, evaluation: string) {
    return new Promise((resolve, reject) => {
      let request: GetFilesByEvaluationRequest = {
        evaluation: evaluation
      };

      (vuexStoreContext.state.filesApi as FilesApi).getFilesByEvaluation(request).subscribe({
        next: (response: Array<StoredFile>) => {
          vuexStoreContext.commit("setEvaluationFiles", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting files: " + errorInfo);
          reject();
        }
      });
    });
  },
  deleteFile(vuexStoreContext: any, deleteFileRequest: DeleteFileRequest) {
    return new Promise((resolve, reject) => {
      let request: DeleteFileRequest = deleteFileRequest;
      (vuexStoreContext.state.filesApi as FilesApi).deleteFile(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("DeleteFile: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  downloadFile(vuexStoreContext: any, file: StoredFile) {
    let request: DownloadFileRequest = {
      id: file.id
    };
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.filesApi as FilesApi).downloadFile(request).subscribe({
        next: (response: Blob) => {
          saveFile(file.filename, response);
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("DownloadFile: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  updateFile(vuexStoreContext: any, file: StoredFile) {
    return new Promise((resolve, reject) => {
      let request: UpdateFileRequest = {
        id: file.id,
        storedFile: file
      };
      console.log(request);
      (vuexStoreContext.state.filesApi as FilesApi).updateFile(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("UpdateFile: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  uploadFile(vuexStoreContext: any, fileUploadRequest: FileUploadRequest) {
    return new Promise((resolve, reject) => {
      let request: HandleUploadRequest = fileUploadRequest;
      (vuexStoreContext.state.filesApi as FilesApi).handleUpload(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("UploadFailed: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  validateInput(vuexStoreContext: any, input: string) {
    return new Promise((resolve, reject) => {
      let validationRequest: ValidationRequest = {
        input: input
      };
      let request: ValidateInputRequest = {
        validationRequest: validationRequest
      };
      (vuexStoreContext.state.filesApi as FilesApi).validateInput(request).subscribe({
        next: (response: ValidationResponse) => {
          vuexStoreContext.commit("setValidationOutput", response);
          resolve(response);
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error validating: " + errorInfo);
          reject({ response, status });
        }
      });
    });
  },
  syncFiles(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.filesApi as FilesApi).syncFiles().subscribe({
        next: (response: StringResponse) => {
          resolve(response);
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error syncing: " + errorInfo);
          reject({ response, status });
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

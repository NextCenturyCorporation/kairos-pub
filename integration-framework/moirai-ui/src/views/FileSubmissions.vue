<template src="./html/FileSubmissions.html"></template>

<script lang="ts">
import { Component, Mixins, Watch } from "vue-property-decorator";
import BaseComponent from "../views/BaseComponent.vue";
import FileUploader from "../components/file-uploader/file-uploader.vue";
import FileViewer from "../components/file-viewer/file-viewer.vue";
import FileDisplay from "../components/file-display/file-display.vue";
import { StoredFile, FileDisplayInfo } from "zeus-files-api";
import { deepClone } from "../utils/otherFunctions";
import { dateInRange } from "../utils/date";

import { FileUploadRequest, DeleteFileRequest } from "zeus-files-api";
import { Dataset } from "evaluation-api";

@Component({
  components: {
    FileUploader,
    FileViewer,
    FileDisplay
  }
})
export default class FileSubmissions extends Mixins(BaseComponent) {
  private submissionNames: Array<String> = [];
  private allSubmissions: Map<String, Object> = new Map();
  private selectedSubmission: String = "";
  private myFiles: Array<any> = [];
  private publicFiles: Array<any> = [];
  private categoryOptions: Array<Dataset> = [];
  private myDownloadableCategories: Array<String> = [];
  private publicDownloadableCategories: Array<String> = [];
  private window: Number = 1;
  private viewFileDialog: boolean = false;
  private fileDisplayInfo: FileDisplayInfo = this.getEmptyFileDisplayInfo();
  
  @Watch("selectedSubmission")
  private watchSelectedName(newValue: string): void {
    this.getMyFiles();
    this.getPublicFiles();
  }

  private created(): void {
    this.getSubmissions();
  }

  private displayFile(file: StoredFile): void {
    this.$store
      .dispatch("files/retrieveFileDisplayInfo", file.id)
      .then(() => {
        this.fileDisplayInfo = this.$store.getters["files/fileDisplayInfoMap"].get(file.id);
        this.viewFileDialog = true;
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private getEmptyFileDisplayInfo(): FileDisplayInfo {
    return {
      fileId: "",
      filename: "",
      base: "",
      validation: "",
      humanReadable: ""
    };
  }

  private stopDisplayFile(): void {
    this.fileDisplayInfo = this.getEmptyFileDisplayInfo();
    this.viewFileDialog = false;
  }

  private getMyFiles(): void {
    this.$store
      .dispatch("files/retrieveMyFiles", this.selectedSubmission)
      .then(() => {
        this.myFiles = this.$store.getters["files/myStoredFiles"];
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private getPublicFiles(): void {
    this.$store
      .dispatch("files/retrievePublicFiles", this.selectedSubmission)
      .then(() => {
        this.publicFiles = this.$store.getters["files/publicStoredFiles"];
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private uploadFiles(fileUploadRequests: Array<FileUploadRequest>): void {
    this.uploadFilesWithTracking(fileUploadRequests, [], []);
  }

  private uploadFilesWithTracking(
    fileUploadRequests: Array<FileUploadRequest>,
    successList: Array<String>,
    failList: Array<String>
  ): void {
    if (fileUploadRequests.length === 0) {
      return;
    } else {
      let uploadRequest: FileUploadRequest = fileUploadRequests.pop() as FileUploadRequest;
      uploadRequest.experiment = this.selectedSubmission.toString();
      uploadRequest.extraData = "{}";
      this.$store
        .dispatch("files/uploadFile", uploadRequest)
        .then(() => {
          successList.push(uploadRequest.filename);
        })
        .catch(errorStatus => {
          failList.push(uploadRequest.filename);
        })
        .finally(() => {
          if (fileUploadRequests.length > 0) {
            this.uploadFilesWithTracking(fileUploadRequests, successList, failList);
          } else {
            if (failList.length > 0) {
              this.$alert(failList.join(",  "), "The following files failed to upload", "warning");
            } else {
              this.$alert("All files uploaded successfully", "Success", "success");
            }
            this.getMyFiles();
          }
        });
    }
  }

  private deleteFile(id: String): void {
    let deleteFileRequest: DeleteFileRequest = {
      id: id as string
    };

    this.$store
      .dispatch("files/deleteFile", deleteFileRequest)
      .then(() => {
        this.getMyFiles();
        this.getPublicFiles();
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private updateFile(file: StoredFile): void {
    this.$store
      .dispatch("files/updateFile", file)
      .then(() => {
        this.getMyFiles();
        this.getPublicFiles();
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }
  private getSubmissions() {
    this.$store
      .dispatch("evaluations/retrieveEvaluations")
      .then(() => {
        let evaluations = this.$store.getters["evaluations/evaluations"];
        // sorts array based on creation date
        evaluations = evaluations.sort((a: any, b: any) => a.creationDate - b.creationDate);
        for (const evaluation of evaluations) {
          // add evaluation objects to hash map with names as key
          this.allSubmissions.set(evaluation.name, evaluation);
          this.submissionNames.push(evaluation.name);
        }

        this.selectedSubmission = this.submissionNames[0];
      })
      .catch(status => {
        this.handleErrorStatus(status);
      })
      .then(() => {
        this.updateSubmission();
      });
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }

  private updateSubmission(): void {
    this.myFiles = [];
    this.publicFiles = [];
    this.categoryOptions = [];
    this.myDownloadableCategories = [];
    this.publicDownloadableCategories = [];

    if (this.selectedSubmission) {
      this.myDownloadableCategories.push("ALL");

      // @ts-ignore: Object is possibly 'null'.
      // ^ignoring error because we know that selectedsubmission is not null since we just null checked...
      for (const dataset of this.allSubmissions.get(this.selectedSubmission).datasets) {
        if (dataset.enabled && dateInRange(dataset.startDate, dataset.endDate)) {
          if (dataset.upload) {
            this.categoryOptions.push(dataset);
          }
          if (dataset.download) {
            this.publicDownloadableCategories.push(dataset.name);
          }
        }
      }
    }
    this.$forceUpdate();
  }
}
</script>

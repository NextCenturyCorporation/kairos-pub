<template src="./file-viewer.html"></template>

<script lang="ts">
import mixins from "vue-class-component";
import { Component, Vue } from "vue-property-decorator";
import { StoredFile } from "zeus-files-api";
import { Dataset } from "evaluation-api";

const FileViewerProps = Vue.extend({
  props: {
    tableData: Array,
    allowDelete: Boolean,
    allowDownload: Array,
    allowAccessChange: Boolean,
    changableCategories: Array,
    downloadableCategories: Array
  }
});

@Component
export default class FileViewer extends mixins(FileViewerProps) {
  private search: string = "";
  private tableLoadingFlag: boolean = false;
  private deleteDockerDialog: boolean = false;
  private deleteDockerSnackbar: boolean = false;
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";

  private deleteFileDialog: boolean = false;
  private selectedFile: StoredFile = null as any;
  private sortBy: Array<String> = ["dateReceived"];
  private defaultHeaders: Array<any> = [
    // { text: "", value: "data-table-expand" },
    { text: "Date Received", align: "left", value: "dateReceived" },
    { text: "Team Name", value: "owner" },
    { text: "Category", value: "category" },
    { text: "Filename", value: "filename" }
  ];
  private headers: Array<any> = [];

  private created(): void {
    this.headers = this.getHeaders();
  }

  private getName(name: String): String {
    let s: Array<String> = name.split("/");
    return s[s.length - 1];
  }

  private getDownloadLink(id: String): String {
    return "/zeus/files/" + id;
  }

  private isDownloadAllowed(file: any): boolean {
    if (this.downloadableCategories) {
      return (
        this.downloadableCategories.includes(file.category) ||
        this.downloadableCategories.includes("ALL")
      );
    } else {
      return false;
    }
  }

  private getHeaders(): Array<any> {
    let h: Array<any> = this.defaultHeaders;
    if (this.allowAccessChange) {
      h.push({ text: "Submitted", value: "publicAccess", sortable: false });
    }
    h.push({ text: "View", value: "view" });
    h.push({ text: "Download", value: "download", sortable: false });
    if (this.allowDelete) {
      h.push({ text: "Delete", value: "delete", sortable: false });
    }
    return this.defaultHeaders;
  }

  private selectFile(file: StoredFile): void {
    this.selectedFile = file;
  }

  private displayFile(file: StoredFile): void {
    this.$emit("file-display", file);
  }

  private deleteFileConfirmed(): void {
    this.$emit("file-delete", this.selectedFile.id);
    this.deleteFileDialog = false;
  }

  private downloadFile(file: StoredFile): void {
    this.$store.dispatch("files/downloadFile", file);
  }

  private changeAccess(file: StoredFile): void {
    this.$emit("file-update", file);
  }

  private canChangeSubmit(file: StoredFile): boolean {
    if (!this.changableCategory(file.category)) {
      return false;
    } else if (file.canSubmit === false) {
      return false;
    } else {
      return true;
    }
  }

  private changableCategory(category: String): boolean {
    var found: boolean = false;
    this.changableCategories.forEach( cat => {
      if (category === (cat as Dataset).name) {
        found = true;
      }
    });
    return found;
  }

  private canDelete(file: StoredFile): boolean {
    if (file.publicAccess === false) {
      // we only disable delete for submitted files
      return true;
    }
    return false;
  }
}
</script>

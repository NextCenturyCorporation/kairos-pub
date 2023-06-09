<style scoped>
.v-image--expand {
  width: 1500px;
}

/* code {
  background: var(vuetify.theme.themes[theme].backdrops.lighten1);
  color: blue;
} */
.v-application code {
  background: var(--v-backdrops-lighten1);
  color: var(--v-primary-base);
  padding: 5px;
  margin: 10px;
}
</style>
<template src="./html/docker.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import { DockerRegistry, DockerUpload, DestroyDockerUploadsRequest } from "zeus-api";
import { toLocaleDateTime, standardToUserTimezone } from "../utils/date";
import { copyToClipboard } from "../utils/otherFunctions";

import BaseComponent from "../views/BaseComponent.vue";
@Component
export default class Docker extends Mixins(BaseComponent) {
  private search: string = "";
  private registry: DockerRegistry = null as any;

  // Image table
  private headers: Array<any> = [
    { text: "", value: "data-table-expand", sortable: false },
    { text: "Repository", align: "left", value: "repo" },
    { text: "Tags", value: "tag" },
    { text: "Date Received", value: "lastModified" },
    { text: "Image URI", value: "dockerimagelocation" },
    { text: "Digest", value: "digest" },
    { text: "", value: "delete", sortable: false }
  ];
  private tableLoadingFlag: boolean = false;
  private tableData: Array<any> = [];

  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";

  //For creating/reseting a registry
  private passwordEntryDialog: boolean = false;
  private registryPassword: String = "";

  //For deleting images dialog
  private selectedImage: DockerUpload = null as any;
  private deleteDockerDialog: boolean = false;
  private deleteDockerSnackbar: boolean = false;

  //For push commands dialog
  private pushCommandsDialog: boolean = false;

  private created(): void {
    this.getRegistry();
  }

  private getRegistry(): void {
    this.$store
      .dispatch("docker/getDockerRegistry")
      .then(() => {
        this.registry = this.$store.getters["docker/registry"];
        this.tableData = this.registry.dockerimagelist != null ? this.registry.dockerimagelist : [];
      })
      .catch();
  }

  private refreshRegistry(): void {
    this.$store
      .dispatch("docker/refreshRegistry")
      .then(() => {
        this.getRegistry();
      })
      .catch(errorStatus => {
        if (errorStatus == 403) {
          this.$store.dispatch(
            "showErrorAppSnackbarMessage",
            "You do not have permission perform this action. Contact your project lead or a KAIROS admin for support if you believe this is an error."
          );
        } else {
          this.$store.dispatch(
            "showErrorAppSnackbarMessage",
            "Unable to refresh registry. Please try again later"
          );
        }
      });
  }

  private openPasswordEntryDialog(): void {
    this.passwordEntryDialog = true;
  }
  private closePasswordEntryDialog(): void {
    this.passwordEntryDialog = false;
    this.registryPassword = "";
  }

  private submitPassword(): void {
    if (this.registryPassword.length < 6) {
      this.$store.dispatch(
        "showErrorAppSnackbarMessage",
        "Password must be atleast 6 characters long"
      );
    } else {
      if (this.registry) {
        this.resetRegistryPassword();
      } else {
        this.createDockerRegistry();
      }
      this.closePasswordEntryDialog();
    }
  }
  private createDockerRegistry(): void {
    this.$store
      .dispatch("docker/createRegistry", this.registryPassword)
      .then(() => {
        this.getRegistry();
      })
      .catch(errorStatus => {
        if (errorStatus == 403) {
          this.$store.dispatch(
            "showErrorAppSnackbarMessage",
            "You do not have permission to create a registry. Contact your project lead or a KAIROS admin for support if you believe this is an error."
          );
        } else {
          this.$store.dispatch(
            "showErrorAppSnackbarMessage",
            "Unable to create a new registry. Please try again later"
          );
        }
      });
  }
  private resetRegistryPassword(): void {
    this.$store
      .dispatch("docker/resetRegistryPassword", this.registryPassword)
      .then(() => {
        this.getRegistry();
      })
      .catch(errorStatus => {
        if (errorStatus == 403) {
          this.$store.dispatch(
            "showErrorAppSnackbarMessage",
            "You do not have permission to change the registry's password. Contact your project lead or a KAIROS admin for support if you believe this is an error."
          );
        } else {
          this.$store.dispatch(
            "showErrorAppSnackbarMessage",
            "Unable to change the password. Please try again later"
          );
        }
      });
  }

  private openPushCommandsDialog(): void {
    this.pushCommandsDialog = true;
  }
  private closePushCommandsDialog(): void {
    this.pushCommandsDialog = false;
  }
  private getLoginCommand(): String {
    return this.registry
      ? "docker login " + this.registry.endpoint + " -u " + this.registry.owner
      : "";
  }
  private getLogoutCommand(): String {
    return this.registry ? "docker logout " + this.registry.endpoint : "";
  }
  private getTagCommand(): String {
    return this.registry
      ? "docker tag <image name>:<image version> " +
          this.registry.endpoint +
          "<image name>:<image version>"
      : "";
  }

  private getPushCommand(): String {
    return this.registry
      ? "docker push " + this.registry.endpoint + "/<image name>:<image version>"
      : "";
  }

  private selectImage(image: DockerUpload): void {
    this.selectedImage = image;
  }

  private deleteImage(): void {
    let destroyDockerUploadsRequest: DestroyDockerUploadsRequest = {
      id: this.selectedImage.id as string
    };
    this.$store
      .dispatch("docker/deleteImage", destroyDockerUploadsRequest)
      .then(() => {
        this.selectedImage = null as any;
        this.deleteDockerDialog = false;
        this.deleteDockerSnackbar = true;
        this.refreshRegistry();
      })
      .catch();
  }

  private toClipboard(value: string) {
    copyToClipboard(value);
  }

  private copyDigest(image: DockerUpload) {
    copyToClipboard(image.digest as string);
  }

  private getTime(value: string) {
    return standardToUserTimezone(value, this.$store.getters["user/currentUser"].timezone);
  }
}
</script>

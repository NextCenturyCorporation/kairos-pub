<template src="./system-settings.html"></template>

<script lang="ts">
import Mixins from "vue-class-component";
import { Component } from "vue-property-decorator";
import { deepClone, getHttpPostErrorNotice } from "../../utils/otherFunctions";
import { ZeusSetting } from "zeus-api";
import BaseComponent from "../../views/BaseComponent.vue";

@Component
export default class SystemSettings extends Mixins(BaseComponent) {
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";
  private headers: Array<any> = [
    { text: "Name", align: "left", value: "name" },
    { text: "Value", value: "value" },
    { text: "", align: "right", value: "edit", sortable: false },
    { text: "", value: "delete", sortable: false }
  ];
  private settings: Array<ZeusSetting> = this.getSettings();
  private sortBy: Array<String> = ["name"];
  private search: String = "";
  private selectedSettingToModify: ZeusSetting = { name: "", value: "" };
  private addSettingDialog: boolean = false;
  private newSettingName: string = "";
  private newSettingValue: string = "";
  private editDialog: boolean = false;
  private deletionDialog: boolean = false;

  private getSettings(): Array<any> {
    this.$store
      .dispatch("admin/retrieveSettings")
      .then(() => {
        this.settings = this.$store.getters["admin/settings"];
      })
      .catch(errorStatus => {
        let snackBarErrorMessage = getHttpPostErrorNotice(errorStatus, this.$router);
        this.$store.dispatch("showErrorAppSnackbarMessage", snackBarErrorMessage);
      })
      .then(() => {
        this.$forceUpdate();
      });
    return deepClone(this.$store.getters["admin/settings"]);
  }
  private openDialog(setting: ZeusSetting): void {
    this.resetDialogs();
    this.selectedSettingToModify = setting;
    this.newSettingName = setting.name;
    this.newSettingValue = setting.value;
  }
  private resetDialogs(): void {
    this.newSettingName = "";
    this.newSettingValue = "";
  }
  private confirmSettingAdd(): void {
    this.addSettingDialog = false;
    let setting: ZeusSetting = {
      name: this.newSettingName,
      value: this.newSettingValue
    };
    this.$store.dispatch("admin/postSetting", setting).then(() => {
      this.settings = this.getSettings();
    });
    this.resetDialogs();
  }
  private confirmSettingEdit(): void {
    this.editDialog = false;
    this.selectedSettingToModify.name = this.newSettingName;
    this.selectedSettingToModify.value = this.newSettingValue;
    this.$store.dispatch("admin/postSetting", this.selectedSettingToModify).then(() => {
      this.settings = this.getSettings();
    });
    this.resetDialogs();
  }
  private confirmSettingDeletion(): void {
    this.deletionDialog = false;
    this.$store.dispatch("admin/deleteSetting", this.selectedSettingToModify.id).then(() => {
      this.resetDialogs();
      this.settings = this.getSettings();
    });
    this.resetDialogs();
  }
}
</script>

<template src="./modify-feature-flags.html"></template>

<script lang="ts">
import mixins from "vue-class-component";
import { Component, Vue } from "vue-property-decorator";
import { deepClone } from "../../utils/otherFunctions";
import {
  userTimeToStandard,
  standardToUserTimezone,
  dateInUserTimeZone,
  timeInUserTimeZone
} from "../../utils/date";
import { FeatureFlag, FeatureFlagOverride } from "zeus-ui-api";

const props = Vue.extend({
  props: {
    tableData: Array
  }
});

@Component
export default class ModifyFeatureFlags extends mixins(props) {
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";
  private featureFlags: Array<FeatureFlag> = deepClone(this.$store.getters["admin/featureFlags"]);

  /*********  Table Settings *********/
  private defaultHeaders: Array<any> = [
    { text: "Name", align: "left", value: "name" },
    { text: "Description", align: "left", value: "" },
    { text: "Default", align: "left", value: "enabled" },
    { text: "", align: "right", value: "createNew" },
    { text: "", align: "right", value: "delete" }
  ];
  private headers: Array<any> = [];
  private tabidx: number = 0;
  private tableLoadingFlag: boolean = false;
  private defaultDisplayValue: String = "Nothing to display";
  private sortBy: Array<String> = ["name"];
  private search: String = "";

  // Form fields
  private validOverrideForm: boolean = true;
  private newOverrideName: String = "";
  private localStartDate: string = "";
  private localEndDate: string = "";
  private newOverrideStatus: boolean = false;
  private localStartTime: string = "";
  private localEndTime: string = "";
  private newFeatureFlagName: string = "";
  private newFeatureFlagStatus: boolean = false;
  private selectedFeatureFlagToEdit: any = null as any;
  private selectedOverrideToEdit: any = null as any;

  // Edit handlers
  private editName: boolean = false;
  private deleteOverride: boolean = false;
  private editOverride: boolean = false;
  private createOverride: boolean = false;
  private creatingNewFeatureFlag: boolean = false;
  private deletingFeatureFlag: boolean = false;

  getFeatureFlags(): any {
    this.$store
      .dispatch("admin/retrieveAllFeatureFlags")
      .then(() => {
        this.featureFlags = deepClone(this.$store.getters["admin/featureFlags"]);
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private getHeaders(): Array<any> {
    return this.defaultHeaders;
  }

  private created(): void {
    this.headers = this.getHeaders();
    this.getFeatureFlags();
  }

  private openCreateFeatureFlag(): void {
    this.newFeatureFlagName = "";
    this.creatingNewFeatureFlag = true;
  }

  private saveFeatureFlag(): void {
    var newFF: FeatureFlag = {
      id: "",
      name: this.newFeatureFlagName,
      enabled: this.newFeatureFlagStatus,
      overrides: []
    };

    this.$store
      .dispatch("admin/createFeatureFlag", newFF)
      .then(userMessage => {
        this.$store.dispatch("showAppSnackbarMessage", "Feature flag created");
        this.newFeatureFlagName = "";
        this.newFeatureFlagStatus = false;
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      })
      .then(() => {
        this.getFeatureFlags();
        this.creatingNewFeatureFlag = false;
      });
  }

  private openFeatureFlagDelete(flag: FeatureFlag) {
    this.deletingFeatureFlag = true;
    this.newFeatureFlagName = flag.name;
    this.selectedFeatureFlagToEdit = flag;
  }

  private confirmFeatureFlagDelete(): void {
    this.$store
      .dispatch("admin/deleteFeatureFlag", this.selectedFeatureFlagToEdit)
      .then(userMessage => {
        this.$store.dispatch("showAppSnackbarMessage", "Feature flag deleted");
        this.newFeatureFlagName = "";
        this.selectedFeatureFlagToEdit = null;
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      })
      .then(() => {
        this.deletingFeatureFlag = false;
        this.getFeatureFlags();
      });
  }

  private openOverrideDelete(flag: FeatureFlag, override: FeatureFlagOverride): void {
    this.deleteOverride = true;
    this.selectedFeatureFlagToEdit = flag;
    this.selectedOverrideToEdit = override;
  }

  private deleteOverrideConfirmed(): void {
    this.deleteOverride = false;
    for (let i = 0; i < this.selectedFeatureFlagToEdit.overrides.length; i++) {
      if (this.selectedFeatureFlagToEdit.overrides[i].id === this.selectedOverrideToEdit.id) {
        this.selectedFeatureFlagToEdit.overrides.splice(i, 1);
        this.saveOverride();
        break;
      }
    }
  }

  private openEditOverride(flag: FeatureFlag, override: FeatureFlagOverride): void {
    this.createOverride = false;
    this.selectedFeatureFlagToEdit = flag;
    this.selectedOverrideToEdit = override;
    this.newOverrideName = override.name;
    this.localStartDate = override.startDate
      ? dateInUserTimeZone(override.startDate, this.$store.getters["user/currentUser"].timezone)
      : "";
    this.localEndDate = override.endDate
      ? dateInUserTimeZone(override.endDate, this.$store.getters["user/currentUser"].timezone)
      : "";

    this.localStartTime = override.startDate
      ? timeInUserTimeZone(override.startDate, this.$store.getters["user/currentUser"].timezone)
      : "";
    this.localEndTime = override.endDate
      ? timeInUserTimeZone(override.endDate, this.$store.getters["user/currentUser"].timezone)
      : "";

    this.newOverrideStatus = override.override;
    this.editOverride = true;
  }

  private openCreateNewOverride(flag: FeatureFlag) {
    this.selectedFeatureFlagToEdit = flag;
    this.createOverride = true;
    this.newOverrideName = "";
    this.localStartDate = "";
    this.localEndDate = "";
    this.localStartTime = "";
    this.localEndTime = "";
    this.newOverrideStatus = false;
    this.editOverride = true;
  }

  fillOverrideForm(): void {
    if (this.createOverride) {
      var emptyOverride: FeatureFlagOverride = {
        name: "",
        id: "",
        startDate: "",
        endDate: "",
        override: false
      };

      this.selectedOverrideToEdit = emptyOverride;
      this.selectedOverrideToEdit.name = this.newOverrideName;
      this.selectedOverrideToEdit.startDate = userTimeToStandard(
        this.localStartDate + " " + this.localStartTime,
        this.$store.getters["user/currentUser"].timezone
      );
      this.selectedOverrideToEdit.endDate = userTimeToStandard(
        this.localEndDate + " " + this.localEndTime,
        this.$store.getters["user/currentUser"].timezone
      );
      this.selectedOverrideToEdit.override = this.newOverrideStatus;
      this.selectedOverrideToEdit.id = "";

      this.selectedFeatureFlagToEdit.overrides.push(this.selectedOverrideToEdit);
    } else {
      for (let currentOverride of this.selectedFeatureFlagToEdit.overrides) {
        if (currentOverride.id === this.selectedOverrideToEdit.id) {
          currentOverride.name = this.newOverrideName;
          currentOverride.startDate = userTimeToStandard(
            this.localStartDate + " " + this.localStartTime,
            this.$store.getters["user/currentUser"].timezone
          );
          currentOverride.endDate = userTimeToStandard(
            this.localEndDate + " " + this.localEndTime,
            this.$store.getters["user/currentUser"].timezone
          );
          currentOverride.override = this.newOverrideStatus;
          break;
        }
      }
    }
  }

  private saveOverride(): void {
    this.editOverride = false;
    this.$store
      .dispatch("admin/updateFeatureFlag", this.selectedFeatureFlagToEdit)
      .then(userMessage => {
        this.$store.dispatch("showAppSnackbarMessage", "Override Saved");
        this.selectedFeatureFlagToEdit = null;
        this.getFeatureFlags();
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  saveAndUpdateDisplay(): void {
    this.fillOverrideForm();
    this.saveOverride();
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }

  private standardToUserTime(standardTime: string): string {
    return standardToUserTimezone(standardTime, this.$store.getters["user/currentUser"].timezone);
  }
}
</script>

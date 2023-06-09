<style scoped>
* {
  text-transform: none !important;
  font-weight: 550 !important;
}
.left-border {
  border-left-style: solid;
  border-left-color: var(--v-anchor-base) !important;
  border-left-width: 10px;
}
.under-trick {
  margin-bottom: -40px !important;
  min-height: 80px;
  padding-bottom: 40px !important;
}
.static-filter-field {
  width: 12% !important;
}
</style>

<template src="./html/services.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import { rules } from "../utils/data-validation";
import { toStandardViewDate } from "../utils/date";
import { deepClone, getStatusColor, getHttpPostErrorNotice } from "../utils/otherFunctions";
import { Service, ClothoServiceDto, ClothoServiceDtoDatabaseTypeEnum } from "zeus-api";
import NProgress from "nprogress";
import BaseComponent from "../views/BaseComponent.vue";
import { dateInUserTimeZone } from "../utils/date";

@Component
export default class Services extends Mixins(BaseComponent) {
  private validDetailsForms: any = {};
  private search: string = "";
  private tableLoadingFlag: boolean = false;
  private requestServiceDialog: boolean = false;
  private validRequestServiceForm: boolean = true;
  private deleteServiceDialog: boolean = false;
  private deleteServiceId: number = 0;
  //just set this to the model
  private tableData: Array<any> = [];
  private serviceName: string = "";
  private inputType: string = "";
  private inputSubType: any = ClothoServiceDtoDatabaseTypeEnum;
  private inputDetails: string = "";
  // Rulse are type any b/c it can either be a boolean or string
  private serviceNameRules: Array<Function> = rules.serviceName;
  private typeRules: Array<Function> = rules.type;
  private detailsRules: Array<Function> = rules.details;
  private notesRules: Array<Function> = rules.notes;
  private unsubscribeFunction: any;
  private nameFilter: string = "";
  private teamFilter: string = "";
  private statusFilter: string = "";
  private typeFilter: string = "";
  private accessFilter: string = "";
  private accesses: Array<string> = ["Private", "Public"];
  private getStatusColor = getStatusColor;
  private dateModal: boolean = false;
  private dateRange: Array<string> = [];
  public onScreenDetails: any = { access: {}, notes: {} };
  private detailsValuesChanged: any = {};

  get alternateText(): string {
    return this.$vuetify.theme.dark ? " primary--text text--darken-1" : "primary--text";
  }

  get dateFilter(): string {
    return this.dateRange.join(" ~ ");
  }
  get headers(): Array<any> {
    return [
      { text: "", value: "data-table-expand" },
      {
        text: "Name",
        align: "left",
        value: "name",
        filter: (value: string) => {
          if (!this.nameFilter) return true;
          return value && value.includes(this.nameFilter);
        }
      },
      {
        text: "Team",
        value: "teamName",
        filter: (value: string) => {
          if (!this.teamFilter) return true;
          return value && value.includes(this.teamFilter);
        }
      },
      {
        text: "Status",
        value: "status",
        filter: (value: string) => {
          if (!this.statusFilter) return true;
          return value && value.includes(this.statusFilter);
        }
      },
      {
        text: "Date Received",
        value: "dateReceived",
        filter: (value: string) => {
          if (!this.dateFilter) return true;
          if (!value) return false;
          let dateRangeStrings = this.dateFilter.split(" ~ ");
          //Vue input box should prevent more than 2 dates selected
          if (dateRangeStrings.length < 2) return true;
          let valueDate = new Date(value);
          let dateRangeDates: Array<Date> = [];
          dateRangeStrings.forEach(dateString => {
            dateRangeDates.push(new Date(dateString));
          });
          let sortedDateRangeDates = dateRangeDates.sort((a, b) => a.getTime() - b.getTime());
          if (
            valueDate.getTime() < sortedDateRangeDates[0].getTime() ||
            valueDate.getTime() > sortedDateRangeDates[1].getTime()
          )
            return false;
          return true;
        }
      },
      {
        text: "Type",
        value: "type",
        filter: (value: string) => {
          if (!this.typeFilter) return true;
          return value && value.includes(this.typeFilter);
        }
      },
      {
        text: "Access",
        value: "access",
        filter: (value: string) => {
          if (!this.accessFilter) return true;
          return value && value.includes(this.accessFilter);
        }
      },
      { text: "", value: "action", sortable: false }
    ];
  }

  created() {
    this.tableData = this.getServices();
  }

  private toggleFilterPanel(): void {
    let filterBtnClasses: any = (this.$refs["filterButton"] as any).$el.classList;
    let filterPanelClasses: any = (this.$refs["filterCard"] as any).$el.classList;
    let replacementDiv: any = (this.$refs["filterReplacementDiv"] as any).$el.classList;
    filterBtnClasses.contains("under-trick")
      ? filterBtnClasses.remove("under-trick")
      : filterBtnClasses.add("under-trick");
    filterPanelClasses.contains("d-none")
      ? filterPanelClasses.remove("d-none")
      : filterPanelClasses.add("d-none");
    replacementDiv.contains("d-none")
      ? replacementDiv.remove("d-none")
      : replacementDiv.add("d-none");
  }

  private getServices(): Array<any> {
    this.tableLoadingFlag = true;
    let services = this.formatServices(deepClone(this.$store.getters.services));
    this.tableLoadingFlag = false;
    return services;
  }

  private formatServices(services: Array<any>): Array<any> {
    services.forEach((service: any) => {
      this.detailsValuesChanged[service.id] = false;
      this.onScreenDetails.access[service.id] = service.access || "unknown";
      this.onScreenDetails.notes[service.id] = service.details || "";
      if (service.dateReceived) {
        service.dateReceived = toStandardViewDate(new Date(service.dateReceived));
      }
      service.endpoints &&
        service.endpoints.forEach(function(endpoint: any) {
          let url: URL = new URL(
            (endpoint.uri.indexOf("http") > -1 ? "" : "http://") + endpoint.uri
          );
          endpoint.host = url.hostname;
          endpoint.port = url.port;
        });
    });
    return services;
  }

  private refreshServices(): void {
    NProgress.set(0.5);
    this.$store
      .dispatch("updateServicesFromBackend")
      .then(() => {
        this.search = "";
        this.tableData = this.getServices();
      })
      .catch(error => {
        this.$store.dispatch("showErrorAppSnackbarMessage", "Failed to refresh Services");
      })
      .then(() => {
        NProgress.done();
      });
  }

  private resetForm(): void {
    if (this.$refs.requestServiceForm) {
      (this.$refs.requestServiceForm as any).reset();
    }
  }

  private evaluateDetailsChange(item: any): void {
    let newAccessValue = this.onScreenDetails.access[item.id];
    let newDetailsValue = this.onScreenDetails.notes[item.id];
    if (item.access !== newAccessValue || item.details !== newDetailsValue) {
      this.detailsValuesChanged[item.id] = true;
    } else {
      this.detailsValuesChanged[item.id] = false;
    }
    this.detailsValuesChanged = deepClone(this.detailsValuesChanged);
  }

  private saveDetails(item: any): void {
    let newAccessValue = this.onScreenDetails.access[item.id];
    item.access = newAccessValue;
    let newDetailsValue = this.onScreenDetails.notes[item.id];
    item.details = newDetailsValue;
    this.evaluateDetailsChange(item);
    let newService: Service = {
      id: item.id,
      name: item.name,
      access: newAccessValue,
      details: newDetailsValue
    };

    this.$store
      .dispatch("persistServiceUpdate", newService)
      .catch(errStatus => {
        let snackBarErrorMessage = getHttpPostErrorNotice(errStatus, this.$router);
        this.$store.dispatch("showErrorAppSnackbarMessage", "Service update failed");
      })
      .then(() => {
        this.detailsValuesChanged[item.id] = false;
        this.$store.dispatch("showAppSnackbarMessage", "Service details updated!");
      });
  }

  private resetDetails(item: any): void {
    (this.$refs["currentAccess-" + item.id] as any).lazyValue = item.access;
    let newDetailsValue = ((this.$refs["currentNote-" + item.id] as any).lazyValue = item.details);
    this.detailsValuesChanged[item.id] = false;
  }

  private closeForm(): void {
    this.requestServiceDialog = false;
    this.resetForm();
  }

  private submitForm(): void {
    this.requestServiceDialog = false;
    NProgress.start();
    let newService: ClothoServiceDto = {
      name: this.serviceName,
      databaseType: this.inputSubType as ClothoServiceDtoDatabaseTypeEnum,
      details: this.inputDetails
    };
    this.$store
      .dispatch("persistNewService", newService)
      .then(() => {
        this.updateAfterServiceSaved();
      })
      .catch((errStatus: number) => {
        this.handleSaveNewServiceError(errStatus);
      });
  }

  private updateAfterServiceSaved(): void {
    this.resetForm();
    this.$store.dispatch("showAppSnackbarMessage", "New Service Requested!");
    this.refreshServices();
  }

  private handleSaveNewServiceError(errorStatusCode: number): void {
    let snackBarErrorMessage: string;
    if (errorStatusCode === 409) {
      snackBarErrorMessage = "This Service name is already taken. Please use a different name.";
    } else {
      snackBarErrorMessage = getHttpPostErrorNotice(errorStatusCode, this.$router);
    }
    this.$store.dispatch("showErrorAppSnackbarMessage", snackBarErrorMessage);
    NProgress.done();
  }

  private deleteServiceRequest(service: any): void {
    this.deleteServiceId = service.id;
  }

  private visualizationRoute(item: any): void {
    let router = this.$router;
    const graphUrl = item.host + ":" + item.port;
    router.push({
      name: "Visualization",
      query: { graphUrl: graphUrl },
      params: { graphUrl: graphUrl, host: item.host, port: item.port }
    });
  }

  private deleteServiceConfirmed(): void {
    this.deleteServiceDialog = false;
    let service = {};
    this.$store.getters.services.forEach((storeService: any) => {
      if ((storeService as any).id === this.deleteServiceId) service = storeService;
    });
    this.$store
      .dispatch("persistServiceDeletion", service)
      .then(() => {
        this.refreshServices();
        this.$store.dispatch("showAppSnackbarMessage", "Service Deleted!");
      })
      .catch(errStatus => {
        let snackBarErrorMessage = getHttpPostErrorNotice(errStatus, this.$router);
        this.$store.dispatch("showErrorAppSnackbarMessage", "Service deletion failed");
        this.refreshServices();
      });
  }

  private dateInTimeZone(date: string): string {
    return dateInUserTimeZone(date, this.$store.getters["user/currentUser"].timezone);
  }
}
</script>

<template src="./create-k8s-experiment.html"> </template>

<script lang="ts">
import { Component, Watch, Mixins, Prop } from "vue-property-decorator";
import { deepClone } from "../../utils/otherFunctions";
import { rules } from "../../utils/data-validation";
import BaseComponent from "../../views/BaseComponent.vue";
import { ExperimentConfiguration } from "../../interfaces/experiment-configuration";
import { ExperimentPerformer } from "../../interfaces/experiment-performer";
import { standardToUserTimezone } from "../../utils/date";
import { Evaluation, Experiment, ExperimentRun } from "evaluation-api";
import { StoredFile } from "zeus-files-api";
import evaluation from "@/store/modules/evaluation";

@Component
export default class k8sExperiment extends Mixins(BaseComponent) {
  @Prop({ required: true }) evaluation!: Evaluation;
  @Prop({ required: true }) evaluationFiles!: StoredFile[];
  private taskToLocation: Map<string, string> = new Map();
  private experiments: any = this.evaluation.experiments;
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";
  private experimentDialog: boolean = false;
  private overviewDialog: boolean = false;
  private showPerformerOptions: boolean = false;
  private edittingPerformer: boolean = false;
  private previewDisplay: any = {};
  private window: Number = 0;
  private checked: string = "Manual";
  private offset: boolean = true;
  private previewExperiment: Experiment = { id: "", name: "", value: "{}" };

  private bannerTimer: any;

  // Form fields
  private step: any = 0;

  private performerObj: ExperimentPerformer = {
    performername: "",
    schemalibraries: [],
    uri: "docker-compose.yml",
    service_port: "10100"
  };

  private experimentConfigurations: ExperimentConfiguration = {
    type: "",
    targetDataSet: this.evaluation.name,
    manifests: [],
    tasklocation: ""
  };

  private userExperiment: object = null as any;
  
  private validExperimentForm: boolean = true;
  private currentDate: Date = new Date();
  private formTime: string = "";
  private date: Date = new Date();
  private time: any = null as any;

  // // Form Experiment Values
  private formExperimentName: string = "";
  private formPerformers: Array<any> = [];
  private existingExperiment: Object = null as any;
  private programName: string = "KAIROS";
  private cpuDesired: number = 1;
  private cpuType: String = "m5.large";
  private gpuDesired: number = 0;
  private gpuType: String = "p3.2xlarge";
  // Alert toast
  private isEnvAlive: String = this.getK8sStatus();
  private cpuNodeTypes: Array<String> = [];
  private gpuNodeTypes: Array<String> = [];

  private nodeDesired: Array<number> = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]; // Hard coding because doesn't make much sense for a DB entry
  // Rules
  private experimentTypeRule: Array<Function> = rules.experimentType;
  private experimentTargetDataSetRule: Array<Function> = rules.experimentTargetDataSet;
  private experimentEvaluatorRule: Array<Function> = rules.experimentEvaluator;

  /*********  Table Settings *********/
  private headers: Array<any> = [
    { text: "Name", align: "left", value: "name" },
    { text: "Run ID", align: "left", value: "runId" },
    { text: "Status", align: "left", value: "status" },
    { text: "Date Created", align: "left", value: "commissionDate" }
  ];
  private experimentRunHeader: Array<any> = [
    { text: "Run ID", align: "left", value: "name" },
    { text: "Start Run Date", align: "left", value: "startTime" },
    { text: "Complete Date", align: "left", value: "completeTime" },
    { text: "Status", align: "left", value: "status" },
    { text: "Message", align: "left", value: "message" },
    { text: "", align: "left", value: "menu" },
  ];

  private tabidx: number = 0;
  private tableLoadingFlag: boolean = false;
  private defaultDisplayValue: String = "Nothing to display";
  private sortBy: Array<String> = ["name"];
  private search: String = "";
  private performerHeaders: Array<any> = [
    { text: "Added Performers", align: "left", value: "performername" }
  ];

  private created(): void {
    this.getNodeTypes();
    this.k8sSystemsCheck();
    this.headers = this.getHeaders();
    this.performerHeaders = this.getPerformerHeaders();
  }

  private getPerformerHeaders(): Array<any> {
    let h: Array<any> = this.performerHeaders;
    h.push({ text: "", value: "edit" });
    h.push({ text: "", value: "delete" });
    return this.performerHeaders;
  }

  private getHeaders(): Array<any> {
    let h: Array<any> = this.headers;
    h.push({ text: "", value: "edit" });
    return this.headers;
  }

  private openNewExpModal(): void {
    this.experimentDialog = true;
    this.step = 0;
  }

  private closeModal(): void {
    this.resetData();
    this.existingExperiment = this.experimentConfigurations;
    this.experimentDialog = false;
  }

  private getNodeTypes(): void {
    this.$store
      .dispatch("admin/retrieveNodeTypes")
      .then(userMessage => {
        var response = this.$store.getters["admin/nodeTypes"];
        this.cpuNodeTypes = response.cpuTypes.sort();
        this.gpuNodeTypes = response.gpuTypes.sort();
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    return deepClone(this.$store.getters["admin/nodeTypes"]);
  }

  private saveExperiment(): void {
    this.experimentConfigurations.manifests = this.formPerformers;
    var experimentJson = {
      id: "",
      name: this.formExperimentName,
      value: JSON.stringify(this.buildJsonConfiguration()),
      programName: this.programName,
      status: "Pending",
      runId: "",
      comissionDate: this.currentDate.toISOString(),
      experimentRuns: []
    };
    this.evaluation.experiments!.push(experimentJson);
    this.experiments = this.evaluation.experiments;
    this.$emit("update-evaluation", this.evaluation);
    this.closeModal();
  }

  private getExperimentType(input: string): string {
    console.log(input);
    if(input.toLowerCase().indexOf("task1") >=0 ) {
      return "task1";
    } else if(input.toLowerCase().indexOf("task2") >=0 ) {
      return "task2";
    } else {
      return "unknown";
    }
  }

  private buildJsonConfiguration(): any {
    let performerJsonBuilder = [];
    for (let performer of this.formPerformers) {
      let tempString = JSON.stringify(performer).toLowerCase();
      let JSONLowerCase = JSON.parse(tempString);
      performerJsonBuilder.push(JSONLowerCase);
    }
    return {
      experiment: {
        name: this.formExperimentName,
        type: this.getExperimentType(this.experimentConfigurations.type),
        tasklocation: this.taskToLocation.get(this.experimentConfigurations.type),
        evaluationdataset: this.experimentConfigurations.targetDataSet
      },
      cluster: {
        cpu: {
          desired: this.cpuDesired,
          type: this.cpuType
        },
        gpu: {
          desired: this.gpuDesired,
          type: this.gpuType
        }
      },
      manifests: performerJsonBuilder
    };
  }

  private resetData(): void {
    this.checked = "Manual";
    this.formTime = "";
    this.experimentConfigurations = {
      type: "",
      targetDataSet: this.evaluation.name,
      manifests: [],
      tasklocation: ""
    };
    this.performerObj = {
      performername: "",
      schemalibraries: [],
      uri: "docker-compose.yml",
      service_port: "10100"
    };
    this.formPerformers = [];
  }

  // Form Functions
  private nextTab(): void {
    this.step++;
  }

  private backTab(): void {
    this.step--;
  }

  private savePerformer(): void {
    // Find and update
    if (this.edittingPerformer) {
      this.deletePerformer(this.performerObj);
    }
    this.formPerformers.push(this.performerObj);
    this.resetPerformerForm();
    this.showPerformerOptions = false;
    this.edittingPerformer = false;
  }

  private editPerformer(item: any): void {
    this.edittingPerformer = true;
    this.performerObj = item;
    this.showPerformerOptions = true;
  }

  private deletePerformer(item: any): void {
    for (let i = 0; i < this.formPerformers.length; i++) {
      if (this.formPerformers[i].performername === item.performername) {
        this.formPerformers.splice(i, 1);
        break;
      }
    }
    this.showPerformerOptions = false;
  }

  private cancelPerformerAddEdit(): void {
    this.resetPerformerForm();
    this.edittingPerformer = false;
    this.showPerformerOptions = false;
  }

  private resetPerformerForm(): void {
    this.performerObj = {
      performername: "",
      schemalibraries: [],
      uri: "docker-compose.yml",
      service_port: "10100"
    };
  }

  private createPreview(): void {
    // Clear out any half finished performer selection
    this.cancelPerformerAddEdit();
    this.nextTab();
    // var performerString = this.formPerformers.map(performer => performer.performername).join("-")
    this.formExperimentName =
      this.experimentConfigurations.targetDataSet.replace(" ", "-") +
      "-" +
      this.experimentConfigurations.type.replace(" ", "-")
      // "-" +
      // performerString
    this.previewDisplay = this.buildJsonConfiguration();
  }

  private performerDisableSave(): boolean {
    return this.performerObj.performername === "" || this.performerObj.schemalibraries.length === 0
      ? true
      : false;
  }

  private isDisabled(): boolean {
    // Base Experiment form
    if (this.step === 0) {
      return this.experimentConfigurations.type === "";
    } else if (this.step === 1) {
      // Performer selection form
      return this.formPerformers.length === 0;
    } else if (this.step === 2) {
      //wtag check for filled forms
      return this.cpuDesired + this.gpuDesired < 1;
    }
    return false;
  }

  private downloadJson(stage: string): void {
    let jsonStr; 
    if(stage === "pre") {
      jsonStr = JSON.stringify(this.buildJsonConfiguration());
    } else if (stage === "post") {
      jsonStr = this.previewExperiment.value;
    } else {
      throw new Error("invalid state, stage is neither pre nor post");
    }

    let expName: string = JSON.parse(jsonStr).experiment.name;
    let dataStr: string = "data:text/json;charset=utf-8,"+encodeURIComponent(jsonStr);

    var dlAnchorElem = document.getElementById("downloadJsonFile" + stage);
    var fileName = expName + ".json";
    if (dlAnchorElem) {
      dlAnchorElem.setAttribute("href", dataStr);
      dlAnchorElem.setAttribute("download", fileName);
    }
  }

  private openOverview(experiment: Experiment): void {
    this.overviewDialog = true;
    this.previewExperiment = experiment;
  }

  private getOverview(): JSON {
    return JSON.parse(this.previewExperiment.value);
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }

  @Watch("evaluation")
  onEvalChange(): void {
    this.experiments = this.evaluation.experiments;
    this.experimentConfigurations.targetDataSet = this.evaluation.name;
  }

  @Watch("existingExperiment")
  onToggleUserActivity(value: Experiment): void {
    if (value && value.value) {
      var jsonValue = JSON.parse(value.value);
      this.formTime = "";
      this.experimentConfigurations = {
        type: jsonValue.experiment.type,
        targetDataSet: jsonValue.experiment.evaluationdataset,
        manifests: jsonValue.manifests,
        tasklocation: jsonValue.experiment.tasklocation
      };
      this.formPerformers = jsonValue.manifests;
    } else {
      this.resetData();
    }
  }

  private k8sSystemsCheck(): void {
    var timeout = 1000 * 60 * 3; // milli to seconds to minutes (3)
    var self = this;
    this.bannerTimer = setInterval(function(): void {
      self.getK8sStatus();
    }, timeout);
  }

  private waitAndResetInterval(): void {
    var waitTime = 10000 * 60 * 5;
    setTimeout(this.k8sSystemsCheck, waitTime);
  }


  private checkExperimentHasActiveRun(exp: Experiment) {
    // if there are no runs there can't be active runs
    if (!exp.experimentRuns) {
      return false;
    }
    // if any runs are missing completeTime they are active
    for (let i = 0; i < exp.experimentRuns.length; i++) {
        if (!exp.experimentRuns[i].completeTime) {
          return true;
        }
    }
    // if no runs are missing completeTime none are active
    return false;
  }

  private experimentRunIsComplete(experimentRun: ExperimentRun): boolean {
    if (experimentRun.completeTime) {
      return true;
    }
    return false;
  }

  private getExperimentRunMessage(experimentRun: ExperimentRun): string {
    if (experimentRun.error && experimentRun.error.length > 0){
      return experimentRun.error;
    }
    return "Attempt: "+experimentRun.attempt;
  }

  private getExperimentRunningStatus(exp: Experiment): String {
    var status = "";
    if (exp.experimentRuns) { 
      for (let i = 0; i < exp.experimentRuns.length; i++) {
        if (!exp.experimentRuns[i].completeTime) {
          status = exp.experimentRuns[i].status || "";
        }
      }
    }
    return status
  }

  /**
   * *************************************************************************************
   * Service calls.
   * *************************************************************************************
   */


  private getAvailableTasks(): string[] {
    let owners: string[] = [];
    for (let file of this.evaluationFiles) {
      if (!owners.includes(file.owner) && file.category === "task") {
        this.taskToLocation.set(file.owner, file.uri.replace("/" + file.filename, ""))
        owners.push(file.owner);
      }
    }
    return owners;
  }

  private getPerformers(): string[] {
    let owners: string[] = [];
    for (let file of this.evaluationFiles) {
      if (!owners.includes(file.owner) && file.category === "docker-compose") {
        owners.push(file.owner);
      }
    }
    return owners;
  }

  private getComposeFiles(): StoredFile[] {
    let composeFiles: StoredFile[] = [];
    for (let file of this.evaluationFiles) {
      if (file.category === "docker-compose" && file.owner === this.performerObj.performername) {
        composeFiles.push(file);
      }
    }
    return composeFiles;
  }

  private getSchemaPerformers(): StoredFile[] {
    let owners: string[] = [];
    let schemas: StoredFile[] = [];
    for (let file of this.evaluationFiles) {
      if (!owners.includes(file.owner) && file.category === "schemas") {
        owners.push(file.owner);
        file.uri = file.uri.replace("/" + file.filename,"");
+       schemas.push(file);
      }
    }
    return schemas;
  }

  private runExperiment(exp: Experiment): any {
    this.$store
      .dispatch("experiment/runExperiment", { experimentId: exp.id })
      .then(() => { })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    this.experiments = this.evaluation.experiments;
  }

  private stopExperiment(run: ExperimentRun): any {
    this.$store
      .dispatch("experiment/stopExperiment", { experimentId: run.id })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    this.experiments = this.evaluation.experiments;
  }
  
  private deleteExperiment(exp: Experiment): any {
    this.evaluation.experiments = this.evaluation.experiments!.filter(experiment => experiment.id != exp.id);
    this.$emit("update-evaluation", this.evaluation);
    this.$store
      .dispatch("experiment/deleteExperiment", { id: exp.id })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    this.experiments = this.evaluation.experiments;
  }

  private getK8sStatus(): String {
    this.$store
      .dispatch("experiment/retrieveK8sStatus")
      .then(() => {
        this.isEnvAlive = this.$store.getters["experiment/k8sStatus"];
      })
      .catch(errorStatus => {
        this.isEnvAlive = "Terminated"; // defaulting
        this.handleErrorStatus(errorStatus);
      });
    return deepClone(this.$store.getters["experiment/k8sStatus"]);
  }

  private createK8sEnv(): any {
    clearInterval(this.bannerTimer);
    this.isEnvAlive = "Pending";
    this.waitAndResetInterval();
    this.$store.dispatch("experiment/createK8sEnv").catch(errorStatus => {
      this.handleErrorStatus(errorStatus);
    });
  }

  private destroyK8sEnv(): any {
    clearInterval(this.bannerTimer);
    this.isEnvAlive = "Pending";
    this.waitAndResetInterval();
    this.$store.dispatch("experiment/destroyK8sEnv").catch(errorStatus => {
      this.handleErrorStatus(errorStatus);
    });
  }

  private standardToUserTime(standardTime: string): string {
    if (standardTime)
      return standardToUserTimezone(standardTime, this.$store.getters["user/currentUser"].timezone);
    return "--";
  }
}
</script>

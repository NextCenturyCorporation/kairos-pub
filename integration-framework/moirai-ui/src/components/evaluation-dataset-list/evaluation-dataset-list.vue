<template src="./evaluation-dataset-list.html"></template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { Evaluation, Dataset } from "evaluation-api";

import { standardToUserTimezone } from "../../utils/date";
import { deepClone } from "../../utils/otherFunctions";

import DatePicker from "vue2-datepicker";
import "vue2-datepicker/index.css";

@Component({
  components: {
    DatePicker
  }
})
export default class EvaluationDatasetList extends Vue {
  @Prop({ required: true }) evaluation!: Evaluation;
  @Prop({ required: true }) readonly timezone!: string;

  private headers: Array<any> = [
    { text: "Name", align: "left", value: "name" },
    { text: "Enabled", value: "enabled" },
    { text: "Allowed Types", value: "allowedTypes" },
    { text: "Version Files", value: "versionFiles" },
    { text: "Upload", value: "upload" },
    { text: "Download", value: "download" },
    { text: "Start Date", value: "startDate" },
    { text: "End Date", value: "endDate" },
    { text: "Delete", value: "delete" }
  ];

  private nameOptions: Array<string> = [
    "schemas", "docker-compose", "custom"
  ]

  private nameSelected: string = "";

  private nameTextField: boolean = false;

  private defaultWindowDataset: Dataset = {
    id: "",
    name: "",
    enabled: true,
    versionFiles: true,
    upload: true,
    download: false,
    allowedTypes: "",
    startDate: "",
    endDate: ""
  };

  private availableUploadFileTypes: Array<String> = [
    ".json,.json-ld,.jsonld",
    ".txt,.pdf,.doc,.docx,.md,.zip,.gzip",
    ".yml,.yaml,.compose",
    "*"
  ];

  private selectedIndex: any = 0;

  // New/Edit uploadWindow
  private uploadWindowDialog: boolean = false;
  private uploadWindowDataset: Dataset | null = null;
  private uploadWindowFileTypes: Array<String> = [];

  // Delete uploadwindow
  private selectedUploadWindowForDeletion: Dataset | null = null;
  private deleteUploadWindowDialog: boolean = false;


  // for text drop down to appear when custom is selected for new dataset name
  @Watch("nameSelected")
  private watchSelectedName(newValue: string): void {
    if (newValue == 'custom') {
      this.nameTextField = true;
      this.uploadWindowDataset!.name = "";
    } else {
      this.uploadWindowDataset!.name = newValue;
      this.nameTextField = false;
    }
  }
  // Create new evaluation
  private newDataset(): void {
    this.cancelWindowUpload();
    this.uploadWindowDialog = true;
  }

  private update(): void {
    this.$emit("update-evaluation", this.evaluation);
  }

  private confirmWindowUpload(): void {
    if (this.evaluation && this.evaluation.datasets && this.uploadWindowDataset) {
      this.uploadWindowDataset.allowedTypes = this.uploadWindowFileTypes.join(",");
      console.log(this.uploadWindowDataset);
      this.evaluation.datasets.push(this.uploadWindowDataset);
      this.update();
    }
    this.cancelWindowUpload();
  }

  private cancelWindowUpload(): void {
    this.uploadWindowDataset = deepClone(this.defaultWindowDataset);
    this.uploadWindowFileTypes = [];
    this.uploadWindowDialog = false;
  }

  private deleteUploadWindow(item: Dataset): void {
    this.selectedUploadWindowForDeletion = item;
    this.deleteUploadWindowDialog = true;
  }

  private confirmDeleteUploadWindow(): void {
    if (this.evaluation && this.evaluation.datasets && this.selectedUploadWindowForDeletion) {
      let index = this.evaluation.datasets.indexOf(this.selectedUploadWindowForDeletion);
      if (index >= 0) {
        this.evaluation.datasets.splice(index, 1);
      }
      //this.updateEvaluation()
    }
    this.selectedUploadWindowForDeletion = null;
    this.deleteUploadWindowDialog = false;
  }

  private cancelDeleteUploadWindow(): void {
    this.selectedUploadWindowForDeletion = null;
    this.deleteUploadWindowDialog = false;
  }

  private dateInTimeZone(date: string): string {
    return standardToUserTimezone(date, this.timezone);
  }
}
</script>

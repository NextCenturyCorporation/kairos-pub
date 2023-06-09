<template src="./html/evaluations.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import BaseComponent from "./BaseComponent.vue";
import { Evaluation, Dataset } from "evaluation-api";
import { Data } from "vis-network/standalone";
import { StoredFile } from "zeus-files-api";
import EvaluationList from "../components/evaluation-list/evaluation-list.vue";
import EvaluationDatasetList from "../components/evaluation-dataset-list/evaluation-dataset-list.vue";
import CreateK8sExperiment from "../components/create-k8s-experiment/create-k8s-experiment.vue";

import { deepClone } from "../utils/otherFunctions";

@Component({
  components: {
    EvaluationList,
    EvaluationDatasetList,
    CreateK8sExperiment
  }
})
export default class Evaluations extends Mixins(BaseComponent) {
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";
  private timezone: string = this.$store.getters["user/currentUser"].timezone;

  private window: number = 0;
  private evaluations: Array<Evaluation> = [];
  private selectedEval: Evaluation | null = null;
  private evaluationFiles: Array<StoredFile> = [];

  private created(): void {
    this.getEvaluations(null);
  }

  private getEvaluations(evaluation: Evaluation | null): void {
    this.$store
      .dispatch("evaluations/retrieveEvaluations")
      .then(() => {
        this.evaluations = this.$store.getters["evaluations/evaluations"];
        if (evaluation && evaluation.name) {
          let match = this.evaluations.filter(curr => curr.name == evaluation.name);
          if (match.length) {
            this.selectedEval = match[0];
          }
        }
        if (this.selectedEval == null) {
          this.selectedEval = this.evaluations[0];
          this.getEvaluationFiles();
        }
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private getEvaluationFiles(): void {
    this.$store
      .dispatch("files/retrieveEvaluationFiles", this.selectedEval!.name)
      .then(() => {
        this.evaluationFiles = this.$store.getters["files/getEvaluationFiles"];
      })
      .catch(err => {
        console.log("error retrieving evaluation files");
      });
  }

  private selectEval(item: Evaluation): void {
    this.selectedEval = item;
    this.getEvaluationFiles();
  }

   private handleErrorStatus(errorStatus: any): void {
    let errorMessage: string;
    switch(errorStatus) {
      case 401:
        errorMessage =  "User not logged in"
        break;
      case 409:
        errorMessage = "Evaluation already exists with id";
        break;
      default:
        errorMessage = "Unexpected error occured; please try again or contact support";
    }
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }

  private createEvaluation(evaluation: Evaluation): void {
    if (evaluation) {
      this.$store
        .dispatch("evaluations/createEvaluation", evaluation)
        .then(() => {
          this.getEvaluations(evaluation);
        })
        .catch(errorStatus => {
          this.handleErrorStatus(errorStatus);
        });
    }
  }

  private updateEvaluation(evaluation: Evaluation): void {
    this.$store
      .dispatch("evaluations/updateEvaluation", evaluation)
      .then(() => {
        this.getEvaluations(evaluation);
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private syncEvaluation(): void {
    this.$store
      .dispatch("files/syncFiles")
      .then(() => {
        this.getEvaluations(this.selectedEval);
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
  }

  private deleteEvaluation(evaluation: Evaluation): void {
    if (evaluation != null) {
      console.log("deleting " + evaluation);
      this.$store
        .dispatch("evaluations/deleteEvaluation", evaluation)
        .then(() => {
          this.getEvaluations(null);
        })
        .catch(errorStatus => {
          this.handleErrorStatus(errorStatus);
        });
    }
  }
}
</script>

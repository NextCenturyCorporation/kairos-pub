<template src="./evaluation-list.html"></template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { Evaluation, Dataset } from "evaluation-api";

@Component
export default class EvaluationList extends Vue {
  @Prop({ required: true, default: [] }) readonly evaluations!: Array<Evaluation>;

  private selectedIndex: any = 0;

  private newEval: Evaluation | null = null;
  private createEvalDialog: boolean = false;

  private selectedEvalForDeletion: Evaluation | null = null;
  private deleteEvalDialog: boolean = false;

  // Create new evaluation
  private getDefaultEvaluation(): Evaluation {
    return {
      id: "",
      name: "",
      creationDate: "",
      datasets: []
    };
  }

  private createEvaluation(): void {
    this.newEval = this.getDefaultEvaluation();
    this.createEvalDialog = true;
  }

  private confirmCreateEvaluation(): void {
    this.$emit("create-evaluation", this.newEval);
    this.cancelCreateEvaluation();
  }

  private cancelCreateEvaluation(): void {
    this.newEval = null;
    this.createEvalDialog = false;
  }

  // Delete existing evaluation
  private deleteEvaluation(evaluation: Evaluation): void {
    this.selectedEvalForDeletion = evaluation;
    this.deleteEvalDialog = true;
  }

  private confirmDeleteEvaluation(): void {
    this.$emit("delete-evaluation", this.selectedEvalForDeletion);
    this.cancelDeleteEvaluation();
  }

  private cancelDeleteEvaluation(): void {
    this.selectedEvalForDeletion = null;
    this.deleteEvalDialog = false;
  }

  private syncEvaluation(): void {
    this.$emit("sync-evaluation");
  }
}
</script>

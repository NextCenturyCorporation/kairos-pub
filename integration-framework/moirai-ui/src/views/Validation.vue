<template src="./html/Validation.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import BaseComponent from "./BaseComponent.vue";

@Component({})
export default class Validation extends Mixins(BaseComponent) {
  private reader = new FileReader();
  private window: Number = 0;
  private files: Array<File> = [];
  private acceptedFileTypes = ".json, .jsonld";
  private jsonInput: String = "";
  private tableData: Array<any> = [];
  private errorsList: Array<String> = [];
  private warningsList: Array<String> = [];

  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";

  private headers: Array<any> = [
    { text: "Type", align: "left", value: "type" },
    { text: "Text", value: "value" }
  ];

  private hasFile(): boolean {
    return this.files != null && this.files.length > 0;
  }

  private hasJsonInput(): boolean {
    return this.jsonInput != null && this.jsonInput.length > 0;
  }

  private onFileUpload(): void {
    if (this.hasFile()) {
      var file = this.files[0];
      this.reader.onload = res => {
        if (this.reader.result) {
          this.jsonInput = this.reader.result.toString();
        }
      };
      this.reader.onerror = err => console.log(err);
      this.reader.readAsText(file);
    }
  }

  private validate(): void {
    this.tableData = [];
    this.$store
      .dispatch("files/validateInput", this.jsonInput)
      .then(response => {
        console.log(response);
        this.setOutputData(this.$store.getters["files/getValidationOutput"]);
      })
      .catch(({ response, status }) => {
        this.setErrorData(response, status);
      });
  }

  private setOutputData(data: any) {
    if (data != null) {
      data.errorsList.forEach((error: String) =>
        this.tableData.push({
          type: "ERROR",
          value: this.cleanText(error.toString())
        })
      );
      data.warningsList.forEach((warning: String) =>
        this.tableData.push({
          type: "WARNING",
          value: this.cleanText(warning.toString())
        })
      );
      this.window = 1;
    }
  }

  private setErrorData(response: string, status: string) {
    var message = "Unable to send request, received status " + status + ".";
    if (response != null) {
      message += ' with response "' + response + '"';
    }

    this.tableData.push({
      type: "ERROR",
      value: message
    });
    this.window = 1;
  }

  private cleanText(input: string): string {
    if (input.startsWith("Warning: ")) {
      return input.substring("Warning: ".length);
    } else if (input.startsWith("Errors: ")) {
      return input.substring("Errors: ".length);
    }
    return input;
  }
}
</script>

<template src="./file-display.html"></template>

<script lang="ts">
import mixins from "vue-class-component";
import { Component } from "vue-property-decorator";
import BaseComponent from "../../views/BaseComponent.vue";

@Component({
  props: ["fileInfo"]
})
export default class FileDisplay extends mixins(BaseComponent) {
  private tabidx: number = 0;
  private defaultDisplayValue: String = "Nothing to display";

  private getTabs(): Array<String> {
    var names: Array<String> = [];
    if (this.$props.fileInfo != null) {
      if (this.$props.fileInfo.base != null && this.$props.fileInfo.base != "") {
        names.push("Raw");
      }
      if (this.$props.fileInfo.validation != null && this.$props.fileInfo.validation != "") {
        names.push("Validation");
      }
      if (this.$props.fileInfo.humanReadable != null && this.$props.fileInfo.humanReadable != "") {
        names.push("Human Readable");
      }
    }
    return names;
  }

  private getText(): String {
    var activeName = this.getTabs()[this.tabidx];
    if (this.$props.fileInfo != null) {
      if (activeName == "Raw") {
        return this.getTextAsJson(this.$props.fileInfo.base);
      } else if (activeName == "Validation") {
        return this.getTextAsJson(this.$props.fileInfo.validation);
      } else if (activeName == "Human Readable") {
        return this.getTextAsJson(this.$props.fileInfo.humanReadable);
      }
    }
    return "";
  }

  private getTextAsJson(text: string): String {
    try {
      const temp = JSON.parse(text);
      return JSON.stringify(temp, null, 2);
    } catch (error) {
      return text;
    }
  }
}
</script>

<template src="./file-uploader.html"></template>

<script lang="ts">
import { Dataset } from "evaluation-api";
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { VCalendarCategory } from "vuetify/lib";

@Component
export default class FileUploader extends Vue {
  @Prop({ required: true, default: [] }) readonly categoryOptions!: Array<Dataset>;
  private files: Array<any> = [];
  private filenames: Array<String> = [];
  private category: Dataset =
    this.categoryOptions.length > 0 ? this.categoryOptions[0] : (null as any);
  private acceptedFileTypes = this.category.allowedTypes;

  @Watch("categoryOptions")
  private onCategoryChange(val: string, oldVal: string): void {
    this.category = this.categoryOptions.length > 0 ? this.categoryOptions[0] : (null as any);
    this.acceptedFileTypes = this.category.allowedTypes;
    this.files = [];
    this.filenames = [];
  }

  @Watch("category")
  private categoryWatcher(): void {
    // update accepted file types when category changes
    this.acceptedFileTypes = this.category.allowedTypes;
  }

  private onInputChange(): void {
    if (this.hasValidExtensions(this.files)) {
      this.filenames = this.files.map(f => f.name);
    } else {
      this.$alert(
        this.acceptedFileTypes,
        "Error: Only the following file types are accepted.",
        "error"
      );
      this.files = [];
      this.filenames = [];
    }
  }

  private hasValidExtensions(files: Array<File>): boolean {
    let valid = true;
    files.forEach(file => {
      if (!this.hasValidExtension(file)) {
        valid = false;
      }
    });
    return valid;
  }
  private hasValidExtension(file: File): boolean {
    if (this.acceptedFileTypes === "*") {
      return true;
    }
    let ext: String = file.name.split(".").pop() as String;
    ext = "." + ext;
    let validTypes: Array<String> = this.acceptedFileTypes.split(",");
    let match = validTypes.indexOf(ext);
    return match >= 0;
  }

  private onFileUpload(): void {
    let requests = this.files.map((file, i) => {
      return {
        file: file,
        filename: this.filenames[i],
        category: this.category.name,
        createHistory: this.category.name === "TA 2"
      };
    });

    this.$emit("file-uploads", requests);
    this.files = [];
    this.filenames = [];
  }

  private hasFilesToUpload(): boolean {
    return this.files.length > 0 ? true : false;
  }
}
</script>

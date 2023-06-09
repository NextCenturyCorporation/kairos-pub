<style scoped>
.list_alt_holder div i {
  margin-right: -96px;
}
</style>

<template src="./html/support.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import { rules } from "../utils/data-validation";
import NProgress from "nprogress";
import { deepClone, getHttpPostErrorNotice } from "../utils/otherFunctions";
import { ContactRequestDto, FaqCategory } from "zeus-api";
import BaseComponent from "../views/BaseComponent.vue";

@Component
export default class Support extends Mixins(BaseComponent) {
  private categories: Array<any> = this.getCategories();
  private topic: string = "";
  private message: string = "";
  private questionClasses = "primary--text text-subtitle-2 mt-5";
  private headingClasses: string = "font-weight-medium mb-2 primary--text";
  private categoryClasses: string = "text-subtitle-2 primary--text";
  private contactUsDialog: boolean = false;
  private validContactForm: boolean = true;
  private categoryOptions = ["Services"];
  private emailAddressRules: Array<Function> = rules.emailAddress;
  private topicRules: Array<Function> = rules.topic;
  private messageRules: Array<Function> = rules.message;

  get questionAnswerClasses(): string {
    return (
      //@ts-ignore (So VSCODE will stop showing an error in the file)
      (this.$vuetify.theme.dark ? "text--darken-1" : "text--lighten-1") +
      " body-2 font-weight-regular primary--text"
    );
  }

  private getCategories(): Array<any> {
    let storeCategories: Array<FaqCategory> = deepClone(this.$store.getters["faq/categories"]);
    storeCategories &&
      storeCategories.forEach((category: any) => {
        category.status = false;
      });
    this.$forceUpdate();
    return storeCategories;
  }

  private closeForm() {
    this.contactUsDialog = false;
    this.resetForm();
  }

  private submitForm() {
    let currentUser = this.$store.getters["user/currentUser"];
    let contactDto: ContactRequestDto = {
      requestor: currentUser.emailAddress,
      topic: this.topic,
      message: this.message
    };
    this.$store
      .dispatch("faq/submitQuestion", contactDto)
      .then(() => {
        this.contactUsDialog = false;
        this.resetForm();
        this.$store.dispatch("showAppSnackbarMessage", "Inquiry submitted");
      })
      .catch(errStatus => {
        let snackBarErrorMessage = getHttpPostErrorNotice(errStatus, this.$router);
        this.$store.dispatch("showErrorAppSnackbarMessage", snackBarErrorMessage);
      });
  }

  private resetForm(): void {
    if (this.$refs.contactUsForm) {
      (this.$refs.contactUsForm as any).reset();
    }
  }

  private updateReference(categoryIndex: any): void {
    let currentCategoryIndex = 0;
    this.categories.forEach(category => {
      category.status = currentCategoryIndex === categoryIndex ? !category.status : false;
      currentCategoryIndex++;
    });
  }

  private refreshCategories() {
    this.categories = [];
    NProgress.set(0.5);
    this.$store
      .dispatch("faq/retrieveFAQs")
      .then(() => {
        this.categories = this.getCategories();
      })
      .catch(error => {
        this.$store.dispatch("showErrorAppSnackbarMessage", "Failed to refresh Categories");
      })
      .then(() => {
        NProgress.done();
      });
  }
}
</script>

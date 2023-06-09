<style scoped>
.list_alt_holder div i {
  margin-right: -96px;
}
</style>

<template src="./modify-faqs.html"></template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import { rules } from "../../utils/data-validation";
import NProgress from "nprogress";
import { deepClone, getHttpPostErrorNotice } from "../../utils/otherFunctions";
import { ContactRequestDto, Faq, FaqCategory } from "zeus-api";
import BaseComponent from "../../views/BaseComponent.vue";
import draggable from "vuedraggable";

@Component({
  components: {
    draggable
  }
})
export default class Support extends Mixins(BaseComponent) {
  private deletionDialog: boolean = false;
  private sortFAQDialog: boolean = false;
  private editDialog: boolean = false;
  private addCategoryDialog: boolean = false;
  private addFAQDialog: boolean = false;
  private FAQeditDialog: boolean = false;
  private FAQdeletionDialog: boolean = false;
  private deleteFAQCategoryID: string = "";
  private newCategoryName: string = "";
  private selectedCategoryToEdit: any = null as any;
  private selectedFAQToEdit: any = null as any;
  private newQuestion: string = "";
  private newAnswer: string = "";
  private addFAQType: string = "";

  private categories: Array<any> = this.getCategories();
  private emailAddress = "";
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

  // Method for storing category ID to delete
  private openCategoryDeletePanel(categoryID: string, category: FaqCategory) {
    this.deleteFAQCategoryID = categoryID;
    this.selectedCategoryToEdit = category;
  }

  // Method for pushing category deletion to the store
  private confirmCategoryDelete(): void {
    this.$store.dispatch("faq/deleteFAQCategory", this.deleteFAQCategoryID).then(() => {
      this.refreshCategories();
    });
    this.deletionDialog = false;
    this.deleteFAQCategoryID = "";
  }

  // Method for setting values within the category edit panel
  private openEditCategoryName(category: FaqCategory): void {
    this.selectedCategoryToEdit = category;
    this.newCategoryName = category.categoryName;
  }

  // Method for pushing category edits to the store
  private confirmCategoryEdit(): void {
    this.selectedCategoryToEdit.categoryName = this.newCategoryName;
    this.$store.dispatch("faq/updateFAQCategory", this.selectedCategoryToEdit);
    this.resetFAQDialogs();
    this.editDialog = false;
  }

  // Method for setting values within the FAQ edit panel
  private openEditFAQ(FAQ: Faq): void {
    this.selectedFAQToEdit = FAQ;
    this.newQuestion = FAQ.question;
    this.newAnswer = FAQ.answer;
  }

  // Method for pushing FAQ updates to the store
  private confirmFAQEdit(): void {
    this.selectedFAQToEdit.question = this.newQuestion;
    this.selectedFAQToEdit.answer = this.newAnswer;
    this.$store.dispatch("faq/updateFAQ", this.selectedFAQToEdit);
    this.resetFAQDialogs();
    this.FAQeditDialog = false;
  }

  // Method for pushing new category to the store
  private confirmCategoryAdd(): void {
    let newCategory: FaqCategory = {
      categoryName: this.newCategoryName,
      listOrder: this.categories.length + 1
    };
    let newCategoryArr: Array<FaqCategory> = [];
    newCategoryArr.push(newCategory);
    this.$store.dispatch("faq/createFAQCategory", newCategoryArr).then(() => {
      this.refreshCategories();
    });
    this.resetFAQDialogs();
    this.addCategoryDialog = false;
  }

  // Method for pushing new FAQ's to the store
  private confirmFAQAdd(): void {
    let newFAQ: Faq = {
      question: this.newQuestion,
      answer: this.newAnswer,
      listOrder: this.selectedCategoryToEdit.faqs.length + 1
    };
    this.selectedCategoryToEdit.faqs.push(newFAQ);
    this.$store.dispatch("faq/createFAQ", this.selectedCategoryToEdit).then(() => {
      this.refreshCategories();
    });
    this.addFAQDialog = false;
    this.resetFAQDialogs();
  }

  // Method for setting values within the FAQ deletion panel
  private openFAQDeletePanel(FAQ: Faq, category: FaqCategory): void {
    this.newQuestion = FAQ.question;
    this.newAnswer = FAQ.answer;
    this.selectedFAQToEdit = FAQ;
    this.selectedCategoryToEdit = category;
  }

  // Calls store to delete a FAQ
  private confirmFAQDelete(): void {
    this.selectedCategoryToEdit.faqs.forEach((element: Faq, index: number) => {
      if (element.id == this.selectedFAQToEdit.id) {
        delete this.selectedCategoryToEdit.faqs[index];
      }
    });
    this.$store.dispatch("faq/deleteFAQ", this.selectedCategoryToEdit).then(() => {
      this.refreshCategories();
    });
    this.FAQdeletionDialog = false;
    this.resetFAQDialogs();
  }

  // Clears variables for FAQ/Category edit/deletion
  private resetFAQDialogs(): void {
    this.selectedCategoryToEdit = null;
    this.selectedFAQToEdit = null;
    this.newAnswer = "";
    this.newQuestion = "";
    this.newCategoryName = "";
  }

  private confirmCategorySort(): void {
    this.categories.forEach((category: FaqCategory, index: number) => {
      category.listOrder = index + 1;
      this.$store.dispatch("faq/updateFAQCategory", category);
    });
  }

  private confirmFAQSort(category: FaqCategory): void {
    if (category.faqs != null) {
      category.faqs.forEach((faq: Faq, index: number) => {
        faq.listOrder = index + 1;
        this.$store.dispatch("faq/updateFAQ", faq);
      });
    }
    this.sortFAQDialog = false;
  }

  get questionAnswerClasses(): string {
    return (
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
    let contactDto: ContactRequestDto = {
      requestor: this.emailAddress,
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
    if (this.$refs.contactForm) {
      (this.$refs.contactForm as any).reset();
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
        this.confirmCategorySort();
        this.categories.forEach((category: FaqCategory) => {
          this.confirmFAQSort(category);
        });
      });
  }
}
</script>

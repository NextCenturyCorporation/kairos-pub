import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";
import {
  ContactRequestDto,
  SupportApi,
  Faq,
  UpdateFaqCategoryByIdRequest,
  SubmitFaqCategoriesRequest,
  AddContactRequestRequest,
  UsersApi,
  DeleteFaqCategoryRequest,
  FaqCategory,
  UpdateFaqByIdRequest
} from "zeus-api";

const state = () => ({
  supportApi: new SupportApi(JWTAuthInterceptor.Instance),
  usersApi: new UsersApi(JWTAuthInterceptor.Instance),
  categories: [] as Array<FaqCategory>
});

const getters = {
  categories(vuexStoreState: any): Array<any> {
    // Sort categories by list order #
    vuexStoreState.categories.sort((a: FaqCategory, b: FaqCategory) =>
      a.listOrder > b.listOrder ? 1 : -1
    );
    vuexStoreState.categories.forEach((category: FaqCategory) => {
      if (category.faqs != null && category.faqs.length > 1) {
        category.faqs.sort((a: Faq, b: Faq) => (a.listOrder > b.listOrder ? 1 : -1));
      }
    });
    return vuexStoreState.categories;
  }
};

const mutations = {
  setFaqData(vuexStoreState: any, faqCategories: Array<FaqCategory>): void {
    vuexStoreState.categories = faqCategories;
  }
};

const actions = {
  submitQuestion(vuexStoreContext: any, contactRequestDto: ContactRequestDto) {
    return new Promise((resolve, reject) => {
      let request: AddContactRequestRequest = {
        contactRequestDto: contactRequestDto
      };
      (vuexStoreContext.state.usersApi as UsersApi).addContactRequest(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  retrieveFAQs(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.supportApi as SupportApi).retrieveFaqCategories().subscribe({
        next: (response: Array<FaqCategory>) => {
          vuexStoreContext.commit("setFaqData", response);
          resolve();
        },
        error: ({ response, status }) => {
          let errorInfo = response && response.message ? response.message : status;
          console.error("Error getting FAQs: " + errorInfo);
          reject();
        }
      });
    });
  },
  // createFaqCategory - receives a new FaqCategory as input, this FaqCategory won’t have an id
  createFAQCategory(vuexStoreContext: any, category: Array<FaqCategory>) {
    return new Promise((resolve, reject) => {
      let request: SubmitFaqCategoriesRequest = { faqCategory: category };
      (vuexStoreContext.state.supportApi as SupportApi).submitFaqCategories(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  // deleteFaqCategory - receives the id of a FaqCategory as input
  deleteFAQCategory(vuexStoreContext: any, FAQID: string) {
    return new Promise((resolve, reject) => {
      let request: DeleteFaqCategoryRequest = { id: FAQID };
      (vuexStoreContext.state.supportApi as SupportApi).deleteFaqCategory(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  // updateFaqCategory - receives a FaqCategory as input
  updateFAQCategory(vuexStoreContext: any, category: FaqCategory) {
    return new Promise((resolve, reject) => {
      let request: UpdateFaqCategoryByIdRequest = {
        id: category.id as string,
        faqCategory: category
      };
      (vuexStoreContext.state.supportApi as SupportApi).updateFaqCategoryById(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  // createFaq - receives an updated verison of the selected parent FaqCategory to push
  createFAQ(vuexStoreContext: any, category: FaqCategory) {
    return new Promise((resolve, reject) => {
      let request: UpdateFaqCategoryByIdRequest = {
        id: category.id as string,
        faqCategory: category
      };
      (vuexStoreContext.state.supportApi as SupportApi).updateFaqCategoryById(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  // deleteFaq - receives an updated verison of the selected parent FaqCategory to push
  deleteFAQ(vuexStoreContext: any, category: FaqCategory) {
    return new Promise((resolve, reject) => {
      let request: UpdateFaqCategoryByIdRequest = {
        id: category.id as string,
        faqCategory: category
      };
      (vuexStoreContext.state.supportApi as SupportApi).updateFaqCategoryById(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  // updateFaq - receives a FAQ object as input
  updateFAQ(vuexStoreContext: any, faq: Faq) {
    return new Promise((resolve, reject) => {
      let request: UpdateFaqByIdRequest = { id: faq.id as string, faq: faq };
      (vuexStoreContext.state.supportApi as SupportApi).updateFaqById(request).subscribe({
        next() {
          resolve();
        },
        error({ response, status }) {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error(consoleErrorMessage);
          reject(status);
        }
      });
    });
  }
};

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
};

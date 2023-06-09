import { JWTAuthInterceptor } from "@/classes/jwt-auth-interceptor";

import {
  EvaluationApi,
  Evaluation,
  Dataset,
  NewEvaluationRequest,
  UpdateEvaluationRequest,
  DeleteEvaluationRequest
} from "evaluation-api";

import { compareDates } from "../../utils/date";
const state = () => ({
  evaluationApi: new EvaluationApi(<any>JWTAuthInterceptor.Instance),
  evaluations: [] as Array<Evaluation>
});

const getters = {
  evaluations(vuexStoreState: any): string {
    vuexStoreState.evaluations.sort((a: Evaluation, b: Evaluation) => {
      return compareDates(b.creationDate, a.creationDate);
    });
    return vuexStoreState.evaluations;
  }
};

const mutations = {
  setEvaluations(vuexStoreState: any, evaluations: any): void {
    vuexStoreState.evaluations = evaluations;
  }
};

const actions = {
  createEvaluation(vuexStoreContext: any, evaluation: Evaluation) {
    return new Promise((resolve, reject) => {
      let request: NewEvaluationRequest = {
        evaluation: evaluation
      };
      console.log(request);
      (vuexStoreContext.state.evaluationApi as EvaluationApi).newEvaluation(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("createEvaluation: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  retrieveEvaluations(vuexStoreContext: any) {
    return new Promise((resolve, reject) => {
      (vuexStoreContext.state.evaluationApi as EvaluationApi).getEvaluations().subscribe({
        next: (response: any) => {
          vuexStoreContext.commit("setEvaluations", response);
          resolve();
        },
        error: (err: any) => {
          console.log("error");
          reject();
        }
      });
    });
  },
  updateEvaluation(vuexStoreContext: any, evaluation: Evaluation) {
    return new Promise((resolve, reject) => {
      let request: UpdateEvaluationRequest = {
        evaluation: evaluation
      };
      console.log(request);
      (vuexStoreContext.state.evaluationApi as EvaluationApi).updateEvaluation(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("updateEvaluation: " + consoleErrorMessage);
          reject(status);
        }
      });
    });
  },
  deleteEvaluation(vuexStoreContext: any, evaluation: Evaluation) {
    return new Promise((resolve, reject) => {
      let request: DeleteEvaluationRequest = {
        id: evaluation.id
      };
      console.log(request);
      (vuexStoreContext.state.evaluationApi as EvaluationApi).deleteEvaluation(request).subscribe({
        next: () => {
          resolve();
        },
        error: ({ response, status }) => {
          let consoleErrorMessage = response && response.message ? response.message : status;
          console.error("deleteEvaluation: " + consoleErrorMessage);
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

<template src="./html/complex-list.html"> </template>

<script lang="ts">
import "../assets/visStyle.css";
import { visualizationParser } from "../utils/visualization-parser";

import { Component, Mixins } from "vue-property-decorator";
import { deepClone } from "../utils/otherFunctions";

import EventComplex from "../components/simple-vis/simple-vis.vue";

import BaseComponent from "./BaseComponent.vue";

@Component({ components: { EventComplex } })
export default class ComplexList extends Mixins(BaseComponent) {
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";

  private namedGraph: string = "";
  private eventComplex: string = "";
  private eventComplexId: string = "";

  private loading: Boolean = true;
  private eventComplexView = false;
  private listOfNamedGraphs: Array<string> = this.getGraphNames(); //change to service
  private eventComplexList: Array<any> = [];

  private defaultHeaders: Array<any> = [{ text: "Complex Event", align: "left", value: "name" }, { text: "Confidence", align: "left", value: "confidence" }];
  private headers: Array<any> = [];
  private tabidx: number = 0;
  private tableLoadingFlag: boolean = false;
  private defaultDisplayValue: String = "Nothing to display";
  private sortBy: Array<String> = ["confidence"];
  private search: String = "";

  private created(): void {
    this.headers = this.getHeaders();
  }

  private getHeaders(): Array<any> {
    let h: Array<any> = this.defaultHeaders;
    h.push({ text: "", value: "vis" });
    return this.defaultHeaders;
  }

  private getGraphNames() {
    this.loading = true;
    this.$store
      .dispatch("visualization/retrieveGraphNames")
      .then(() => {
        this.listOfNamedGraphs = visualizationParser.createGraphNameList(
          this.$store.getters["visualization/graphNames"]
        );
        this.loading = false;
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    return visualizationParser.createGraphNameList(
      deepClone(this.$store.getters["visualization/graphNames"])
    );
  }

  private getEventComplexList() {
    this.$store
      .dispatch("visualization/retrieveEventComplexList", this.namedGraph)
      .then(() => {
        this.eventComplexList = visualizationParser.createEventComplexList(
          this.$store.getters["visualization/eventComplexList"]
        );
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    return visualizationParser.createEventComplexList(
      deepClone(this.$store.getters["visualization/eventComplexList"])
    );
  }

  private getEventComplex(event: any) {
    console.log(event.id);
    this.eventComplex = event.name;
    this.eventComplexId = event.id;
    this.eventComplexView = true;
    let request: any = {
      namedGraph: this.namedGraph,
      event: this.eventComplexId
    };
    this.$emit("getEventComplex", request);
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }
}
</script>

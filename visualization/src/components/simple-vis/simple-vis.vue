<template src="./simple-vis.html"></template>

<script lang="ts">
import "../../assets/visStyle.css";
import "../../scss/variables.scss"

import { defineComponent, ref } from "vue";
import { useTheme } from "vuetify";
import * as go from "gojs";

import  Vis from "../vis/vis.vue";
import { visualizationParser } from "../../utils/visualization-parser";
import type { ModelTemplate } from "../../interfaces/model-template";
import { ParserCommons } from "../../utils/visualization-parsers/parser-commons";

const $ = go.GraphObject.make;  // for conciseness in defining templates
const goJsClickDelay = 250;

enum NodeBackgroundColors {
  ADDITIONAL = "#0288D1",
  DETECTED = "red",
  PREDICTED = "white",
  MISSING = "#fbef8d",
  ALTERNATE = "white",
  UNKNOWN = "lightgray",
}

export default defineComponent({
  name: 'SimpleVis',
  components: { Vis },
  props: {
    namedGraph: { type: String, required: false, default: "" },
    eventComplex: { type: String, required: false, default: "" }
  },
  data() {
    return {
      acceptedFileTypes: ".json, .jsonld",
      additionalNotes: "",
      additionalNotesIcon: "M27.879 5.879l-3.757-3.757c-1.167-1.167-3.471-2.121-5.121-2.121h-14c-1.65 0-3 1.35-3 3v26c0 1.65 1.35 3 3 3h22c1.65 0 3-1.35 3-3v-18c0-1.65-0.955-3.955-2.121-5.121zM20 4.236c0.069 0.025 0.139 0.053 0.211 0.082 0.564 0.234 0.956 0.505 1.082 0.631l3.757 3.757c0.126 0.126 0.397 0.517 0.631 1.082 0.030 0.072 0.057 0.143 0.082 0.211h-5.764v-5.764zM26 28h-20v-24h12v8h8v16zM8 16h16v2h-16zM8 20h16v2h-16zM8 24h16v2h-16z",
      argumentSuffix: "-caci-argument",
      blue: "#0288D1",
      categoriesPresent: <Array<any>>[],
      defaultConfidence: [0,1],
      editModal: false,
      files: [],
      framedRaw: <any>[],
      groupSuffix: "-caci-group",
      jsonData: [],
      loading: false,
      moduleNodes: <Array<any>>[],
      moduleLinks: <Array<any>>[],
      myLastClick: -1,
      participantArgs: <Array<any>>[],
      reader: new FileReader(),
      red: "#F90917",
      relationDiagram: ref(null),
      relationsFound: "",
      rootNode: this.resetNodeModule(),
      rootRelations: {},
      search: "",
      selectedArgs: <Array<any>>[], // Array to hold status of argument filters
      selectedComplex: "",
      selectedEvent: this.resetNodeModule(),
      teal: "#03fcec",     
      theme: useTheme(),
      yellow: "#F0F634",
      participantsTable: [],
      participantsEventList: new Map(),
      pariciantsRelationsList: new Map(),
      standAlone: true,
      window: 0,
      argumentToggle: false
    }
  },
  watch: {
    // used watchers where v-on:change/@change was not working. calls functions when data of v-model changes.
    defaultConfidence(newConfidence: any) {
      this.filterConfidence(newConfidence);
    }
  },
  beforeMount() {
    // Add license info.  Must execute after loading the library and before you create your first Diagram.
    let gojsKey = import.meta.env.VITE_APP_GOJS_LICENSE_KEY;
    if (typeof gojsKey !== "undefined" && gojsKey !== "") {
      go.Diagram.licenseKey = gojsKey;
    }
  },
  mounted() {
    // Defining this variable here as some goJS elements and legacy code elswhere here do not like
    // the diagram being a reactive vue object.  Placing it in data() would make it reactive.
    this.$options.relationDiagram = ref(null);
    this.$options.myDiagram = <go.Diagram>{};
    this.initVis();
  },
  methods: {
    resetNodeModule(): ModelTemplate {
      return  {
        additionalNotes: "",
        arguments: [],
        argumentString: "",
        category: "",
        categoryStatus: "",
        childrenGate: "",
        childrenList: [],
        comment: [],
        confidence: 0,
        critical: true,
        description: "",
        endTime: 0,
        entityId: "",
        entityRoleName: "",
        expanded: false,
        fromSpot: "right",
        group: "",
        isGroup: false,
        isParent: false,
        isTreeExpanded: true,
        key: "",
        layer: 0,
        opacity: 1,
        parent: "",
        parentDisplayName: "",
        childrenDisplayNames: "",
        participants: [],
        qnode: {
          label: "",
          name: "",
          linkedQNodeId: "",
          linkedQLabelId: ""
        },
        relations: [],
        startTime: 0,
        status: false,
        subgroupEvents: [],
        ta1Explanation: "",
        text: "",
        toSpot: "left",
        visible: true,
        customTable: [],
        repeatable: false,
        origName: "",
        origDescription: ""
      };
    },

    async getTransformedData(input: any) {
      try {
        const parsedData = await visualizationParser.transformData(input);
        this.moduleNodes = parsedData.moduleNodes;
        this.setCategories(this.moduleNodes);
        this.moduleLinks = parsedData.moduleLinks;
        this.framedRaw = parsedData.framedRaw;
        this.participantsEventList = parsedData.participantsAcrossEvents;
        this.pariciantsRelationsList = parsedData.participantsAcrossRelations;
        this.setupData();
      } catch (error) {
        console.log(error);
        alert("Issue reading data");
      } finally {
        this.loading = false;
      }
    },

    setupData() {
      this.groupData();
      this.getRootNode();
      this.rootNode = this.selectedEvent;
      this.loading = false;
      this.buildTopLevelRelations(true);
      this.createArgumentNodes();
     
      if (!this.$options.myDiagram.div) {
        this.initVis();
      }
      else {
        this.$options.myDiagram.model = new go.GraphLinksModel(this.moduleNodes, this.moduleLinks);
      }
    },

    setCategories(nodes: any[]) {
      for (let node of nodes) {
        if (!this.categoriesPresent.includes(node.category)) {
          this.categoriesPresent.push(node.category);
        }
      }
    },

    categoryPresent(category: string) {
      return this.categoriesPresent.includes(category);
    },

    initVis(): void {
      const $ = go.GraphObject.make; // for conciseness in defining templates

      this.$options.myDiagram = $(
        go.Diagram,
        "visualization", // Diagram refers to its DIV HTML element by id
        {
          BackgroundSingleClicked: this.eventBuildTopLevelRelations,
          initialAutoScale: go.Diagram.Uniform,
          layout: $(go.LayeredDigraphLayout, {direction: 90, layerSpacing: 120, columnSpacing: 120}),
          isReadOnly: false, // deny the user permission to alter the diagram or zoom in or out
          isModelReadOnly: false,
          allowZoom: true,
          // allowDelete: false,
          allowInsert: true,
          allowLink: true,
          allowMove: true,
          allowTextEdit: true,
          "grid.visible": false, // display a grid in the background of the diagram
          "grid.gridCellSize": new go.Size(150, 150),
          "toolManager.hoverDelay": 100
        }
      );

      // Source Only
      this.$options.myDiagram.nodeTemplateMap.add(
        "additional",
        $(
          go.Node,
          new go.Binding("fromSpot", "fromSpot"),
          new go.Binding("toSpot", "toSpot"),
          new go.Binding("visible", "visible"),
          new go.Binding("opacity", "opacity"),
          {
            selectable: true,
            click: function(e, node) {
              if (e.clickCount === 1) {
                const time = e.timestamp;
                visComponent.myLastClick = e.timestamp;
                setTimeout(function() {
                  if (visComponent.myLastClick === time) {
                    visComponent.highLightAllLinksOut(e, node);
                  }
                }, goJsClickDelay);
              }
            },
            doubleClick: function(e, node) {
              visComponent.myLastClick = e.timestamp;
              visComponent.showModal(e, node);
            },
          },
          "Auto",
          $(
            go.Shape,
            "RoundedRectangle", // the border
            { fill: NodeBackgroundColors.ADDITIONAL},
            new go.Binding("stroke", "isHighlighted", function(h): any {
              return h ?  $(go.Brush, "Radial", { 0.0: "#edbb2f", 1.0: "#ede218" }) : "black";
            }).ofObject(),
            new go.Binding("strokeWidth", "isHighlighted", function(h): number {
              return h ? 10 : 2;
            }).ofObject()
          ),
          $(
            go.Panel,
            "Auto",
            { padding: 0.5 },
            $(go.Panel, "Vertical",
              $(go.Panel, "Horizontal",
                $(
                  go.TextBlock, // name
                  this.textStyle(),
                  new go.Binding("text", "text"),
                  { textAlign: "center", margin: 2, stroke: "black", maxLines: 2, width: 500, overflow: go.TextBlock.OverflowEllipsis },
                )
              ),
              {
                toolTip:
                $("ToolTip",
                    { "Border.stroke": this.theme.current.dark ? "white" : "black", "Border.strokeWidth": 2 },
                    $(go.TextBlock, { margin: 8, stroke: this.theme.current.dark ? "white" : "black", font: "bold 16px sans-serif"},
                    new go.Binding("text", "description")
                    ),
                    new go.Binding("visible", "", function(data): boolean {
                      return data.description && data.description.trim() != ""
                    })
                    )
            },

            $(
              go.Panel,
              "Horizontal",
              $("Button",
                    {
                        toolTip:
                        $("ToolTip",
                            { "Border.stroke": this.theme.current.dark ? "white" : "black", "Border.strokeWidth": 2 },
                            $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Expand Subgroups" }))
                    },
                    {   
                        "ButtonBorder.fill": this.blue,
                        "ButtonBorder.stroke": "black",
                        "ButtonBorder.strokeWidth": 3,
                        row: 4,
                        column: 1,
                        margin: 5,
                        click: this.expandChildren,
                    },
                    new go.Binding("visible", "isParent"),
                    $(go.TextBlock, { margin: 8, stroke: this.theme ? "white" : "black", font: "bold 16px sans-serif", text:"Expand" }),
                )
              ),// Vertical Button Panel
              $(
                  go.Panel,
                  "Vertical",
                  new go.Binding("visible", "", function(data): boolean {
                    if (data.argumentString != "")
                      return data.expanded;
                    else 
                      return false
                  }),
                  $(go.Shape, "LineH", {stroke:"black"}),
                  $(
                    go.TextBlock,
                    new go.Binding("text", "argumentString"),
                    { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "black", alignment: go.Spot.Center, maxLines: 20, width: 500}
                  )
                )// Arguments
            )// top panel,
          )
        ) // end Table Pane
      ); // end Node

      // Detected
      this.$options.myDiagram.nodeTemplateMap.add(
        "detected",
        $(
          go.Node,
          new go.Binding("fromSpot", "fromSpot"),
          new go.Binding("toSpot", "toSpot"),
          new go.Binding("visible", "visible"),
          new go.Binding("opacity", "opacity"),
          {
            selectable: true,
            click: function(e, node) {
              if (e.clickCount === 1) {
                const time = e.timestamp;
                visComponent.myLastClick = e.timestamp;
                setTimeout(function() {
                  if (visComponent.myLastClick === time) {
                    visComponent.highLightAllLinksOut(e, node);
                  }
                }, goJsClickDelay);
              }
            },
            doubleClick: function(e, node) {
              visComponent.myLastClick = e.timestamp;
              visComponent.showModal(e, node);
            },
          },
          "Auto",
          $(
            go.Shape,
            "RoundedRectangle", // the border
            { fill: NodeBackgroundColors.DETECTED},
            new go.Binding("stroke", "isHighlighted", function(h): any {
              return h ?  $(go.Brush, "Radial", { 0.0: "#edbb2f", 1.0: "#ede218" }) : "black";
            }).ofObject(),
            new go.Binding("strokeWidth", "isHighlighted", function(h): number {
              return h ? 10 : 2;
            }).ofObject()
          ),
          $(
            go.Panel,
            "Auto",
            { padding: 0.5 },
            $(go.Panel, "Vertical",
              $(go.Panel, "Horizontal",
                $(
                  go.TextBlock, // name
                  this.textStyle(),
                  new go.Binding("text", "text"),
                  { textAlign: "center", margin: 2, maxLines: 2, width: 500, stroke: "white", overflow: go.TextBlock.OverflowEllipsis },
                ),
              ),
              {
                toolTip:
                $("ToolTip",
                    { "Border.stroke": this.theme.current.dark ? "white" : "black", "Border.strokeWidth": 2 },
                    $(go.TextBlock, { margin: 8, stroke: this.theme.current.dark ? "white" : "black", font: "bold 16px sans-serif"},
                    new go.Binding("text", "description")
                    ),
                    new go.Binding("visible", "", function(data): boolean {
                      return data.description && data.description.trim() != ""
                    })
                    )
            },
            $(
              go.Panel,
              "Horizontal",
              $("Button",
                    {
                        toolTip:
                        $("ToolTip",
                            { "Border.stroke": this.theme.current.dark ? "white" : "black", "Border.strokeWidth": 2 },
                            $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Expand Subgroups" }))
                    },
                    {   
                        "ButtonBorder.fill": this.blue,
                        "ButtonBorder.stroke": "black",
                        "ButtonBorder.strokeWidth": 3,
                        row: 4,
                        column: 1,
                        margin: 5,
                        click: this.expandChildren,
                    },
                    new go.Binding("visible", "isParent"),
                    $(go.TextBlock, { margin: 8, stroke: this.theme ? "white" : "black", font: "bold 16px sans-serif", text:"Expand" }),
                )
              ),// Vertical Button Panel
              $(
                  go.Panel,
                  "Vertical",
                  new go.Binding("visible", "", function(data): boolean {
                    if (data.argumentString != "")
                      return data.expanded;
                    else 
                      return false
                  }),
                  $(go.Shape, "LineH", {stroke:"white"}),
                  $(
                    go.TextBlock,
                    new go.Binding("text", "argumentString"),
                    { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "white", alignment: go.Spot.Center, maxLines: 20, width: 500 }
                  )
                )// Arguments
            )// top panel
          ),
        )
      ); // end Node

      // Predicted
      this.$options.myDiagram.nodeTemplateMap.add(
        "predicted",
        $(
          go.Node,
          new go.Binding("fromSpot", "fromSpot"),
          new go.Binding("toSpot", "toSpot"),
          new go.Binding("visible", "visible"),
          new go.Binding("opacity", "opacity"),
          {
            fromSpot: go.Spot.Top,
            toSpot: go.Spot.BottomRight,
            selectable: true,
            click: function(e, node) {
              if (e.clickCount === 1) {
                const time = e.timestamp;
                visComponent.myLastClick = e.timestamp;
                setTimeout(function() {
                  if (visComponent.myLastClick === time) {
                    visComponent.highLightAllLinksOut(e, node);
                  }
                }, goJsClickDelay);
              }
            },
            doubleClick: function(e, node) {
              visComponent.myLastClick = e.timestamp;
              visComponent.showModal(e, node);
            },
          },
          "Auto",
          $(
            go.Shape,
            "RoundedRectangle", // the border
            { fill: NodeBackgroundColors.PREDICTED},
            new go.Binding("stroke", "isHighlighted", function(h): any {
              return h ?  $(go.Brush, "Radial", { 0.0: "#edbb2f", 1.0: "#ede218" }) : "black";
            }).ofObject(),
            new go.Binding("strokeWidth", "isHighlighted", function(h): number {
              return h ? 10 : 2;
            }).ofObject()
          ),
          $(
            go.Panel,
            "Auto",
            $(go.Panel, "Vertical",
              $(go.Panel, "Horizontal",
              $(
                  go.TextBlock, // name
                  this.textStyle(),
                  new go.Binding("text", "text"),
                  { textAlign: "center", margin: 4, maxLines: 2, width: 500, stroke: "black", overflow: go.TextBlock.OverflowEllipsis },
                ),
              ),
              {
                toolTip:
                $("ToolTip",
                    { "Border.stroke": this.theme.current.dark ? "white" : "black", "Border.strokeWidth": 2 },
                    $(go.TextBlock, { margin: 8, stroke: this.theme.current.dark ? "white" : "black", font: "bold 16px sans-serif"},
                    new go.Binding("text", "description"),
                    ),
                   
                    new go.Binding("visible", "", function(data): boolean {
                      return data.description && data.description.trim() != ""; 
                    }),)
            },
            $(
              go.Panel,
              "Horizontal",
              $("Button",
                    {
                        toolTip:
                        $("ToolTip",
                            { "Border.stroke": this.theme.current.dark ? "white" : "black", "Border.strokeWidth": 2 },
                            $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Expand Subgroups" }))
                    },
                    {   
                        "ButtonBorder.fill": this.blue,
                        "ButtonBorder.stroke": "black",
                        "ButtonBorder.strokeWidth": 3,
                        row: 4,
                        column: 1,
                        margin: 5,
                        click: this.expandChildren,
                    },
                    new go.Binding("visible", "isParent"),
                    $(go.TextBlock, { margin: 8, stroke: this.theme ? "white" : "black", font: "bold 16px sans-serif", text:"Expand" }),
                )
              ),// Vertical Button Panel
              $(
                  go.Panel,
                  "Vertical",
                  new go.Binding("visible", "", function(data): boolean {
                    if (data.argumentString != "")
                      return data.expanded;
                    else 
                      return false
                  }),
                  $(go.Shape, "LineH", {stroke:"black"}),
                  $(
                    go.TextBlock,
                    new go.Binding("text", "argumentString"),
                    { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "black", alignment: go.Spot.Center, maxLines: 20, width: 500}
                  )
                )// Arguments
            )
          )
        )
      ); // end Node

      // create the link template with outlink text
      this.$options.myDiagram.linkTemplateMap.add(
        "outlink",
        $(
          go.Link,
          new go.Binding("zOrder"),
          { routing: go.Link.AvoidsNodes, corner: 10, curve: go.Link.JumpOver, toShortLength: 6, toEndSegmentLength: 10, selectable : false },
          $(go.Shape, "RoundedRectangle", { stroke: "black", scale: 1, strokeWidth: 5}, new go.Binding("stroke", "highLighted", function (v) {return v ? "#F90917" : "black"})),
          $(
            go.Shape, // arrowhead
            { toArrow: "Triangle", stroke: "black", scale: 3, fill: "black" },
            new go.Binding("stroke", "highLighted", function (v) {return v ? "#F90917" : "black"}),
            new go.Binding("fill", "highLighted", function (v) {return v ? "#F90917" : "black"})
          )
        )
      );

      this.$options.myDiagram.linkTemplateMap.add(
        "noarrow",
        $(
          go.Link,
          new go.Binding("zOrder"),
          { routing: go.Link.AvoidsNodes, corner: 10, toShortLength: 50, toEndSegmentLength: 0, selectable : false },
          $(go.Shape, 
              "RoundedRectangle", 
              { scale: 1, strokeWidth: 2},
              new go.Binding("stroke", "highLighted", function (v) {return v ? "#F90917" : "#0288D1"}),
              new go.Binding("strokeWidth", "highLighted", function (v) {return v ? 5 : 5 })
              
          )
        )
      );
      
      // Grouping
      this.$options.myDiagram.groupTemplate = $(
        go.Group,
        "Auto",
        new go.Binding("visible", "visible"),
        {
          selectionAdornmentTemplate:
            $(go.Adornment, "Auto",
              $(go.Shape, "RoundedRectangle",
              { fill: null, stroke: "black", strokeWidth: 8 }),
              $(go.Placeholder)
            )  // end Adornment
        },
        {
          click: this.groupClick,
          doubleClick: this.showModal
        },
        { layout: $(go.LayeredDigraphLayout, { direction: 0, columnSpacing: 75, layerSpacing: 150 }) },
        $(
          go.Shape,
          "RoundedRectangle", // surrounds everything
          { parameter1: 10, fill: "transparent", stroke: this.theme.current.dark ? "white" : "black" }
        ),
        $(
          go.Panel,
          "Vertical", // position header above the subgraph
          { defaultAlignment: go.Spot.Left },
          $(
            go.Panel,
            "Horizontal", // the header
            { defaultAlignment: go.Spot.Top },
          //   $("SubGraphExpanderButton", { row: 0, column: 0, margin: 5, width: 65, height: 65, background: this.theme ? "white" : "black" }), // this Panel acts as a Button
            $(
              go.TextBlock,
              this.textStyle(), // group title near top, next to button
              {
                textAlign: "left",
                stroke: this.theme.current.dark ? "white" : "black",
                alignment: go.Spot.Center
              },
              new go.Binding("text", "text")
            ),
            $(
              go.TextBlock,
              new go.Binding("text", "childrenGate"),
              { font: "bold 44pt Roboto", textAlign: "center", margin: 5, stroke: "black", alignment: go.Spot.Center },
              {
                textAlign: "right",
                stroke: this.theme.current.dark ? "white" : "black"
              }
            )
          ),
          $(
            go.Placeholder, // represents area for all member parts
            { padding: new go.Margin(15, 15), background: "transparent" }
          )
        )
      );

      // Orphan Grouping
      this.$options.myDiagram.groupTemplateMap.add(
        "orphanage",
        $(
        go.Group,
        "Auto",
        new go.Binding("visible", "visible"),
        {
          click: this.groupClick,
          doubleClick: this.showModal
        },
        {
          selectionAdornmentTemplate:
            $(go.Adornment, "Auto",
              $(go.Shape, "RoundedRectangle",
              { fill: null, stroke: "black", strokeWidth: 8 }),
              $(go.Placeholder)
            )  // end Adornment
        },
        { layout: $(go.LayeredDigraphLayout, { direction: 90, columnSpacing: 75, layerSpacing: 150 }) },
        $(
          go.Shape,
          "RoundedRectangle", // surrounds everything
          { parameter1: 10, fill: "transparent", stroke: this.theme.current.dark ? "white" : "black" }
        ),
        $(
          go.Panel,
          "Vertical", // position header above the subgraph
          { defaultAlignment: go.Spot.Left },
          $(
            go.Panel,
            "Horizontal", // the header
            { defaultAlignment: go.Spot.Top },
          //   $("SubGraphExpanderButton", { row: 0, column: 0, margin: 5, width: 65, height: 65, background: this.theme ? "white" : "black" }), // this Panel acts as a Button
            $(
              go.TextBlock,
              this.textStyle(), // group title near top, next to button
              {
                textAlign: "left",
                stroke: this.theme.current.dark ? "white" : "black",
                alignment: go.Spot.Center
              },
              new go.Binding("text", "text")
            ),
            $(
              go.TextBlock,
              new go.Binding("text", "childrenGate"),
              { font: "bold 44pt Roboto", textAlign: "center", margin: 5, stroke: this.theme.current.dark ? "white" : "black", alignment: go.Spot.Center }  
            )
          ),
          $(
            go.Placeholder, // represents area for all member parts
            { padding: new go.Margin(15, 15), background: "transparent" }
          )
        )
      )
      )
      
      const visComponent = this;
      this.$options.myDiagram.model = new go.GraphLinksModel(this.moduleNodes, this.moduleLinks);

      $(go.Overview, "overviewVis", {
        observed: <go.Diagram> this.$options.myDiagram,
        contentAlignment: go.Spot.Center
      });
    },

    textStyle(): any {
      return { font: "44pt Roboto", textAlign: "center", margin: 5, stroke: "black" };
    },

    scaleWidth(num: any) {
      return num;
     // return num * this.$options.myDiagram._widthFactor;
    },

    linkColorConverter(linkdata: any, elt: any) {
      var link = elt.part;
      if (!link) return this.blue;
      var f = link.fromNode;
      if (!f || !f.data || !f.data.critical) return this.blue;
      var t = link.toNode;
      if (!t || !t.data || !t.data.critical) return this.blue;
      return this.teal; // when both Link.fromNode.data.critical and Link.toNode.data.critical
    },

    groupData(): void {
      this.moduleNodes.forEach((node: { key: string, group: string; subgroupEvents: any[] }) => {
        if (node.subgroupEvents.length > 0) {
          node.subgroupEvents.forEach((child: any) => {
            var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key == child["@id"]);
            if (objIndex >= 0) {
                this.moduleNodes[objIndex].group = node.key;
            }
          });
        }
      });
    },

    updateEvent(e: any, obj: any): void {
      let task = obj.part.data;
      this.selectedEvent = task;
    },

    handleErrorStatus(errorStatus: any): void {
      let errorMessage =
        errorStatus === 401
          ? "User not logged in"
          : "Unexpected error occured; please try again or contact support";
      // this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
    },

    getBackgroundColor(category: string) {
      let backgroundColor: string = "";

      switch (category) {
        case "missing":
          backgroundColor = NodeBackgroundColors.MISSING;
          break;
        case "additional":
          backgroundColor = NodeBackgroundColors.ADDITIONAL;
          break;
        case "predicted":
          backgroundColor = NodeBackgroundColors.PREDICTED;
          break;
        case "detected":
          backgroundColor = NodeBackgroundColors.DETECTED;
          break;
        case "alternate":
          backgroundColor = NodeBackgroundColors.ALTERNATE;
          break;
        case "unknown":
          backgroundColor = NodeBackgroundColors.UNKNOWN;
          break;
      }
      return {
        "background-color": backgroundColor
      };
    },

    getRootNode(): void {
      for (let data of this.framedRaw[ParserCommons.getKairosReference("events")][0]["@list"]) {
          if (data[ParserCommons.getKairosReference("isTopLevel")] ? data[ParserCommons.getKairosReference("isTopLevel")][0]["@value"] : false) {
              this.selectedComplex = data["http://schema.org/name"] ? data["http://schema.org/name"][0]["@value"] : "";
              this.selectedEvent.text = data["http://schema.org/name"] ? data["http://schema.org/name"][0]["@value"] :"";
              this.selectedEvent.description = data["http://schema.org/description"]
                ? data["http://schema.org/description"][0]["@value"]
                : "";
              this.selectedEvent.key = data["@id"];
              this.selectedEvent.relations = [];
              break;
          }
      }
      if (this.selectedEvent.key === "") {
        alert("No Top Level Event")
      }
    },

    searchDiagram(event: Event) {
        const input = this.search;
        (<go.Diagram>this.$options.myDiagram).startTransaction("highlight-search");
        this.$options.myDiagram.clearHighlighteds();
        this.selectedArgs = [];


        if (input && input.toString().trim().length > 0) {
        // search four different data properties for the string, any of which may match for success
        // create a case insensitive RegExp from what the user typed
        const safe = input.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
        const regex = new RegExp(safe, "i");
        const results: go.Iterator<go.Node> = this.$options.myDiagram.findNodesByExample(
            { key: regex },
            { description: regex },
            { text: regex },
            { argumentString: regex}
        );

        while (results.next()) {
          const valueKey: go.Key = results.value.key;
          if (valueKey && !valueKey.toString().includes(this.argumentSuffix)) {
            results.value.isHighlighted = true;
          }
        }

        // try to center the diagram at the first node that was found
        if (results && results.count > 0) {
          const first: any = results.first();
          this.$options.myDiagram.centerRect(first.actualBounds);
        }
      }
      this.$options.myDiagram.commitTransaction("highlight-search");
    },

    expandChildren(e: any, obj: any):void {
      // Flag the event as handled so it doesn't fall through to the node and also trigger
      // argument expansion.
      e.handled = true;

      // Update the event as necessary.
      this.updateEvent(e,obj);
      this.$options.myDiagram.startTransaction('update');
    
      let task = obj.part.data;
      this.filterLinks([task.key+this.groupSuffix]);

      let results = this.moduleLinks.filter((obj: { from: any }) => obj.from == task.key);
      if (results.length > 0) {
          for (let subGroup of results) {
              var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key == subGroup.to);
              // recursively hide children
              if (objIndex >= 0 && this.moduleNodes[objIndex].visible && this.moduleNodes[objIndex].isGroup) {
                  this.hideChildren(objIndex);
              } else if (objIndex >= 0 && this.moduleNodes[objIndex].isGroup) {
                  this.moduleNodes[objIndex].visible = !this.moduleNodes[objIndex].visible;
                  this.$options.myDiagram.model.assignAllDataProperties(this.moduleNodes[objIndex],this.moduleNodes[objIndex]);
                  if(this.moduleNodes[objIndex].subgroupEvents.length > 0) {
                      for (let subGroup of this.moduleNodes[objIndex].subgroupEvents) {
                          objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key == subGroup["@id"]);
                          if (objIndex >= 0)
                              this.moduleNodes[objIndex].visible = !this.moduleNodes[objIndex].visible;
                              this.$options.myDiagram.model.assignAllDataProperties(this.moduleNodes[objIndex],this.moduleNodes[objIndex]);
                      }
                  }
              }
          }
      }
      this.$options.myDiagram.updateAllTargetBindings();
      this.$options.myDiagram.commitTransaction('update');
    },

    expandArguments(e: any, obj: any):void {
      this.$options.myDiagram.startTransaction('argument');
      let task = obj.part.data;
      
      var id = task.key;// + this.argumentSuffix;
      this.filterLinks([id]);
      var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === id);
      if (objIndex >= 0) {
        this.$options.myDiagram.model.setDataProperty(this.moduleNodes[objIndex],"expanded", !this.moduleNodes[objIndex].expanded);
      }
      // this.$options.myDiagram.updateAllTargetBindings();
      this.$options.myDiagram.commitTransaction('argument');
    },

    hideChildren(index: number): void {
      this.moduleNodes[index].visible = false;
      this.$options.myDiagram.model.assignAllDataProperties(this.moduleNodes[index],this.moduleNodes[index]);
      let results =  [];
      if (this.moduleNodes[index].key.includes(this.groupSuffix)) {
          for (let group of this.moduleNodes[index].subgroupEvents) {
              results.push(group["@id"]);
          }
      } else {
          const filtered = this.moduleLinks.filter((x: {from: string}) => x.from === this.moduleNodes[index].key);
          for (let link of filtered) {
              results.push(link.to);
          }
      }
      
      for (let result of results) {
          const objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === result);
          if (objIndex >= 0) 
              this.hideChildren(objIndex);
      }
    },

    filterConfidence(input: any) {
        (<go.Diagram>this.$options.myDiagram).startTransaction("confidence-filter");
        let lower = input[0];
        let upper = input[1];
        for(let node of this.moduleNodes) {
          if ((node.confidence > upper || node.confidence < lower) && !node.key.includes(this.argumentSuffix)) {
            node.opacity = .3;
          } else {
            node.opacity = 1;
          }
        }
        this.$options.myDiagram.updateAllTargetBindings();
        this.$options.myDiagram.commitTransaction("confidence-filter");
    },

    groupClick(e: any, obj: any): void {
        this.$options.myDiagram.startTransaction();
        let task = obj.part.data;
        this.filterLinks([task.key]);
        this.$options.myDiagram.commitTransaction();
        this.updateEvent(e,obj);
        this.updateRelations(e,obj);
        if (task.key === this.rootNode.key) {
          this.buildTopLevelRelations(false);
        }
        
    },

    argumentClick(e: any, obj: any): void {
        this.$options.myDiagram.startTransaction();
        let task = obj.part.data;
        this.filterLinks([task.key]);
        this.$options.myDiagram.commitTransaction();
    },

    highLightAllLinksOut(e: any, obj: any): void {
        this.updateEvent(e,obj);
        this.updateRelations(e,obj);
        this.expandArguments(e,obj);
        this.$options.myDiagram.startTransaction();
        let task = obj.part.data;
        let results = this.moduleLinks.filter((obj: { to: any }) => obj.to == task.key);
        var arrayExt = [];
        arrayExt.push(task.key + this.groupSuffix);
        for (var item of results) {
          arrayExt.push(item.to);
        }
        this.filterLinks(arrayExt);
        this.$options.myDiagram.commitTransaction();
    },

    filterLinks(ModelKey: Array<String>): void {
        for (let i = 0; i < this.moduleLinks.length; i++) {
            if (ModelKey.includes(this.moduleLinks[i].to)) {
                this.$options.myDiagram.model.setDataProperty(this.moduleLinks[i], "highLighted", true);
                this.$options.myDiagram.model.setDataProperty(this.moduleLinks[i], "zOrder", 100);
            }
            else {
                this.$options.myDiagram.model.setDataProperty(this.moduleLinks[i], "highLighted", false);
                this.$options.myDiagram.model.setDataProperty(this.moduleLinks[i], "zOrder", 1);
            }
        }
    },
    toggleArguments(): void {
      this.$options.myDiagram.startTransaction("ToggleArguments");
      for (var i = 0; i < this.moduleNodes.length; i++) {
        this.$options.myDiagram.model.setDataProperty(this.moduleNodes[i],"expanded", this.argumentToggle);
      }
      this.$options.myDiagram.updateAllTargetBindings();
      this.$options.myDiagram.commitTransaction("ToggleArguments");
    },
    expandAll(): void {
        this.$options.myDiagram.startTransaction("ExpandAll");
        for (var i = 0; i < this.moduleNodes.length; i++) {
          this.moduleNodes[i].visible = true;
          if (this.moduleNodes[i].arguments.length > 0) {
            this.$options.myDiagram.model.assignAllDataProperties(this.moduleNodes[i],this.moduleNodes[i]);
            // this.$options.myDiagram.model.setDataProperty(this.moduleNodes[i],"expanded", true);
          }
        }
        this.$options.myDiagram.updateAllTargetBindings();
        this.$options.myDiagram.commitTransaction("ExpandAll");
    },
    collapseAll(): void {
        this.$options.myDiagram.startTransaction("CollapseAll");
        for (var i = 0; i < this.moduleNodes.length; i++) {
            if (this.moduleNodes[i].key != this.rootNode.key && this.moduleNodes[i].parent != this.rootNode.key &&  this.moduleNodes[i].parent != this.rootNode.key + this.groupSuffix 
            || (this.moduleNodes[i].key != this.rootNode.key && this.moduleNodes[i].key.includes(this.groupSuffix))) {
                this.moduleNodes[i].visible = false;
                this.$options.myDiagram.model.assignAllDataProperties(this.moduleNodes[i],this.moduleNodes[i]);
            }
            // this.$options.myDiagram.model.setDataProperty(this.moduleNodes[i],"expanded", false);
        }
        this.$options.myDiagram.updateAllTargetBindings();
        this.$options.myDiagram.commitTransaction("CollapseAll");
    },
    updateRelations(e: any, obj: any): void {

      let task = obj.part.data;
      let hasRelations = false;
      let nextNodeID = 0;

      this.selectedEvent = task;

      let nodes: Array<any> = [];
      let edges: Array<any> = [];

      if (task.relations.length > 0) {
        // For each argument that exists, create a node
        task.relations.forEach((relation: any) => {
        let textColor = "white";
        let border = "black";
        let background = "red";
        var groupId = nextNodeID + "-group";
        let titleSuffix =  relation["http://schema.org/name"] ? (": " + relation["http://schema.org/name"][0]["@value"]) : ""
        let title = "Instantiated" + titleSuffix;
          if (!relation[ParserCommons.getKairosReference("relationProvenance")]) {
            border = "black";
            background = "white";
            title = "Predicted" + titleSuffix;
            textColor = "black";
          }

          nodes.push({
            key: groupId,
            isGroup: true
          });
          nodes.push({
                key: nextNodeID++,
                label: relation[ParserCommons.getKairosReference("relationSubject")][0]["@id"],
                background: background,
                border: border,
                title: title,
                font: { size: 50, color: textColor},
                group: groupId
          });
          edges.push({
            from: nextNodeID -1,
            to: nextNodeID
          });

          nodes.push({
                key: nextNodeID++,
                label: relation[ParserCommons.getKairosReference("wd_label")][0]["@list"][0]["@value"],
                background: background,
                border: border,
                title: title,
                font: { size: 50, color: textColor },
                group: groupId
          });
          edges.push({
            from: nextNodeID -1,
            to: nextNodeID
          });
          nodes.push({
            key: nextNodeID++,
            label: relation[ParserCommons.getKairosReference("relationObject")][0]["@id"],
            background: background,
            border: border,
            title: title,
            font: { size: 50, color: textColor },
            group: groupId
          });
        });
        hasRelations = true;
      }

      if (hasRelations) {
        this.relationsFound = "";
      } else {
        // If there are no arguments/relations, alert the user
        this.relationsFound = ":   No relations found";
      }

      (<any>this.$refs.relationDiagram).setRelations(nodes, edges);
    },

    eventBuildTopLevelRelations() {
      this.buildTopLevelRelations(false);
    },

    buildTopLevelRelations(firstLoad: boolean) {
      this.selectedEvent = this.resetNodeModule();
      this.getRootNode();
      let parentRelations: any = {
        part: {
          data: this.selectedEvent
        }
      }
      if (this.framedRaw[ParserCommons.getKairosReference("relations")]) {
        for (let relation of this.framedRaw[ParserCommons.getKairosReference("relations")]) {
          parentRelations.part.data.relations.push(relation);
        }
        // Lookup event / entity values from IDs
        parentRelations.part.data = ParserCommons.getRelationReference(parentRelations.part.data, this.framedRaw);
        
        if (firstLoad) {
          parentRelations.part.data.relations.forEach((relation: any) => {
            let ogRelationObj = relation[ParserCommons.getKairosReference("relationObject")][0]["@id"];
            let ogRelationSub = relation[ParserCommons.getKairosReference("relationSubject")][0]["@id"];
            parentRelations.part.data.relations.forEach((isDuplicate: any, duplicateIndex: number) => {
                if (relation[ParserCommons.getKairosReference("relationSubject")][0]["@id"] === isDuplicate[ParserCommons.getKairosReference("relationSubject")][0]["@id"]
                && relation[ParserCommons.getKairosReference("wd_label")][0]["@list"][0]["@value"] === isDuplicate[ParserCommons.getKairosReference("wd_label")][0]["@list"][0]["@value"]
                && isDuplicate[ParserCommons.getKairosReference("relationObject")][0]["@id"] !== ogRelationObj) {

                  relation[ParserCommons.getKairosReference("relationObject")][0]["@id"] = relation[ParserCommons.getKairosReference("relationObject")][0]["@id"] +" AND " + isDuplicate[ParserCommons.getKairosReference("relationObject")][0]["@id"];
                  parentRelations.part.data.relations.splice(duplicateIndex, 1);
                
                } else if (relation[ParserCommons.getKairosReference("relationObject")][0]["@id"] === isDuplicate[ParserCommons.getKairosReference("relationObject")][0]["@id"]
                && relation[ParserCommons.getKairosReference("wd_label")][0]["@list"][0]["@value"] === isDuplicate[ParserCommons.getKairosReference("wd_label")][0]["@list"][0]["@value"]
                && ogRelationSub !== isDuplicate[ParserCommons.getKairosReference("relationSubject")][0]["@id"]) {

                  relation[ParserCommons.getKairosReference("relationSubject")][0]["@id"] = relation[ParserCommons.getKairosReference("relationSubject")][0]["@id"] + " AND " + isDuplicate[ParserCommons.getKairosReference("relationSubject")][0]["@id"];
                  parentRelations.part.data.relations.splice(duplicateIndex, 1);
                }
              });
          });
          this.rootRelations = parentRelations;
        } else {
          parentRelations = this.rootRelations;
        }
        

        this.updateRelations({},parentRelations);
      }
    },

    createArgumentNodes() {
      this.participantArgs = [];
      for (let task of this.moduleNodes) {
        if (task.arguments && task.arguments.length > 0) {
          // For each argument that exists, create a node
          task.arguments.forEach((singleArg: any) => {
          
            let argExists = false;
            this.participantArgs.forEach((argument: any, index: number) => {
              if (
                singleArg.id === argument.id
              ) {
                argExists = true;
                this.participantArgs[index].count++;
              }
            });

            if (!argExists) {
              this.participantArgs.push({
                id: singleArg.id,
                name: singleArg.name,
                count: 1
              });
            }
          });
        }
      }

      function compare( a: any, b: any ) {
        if ( a.count < b.count ){
          return 1;
        }
        if ( a.count > b.count ){
          return -1;
        }
        return 0;
      }

      this.participantArgs.sort(compare);
    },

      // Filters Event Complex viusalization when items are selected
    filterArguments(): void {
      this.$options.myDiagram.startTransaction("highlight-filter");
      // Remove all highlights
      this.$options.myDiagram.clearHighlighteds();
      this.search = "";

      this.moduleNodes.forEach((event: any) => {
        let eventArgs: Array<number> = [];
        // If an event has arguments, filter
        if (event.arguments && event.arguments.length > 0) {
          event.arguments.forEach((argument: any) => {
            this.participantArgs.forEach((arg: any, argIndex: any) => {
              if (arg.id === argument.id) {
                // Add the index of the arguments found in the selected event to an array (this index value cooresponds with the "selectedArgs" values)
                eventArgs.push(argIndex);
              }
            });
          });
          // if the event contains at least one of the selected args, display it. Otherwise hide it
          const found = this.selectedArgs.some(r => eventArgs.indexOf(r) >= 0);
          if (found) {
            var results = this.$options.myDiagram.findNodesByExample({ key: event.key });
            while (results.next()) {
              results.value.isHighlighted = true;
            }
          }
        }
      });
      this.$options.myDiagram.commitTransaction("highlight-filter");
    },

    onFileUpload(): void {
      if (this.files != null && this.files.length > 0) {
        this.argumentToggle = false;
        this.loading = true;
        const file = this.files[0];
        this.reader.readAsText(file);
        this.reader.onload = res => {
          if (this.reader.result) {
            let data: any = JSON.parse(this.reader.result.toString());
            this.getTransformedData(data);
          }
        };

      }
    },

  showModal(e: any, obj: any): void {
    this.selectedEvent = obj.part.data;
    if (this.selectedEvent.key != this.rootNode.key){
      this.additionalNotes = obj.part.data.additionalNotes;
      this.getDisplayInformation(obj.part.data);
      this.editModal = true;
    }
  },

  getDisplayInformation(event: any): void {
    this.selectedEvent.childrenDisplayNames = "";
    const childrenList = [];
    var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === event.parent);
    if (objIndex >= 0) {
      this.selectedEvent.parentDisplayName = this.moduleNodes[objIndex].text;
    }
    for (var i = 0; i < event.childrenList.length; i++) {
      var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === event.childrenList[i]["@id"]);
      if (objIndex >= 0) {
        childrenList.push(this.moduleNodes[objIndex].text);
      }
    }
    this.selectedEvent.childrenDisplayNames = childrenList.join(",");
    this.participantsTable = event.customTable;
  },

  saveData(): void {
    var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === this.selectedEvent.key);
    if (objIndex >= 0) {
      this.$options.myDiagram.model.setDataProperty(this.moduleNodes[objIndex],"additionalNotes", this.additionalNotes);
      this.sendSaveToServer();
    }
    this.editModal = false;
  },

  getEventsForParticipants(items: any): any {
    var results = [];
    for(let item of items) {
      results.push(this.participantsEventList.get(item.id) ? this.participantsEventList.get(item.id) : []);
    }
    return results.join(",");
  },

  sendSaveToServer(): void {
    // var userName = this.$store.getters["user/currentUser"].username;
    // let request: SaveOrUpdateRequest = {
    //   namedGraph: this.namedGraph,
    //   eventKey: this.selectedEvent.key,
    //   additionalInfo: this.additionalNotes.replace(/\n/g,'\\\\n'),
    //   userName: userName
    // };
    // this.$store
    //   .dispatch("visualization/saveOrUpdateNode", request)
    //   .then(() => {
    //     console.log("saving");
    //   })
    //   .catch(errorStatus => {
    //     this.handleErrorStatus(errorStatus);
    //   });
  }

}

});
</script>

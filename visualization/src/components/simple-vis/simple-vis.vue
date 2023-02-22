<template src="./simple-vis.html"> </template>

<script lang="ts">
import "../../assets/visStyle.css";

import { Component, Mixins, Vue, Prop, Watch } from "vue-property-decorator";
import * as go from "gojs";

import Vis from "../vis/vis.vue";
import { visualizationParser } from "../../utils/visualization-parser";
import { ModelTemplate } from "../../interfaces/vis-node-template";
import { ParserCommons } from "../../utils/visualization-parsers/parser-commons";

export enum NodeBackgroundColors {
  ADDITIONAL = "#0288D1",
  DETECTED = "red",
  PREDICTED = "white",
  MISSING = "#fbef8d",
  ALTERNATE = "white",
  UNKNOWN = "lightgray"
}

const SimpleVisProps = Vue.extend({
  props: {
    jsonInput: Array
  }
});

@Component({ components: { Vis } })
export default class SimpleVis extends Mixins(SimpleVisProps) {
  private files: Array<File> = [];
  private acceptedFileTypes = ".json, .jsonld";
  private reader = new FileReader();


  private theme: any = this.$vuetify.theme.dark;
  private myDiagram: any;
  @Prop({ required: false, default: "" }) readonly namedGraph!: string;
  @Prop({ required: false, default: "" }) readonly eventComplex!: string;
  private jsonData: any = [];
  private categoriesPresent: string[] = [];

  private moduleNodes: any = [];
  private moduleLinks: any = [];
  private framedRaw: any;
  // colors used, named for easier identification
  private blue: any = "#0288D1";
  private yellow: any = "#F0F634";
  private red: any = "#F90917";
  private teal: any = "#03fcec";

  private argumentSuffix: string ="-caci-argument";
  private groupSuffix: string ="-caci-group";

  // Array to hold status of argument filters
  private selectedArgs: Array<number> = [];

  private search: String = "";

  private loading: boolean = false;
  private defaultConfidence: any = [0,1];
  private selectedComplex: String = "";

  private relationsFound: string = "";
  private participantArgs: Array<{ id:string, name: string; count: number }> = [];
  
  private selectedEvent: ModelTemplate = this.resetNodeModule();
  private rootNode: ModelTemplate = this.resetNodeModule();
  private rootRelations: any = {};

  private editModal: boolean = false;
  private additionalNotes: string = "";

  private additionalNotesIcon: string = "M27.879 5.879l-3.757-3.757c-1.167-1.167-3.471-2.121-5.121-2.121h-14c-1.65 0-3 1.35-3 3v26c0 1.65 1.35 3 3 3h22c1.65 0 3-1.35 3-3v-18c0-1.65-0.955-3.955-2.121-5.121zM20 4.236c0.069 0.025 0.139 0.053 0.211 0.082 0.564 0.234 0.956 0.505 1.082 0.631l3.757 3.757c0.126 0.126 0.397 0.517 0.631 1.082 0.030 0.072 0.057 0.143 0.082 0.211h-5.764v-5.764zM26 28h-20v-24h12v8h8v16zM8 16h16v2h-16zM8 20h16v2h-16zM8 24h16v2h-16z";

  resetNodeModule(): ModelTemplate {
    return  {
    key: "",
    text: "",
    group: "",
    visible: true,
    isTreeExpanded: true,
    isGroup: false,
    description: "",
    confidence: 0,
    category: "",
    critical: true,
    startTime: 0,
    endTime: 0,
    entityId: "",
    entityRoleName: "",
    subgroupEvents: [],
    participants: [],
    status: false,
    arguments: [],
    argumentString: "",
    qnode: {
      label: "",
      name: "",
      linkedQNodeId: "",
      linkedQLabelId: ""
    },
    comment: [],
    ta1Explanation: "",
    relations: [],
    childrenGate: "",
    parent: "",
    isParent: false,
    toSpot: "left",
    fromSpot: "right",
    layer: 0,
    opacity: 1,
    expanded: false,
    additionalNotes: ""
  };
  }

  @Watch("jsonInput")
  private getEventComplex() {
    if (this.jsonInput == null) return;
    this.getTransformedData(this.jsonInput);
  }

  private async getTransformedData(input: any) {
    var parsedData = await visualizationParser.transformData(input);
    this.moduleNodes = parsedData.moduleNodes;
    this.setCategories(this.moduleNodes);
    this.moduleLinks = parsedData.moduleLinks;
    this.framedRaw = parsedData.framedRaw;
    this.setupData();
  }

  private setupData() {
    this.groupData();
    this.getRootNode();
    this.rootNode = this.selectedEvent;
    this.loading = false;
    this.buildTopLevelRelations(true);
    this.createArgumentNodes();
    if (!this.myDiagram)
      this.initVis();
    else
      this.myDiagram.model = new go.GraphLinksModel(this.moduleNodes, this.moduleLinks);
  }

  private setCategories(nodes: any[]) {
    for (let node of nodes) {
      if (!this.categoriesPresent.includes(node.category)) {
        this.categoriesPresent.push(node.category);
      }
    }
  }

  private categoryPresent(category: string) {
    return this.categoriesPresent.includes(category);
  }

  private initVis(): void {
    var $ = go.GraphObject.make; // for conciseness in defining templates

    this.myDiagram = $(
      go.Diagram,
      "visualization", // Diagram refers to its DIV HTML element by id
      {
        BackgroundSingleClicked: this.eventBuildTopLevelRelations,
        initialAutoScale: go.Diagram.Uniform,
        layout: $(go.TreeLayout, {angle: 90, nodeSpacing: 120, layerSpacing: 120, layerStyle: go.TreeLayout.LayerUniform}),
        isReadOnly: false, // deny the user permission to alter the diagram or zoom in or out
        isModelReadOnly: false,
        allowZoom: true,
        // allowDelete: false,
        allowInsert: true,
        allowLink: true,
        allowMove: true,
        allowTextEdit: true,
        "grid.visible": false, // display a grid in the background of the diagram
        "grid.gridCellSize": new go.Size(150, 150)
      }
    );

    // create the template for the standard nodes
    this.myDiagram.nodeTemplate = $(
      go.Node,
      new go.Binding("visible", "visible"),
      new go.Binding("fromSpot", "fromSpot"),
      new go.Binding("toSpot", "toSpot"),
      new go.Binding("opacity", "opacity"),
      {
        selectable: true,
        click: this.argumentClick
      },
      "Auto",
      $(
        go.Shape,
        "RoundedRectangle", // the border
        { fill: "white", strokeWidth: 2 },
        new go.Binding("stroke", "isHighlighted", function(h): any {
            return h ?  $(go.Brush, "Radial", { 0.0: "#edbb2f", 1.0: "#ede218" }) : "black";
          }).ofObject(),
        new go.Binding("strokeWidth", "isHighlighted", function(h): number {
            return h ? 10 : 2;
          }).ofObject()
      ),
      $(
        go.Panel,
        "Table",
        { padding: 0.5 },
        $(go.RowColumnDefinition, { column: 1, separatorStroke: "white" }),
        $(go.RowColumnDefinition, { column: 2, separatorStroke: "white" }),
        $(go.RowColumnDefinition, {
          row: 1,
          separatorStroke: "black",
          background: "white",
          coversSeparators: true
        }),
        $(go.RowColumnDefinition, { row: 2, separatorStroke: "white" }),
        $(
          go.TextBlock, // name
          { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "black" },
          new go.Binding("text", "text"),
          
          { row: 0, column: 0, margin: 5, textAlign: "center" }
        ))
    ); // end Node

    // Source Only
    this.myDiagram.nodeTemplateMap.add(
      "additional",
      $(
        go.Node,
        new go.Binding("fromSpot", "fromSpot"),
        new go.Binding("toSpot", "toSpot"),
        new go.Binding("visible", "visible"),
        new go.Binding("opacity", "opacity"),
        {
          selectable: true,
          click: this.highLightAllLinksOut,
          doubleClick: this.prepareEdit
        },
        "Auto",
        $(
          go.Shape,
          "RoundedRectangle", // the border
          { fill: NodeBackgroundColors.ADDITIONAL },
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
                new go.Binding("text", "text")
              ),
              $(go.Panel, "Auto",
                $(go.Shape, "Circle",
                  { fill: this.blue, strokeWidth: 0, stroke: "gray", width: 60, height: 60},
                  new go.Binding("fill", this.blue),
                  ),
                  $(go.Shape,
                  new go.Binding("stroke", "additionalNotes", function (v) {if(!v){return "#0288D1"} return "black"}),
                  {
                    margin: 0, fill: this.blue, stroke:"black", strokeWidth: 1,
                    geometryString: this.additionalNotesIcon
                  }
                ),
              ),
            ),
            {
              toolTip:
              $("ToolTip",
                  { "Border.stroke": this.theme ? "white" : "black", "Border.strokeWidth": 2 },
                  $(go.TextBlock, { margin: 8, stroke: this.theme ? "white" : "black", font: "bold 16px sans-serif"},
                  new go.Binding("text", "description")
                  ))
          },

          $(
            go.Panel,
            "Horizontal",
            $("Button",
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : "black", "Border.strokeWidth": 2 },
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
              ),
              $("Button",
                  new go.Binding("visible", "arguments", function (v) {return v.length > 0}),
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : this.blue, "Border.strokeWidth": 2 },
                          $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Arguments" }))
                  },
                  {   
                      "ButtonBorder.fill": this.red,
                      "ButtonBorder.stroke": "black",
                      "ButtonBorder.strokeWidth": 3,
                      row: 4,
                      column: 2,
                      margin: 5,
                      click: this.expandArguments,
                  },
                  $(go.TextBlock, { margin: 8, stroke: "white", font: "bold 16px sans-serif", text:"Arguments" }),
              ),
              $("Button",
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : this.blue, "Border.strokeWidth": 2 },
                          $(go.TextBlock, { margin: 8, stroke:"black", font: "bold 16px sans-serif", text:"Display Provenance Viewer" }))
                  },
                  {   
                      "ButtonBorder.fill": this.yellow,
                      "ButtonBorder.stroke": "black",
                      "ButtonBorder.strokeWidth": 3,
                      row: 4,
                      column: 3,
                      margin: 5,
                      click: function(v) { alert("This will bring up event provenance from the source documents") },
                  },
                  $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Source" }),
              )
            ),// Vertical Button Panel
            $(
                go.Panel,
                "Vertical",
                 new go.Binding("visible", "expanded", function(h): number {
                    return h;
                  }),
                $(go.Shape, "LineH", {stroke:"black"}),
                $(
                  go.TextBlock,
                  new go.Binding("text", "argumentString"),
                  { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "black", alignment: go.Spot.Center},
                  {
                    textAlign: "center",
                    stroke: this.theme ? "white" : "black" 
                  }
                )
              )// Arguments
          )// top panel,
        )
      ) // end Table Pane
    ); // end Node

    this.myDiagram.nodeTemplateMap.add(
      "detected",
      $(
        go.Node,
        new go.Binding("fromSpot", "fromSpot"),
        new go.Binding("toSpot", "toSpot"),
        new go.Binding("visible", "visible"),
        new go.Binding("opacity", "opacity"),
        {
          selectable: true,
          click: this.highLightAllLinksOut,
          doubleClick: this.prepareEdit
        },
        "Auto",
        $(
          go.Shape,
          "RoundedRectangle", // the border
          { fill: NodeBackgroundColors.DETECTED },
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
                new go.Binding("text", "text")
              ),
              $(go.Panel, "Auto",
                $(go.Shape, "Circle",
                  { fill: NodeBackgroundColors.DETECTED, strokeWidth: 0, stroke: "gray", width: 60, height: 60,  }),
                  $(go.Shape,
                  new go.Binding("stroke", "additionalNotes", function (v) {if(!v){return NodeBackgroundColors.DETECTED} return "black"}),
                    {
                      margin: 0, fill: NodeBackgroundColors.DETECTED, stroke:"black", strokeWidth: 1,
                      geometryString: this.additionalNotesIcon
                    }
                  ),
              ),
            ),
            {
              toolTip:
              $("ToolTip",
                  { "Border.stroke": this.theme ? "white" : "black", "Border.strokeWidth": 2 },
                  $(go.TextBlock, { margin: 8, stroke: this.theme ? "white" : "black", font: "bold 16px sans-serif"},
                  new go.Binding("text", "description")
                  ))
          },
          $(
            go.Panel,
            "Horizontal",
            $("Button",
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : "black", "Border.strokeWidth": 2 },
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
              ),
              $("Button",
                  new go.Binding("visible", "arguments", function (v) {return v.length > 0}),
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : this.blue, "Border.strokeWidth": 2 },
                          $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Arguments" }))
                  },
                  {   
                      "ButtonBorder.fill": this.red,
                      "ButtonBorder.stroke": "black",
                      "ButtonBorder.strokeWidth": 3,
                      row: 4,
                      column: 2,
                      margin: 5,
                      click: this.expandArguments,
                  },
                  $(go.TextBlock, { margin: 8, stroke: "white", font: "bold 16px sans-serif", text:"Arguments" }),
              ),
              $("Button",
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : this.blue, "Border.strokeWidth": 2 },
                          $(go.TextBlock, { margin: 8, stroke:"black", font: "bold 16px sans-serif", text:"Display Provenance Viewer" }))
                  },
                  {   
                      "ButtonBorder.fill": this.yellow,
                      "ButtonBorder.stroke": "black",
                      "ButtonBorder.strokeWidth": 3,
                      row: 4,
                      column: 3,
                      margin: 5,
                      click: function(v) { alert("This will bring up event provenance from the source documents") },
                  },
                  $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Source" }),
              )
            ),// Vertical Button Panel
            $(
                go.Panel,
                "Vertical",
                 new go.Binding("visible", "expanded", function(h): number {
                    return h;
                  }),
                $(go.Shape, "LineH", {stroke:"black"}),
                $(
                  go.TextBlock,
                  new go.Binding("text", "argumentString"),
                  { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "black", alignment: go.Spot.Center},
                  {
                    textAlign: "center",
                    stroke: "white"
                  }
                )
              )// Arguments
          )// top panel
        ),
      )
    ); // end Node

    this.myDiagram.nodeTemplateMap.add(
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
          click: this.highLightAllLinksOut,
          doubleClick: this.prepareEdit
        },
        "Auto",
        $(
          go.Shape,
          "RoundedRectangle", // the border
          { fill: NodeBackgroundColors.PREDICTED },
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
                new go.Binding("text", "text")
              ),
              $(go.Panel, "Auto",
                $(go.Shape, "Circle",
                  { fill: "white", strokeWidth: 0, stroke: "gray", width: 60, height: 60}),
                  $(go.Shape,
                    new go.Binding("stroke", "additionalNotes", function (v) {if(!v){return "white"} return "black"}),
                    {
                      margin: 0, fill: "white", stroke:"black", strokeWidth: 1,
                      geometryString: this.additionalNotesIcon
                    }
                  ),
              ),
            ),
            {
              toolTip:
              $("ToolTip",
                  { "Border.stroke": this.theme ? "white" : "black", "Border.strokeWidth": 2 },
                  $(go.TextBlock, { margin: 8, stroke: this.theme ? "white" : "black", font: "bold 16px sans-serif"},
                  new go.Binding("text", "description")
                  ))
          },
          $(
            go.Panel,
            "Horizontal",
            $("Button",
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : "black", "Border.strokeWidth": 2 },
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
              ),
              $("Button",
                  new go.Binding("visible", "arguments", function (v) {return v.length > 0}),
                  {
                      toolTip:
                      $("ToolTip",
                          { "Border.stroke": this.theme ? "white" : this.blue, "Border.strokeWidth": 2 },
                          $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 16px sans-serif", text:"Arguments" }))
                  },
                  {   
                      "ButtonBorder.fill": this.red,
                      "ButtonBorder.stroke": "black",
                      "ButtonBorder.strokeWidth": 3,
                      row: 4,
                      column: 2,
                      margin: 5,
                      click: this.expandArguments,
                  },
                  $(go.TextBlock, { margin: 8, stroke: "white", font: "bold 16px sans-serif", text:"Arguments" }),
              )
            ),// Vertical Button Panel
            $(
                go.Panel,
                "Vertical",
                new go.Binding("visible", "expanded", function(h): number {
                    return h;
                }),
                $(go.Shape, "LineH", {stroke:"black"}),
                $(
                  go.TextBlock,
                  new go.Binding("text", "argumentString"),
                  { font: "22pt Roboto", textAlign: "center", margin: 5, stroke: "black", alignment: go.Spot.Center},
                  {
                    textAlign: "center",
                    stroke: this.theme ? "white" : "black" 
                  }
                )
              )// Arguments
          )
         )
      )
    ); // end Node

    // create the link template with outlink text
    this.myDiagram.linkTemplateMap.add(
      "outlink",
      $(
        go.Link,
        new go.Binding("zOrder"),
        { toShortLength: 6, toEndSegmentLength: 10, selectable : false },
        { routing: go.Link.AvoidsNodes, corner: 10 },
        $(go.Shape, "RoundedRectangle", { stroke: this.blue, scale: 1, strokeWidth: 5 }),
        $(
          go.Shape, // arrowhead
          { toArrow: "Triangle", stroke: this.blue, scale: 3, fill: this.blue }
        )
      )
    );

    this.myDiagram.linkTemplateMap.add(
      "noarrow",
      $(
        go.Link,
        new go.Binding("zOrder"),
        { toShortLength: 0, toEndSegmentLength: 10, selectable : false},
        { routing: go.Link.AvoidsNodes, corner: 3},
        $(go.Shape, 
            "RoundedRectangle", 
            { scale: 1, strokeWidth: 5},
            new go.Binding("stroke", "highLighted", function (v) {return v ? "#F90917" : "#0288D1"})
        )
      )
    );
    
    // Grouping
    this.myDiagram.groupTemplate = $(
      go.Group,
      "Auto",
      new go.Binding("visible", "visible"),
      {
        click: this.groupClick,
      },
      { layout: $(go.LayeredDigraphLayout, { direction: 0, columnSpacing: 75, layerSpacing: 150 }) },
      $(
        go.Shape,
        "RoundedRectangle", // surrounds everything
        { parameter1: 10, fill: "transparent", stroke: this.theme ? "white" : "black" }
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
              stroke: this.theme ? "white" : "black" ,
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
              stroke: this.theme ? "white" : "black" 
            }
          )
        ),
        $(
          go.Placeholder, // represents area for all member parts
          { padding: new go.Margin(15, 15), background: "transparent" }
        )
      )
    );

    this.myDiagram.groupTemplateMap.add(
      "orphanage",
      $(
      go.Group,
      "Auto",
      new go.Binding("visible", "visible"),
      {
        click: this.groupClick,
      },
      { layout: $(go.LayeredDigraphLayout, { direction: 90, columnSpacing: 75, layerSpacing: 150 }) },
      $(
        go.Shape,
        "RoundedRectangle", // surrounds everything
        { parameter1: 10, fill: "transparent", stroke: this.theme ? "white" : "black" }
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
              stroke: this.theme ? "white" : "black" ,
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
              stroke: this.theme ? "white" : "black" 
            }
          )
        ),
        $(
          go.Placeholder, // represents area for all member parts
          { padding: new go.Margin(15, 15), background: "transparent" }
        )
      )
    )
    )

    this.myDiagram.model = new go.GraphLinksModel(this.moduleNodes, this.moduleLinks);

    var overview = $(go.Overview, "overviewVis", {
      observed: this.myDiagram,
      contentAlignment: go.Spot.Center
    });

  }

  textStyle(): any {
    return { font: "44pt Roboto", textAlign: "center", margin: 5, stroke: "black" };
  }

  scaleWidth(num: any) {
    return num * this.myDiagram._widthFactor;
  }

  linkColorConverter(linkdata: any, elt: any) {
    var link = elt.part;
    if (!link) return this.blue;
    var f = link.fromNode;
    if (!f || !f.data || !f.data.critical) return this.blue;
    var t = link.toNode;
    if (!t || !t.data || !t.data.critical) return this.blue;
    return this.teal; // when both Link.fromNode.data.critical and Link.toNode.data.critical
  }

  private groupData(): void {
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
  }

  updateEvent(e: any, obj: any): void {
     let task = obj.part.data;
     this.selectedEvent = task;
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    // this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }


  private getBackgroundColor(category: string) {
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
  }

  private getRootNode(): void {
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
  }

  searchDiagram(input: string) {
      this.myDiagram.startTransaction("highlight-search");
      this.myDiagram.clearHighlighteds();
      this.selectedArgs = [];

      if (input && input.trim().length > 0) {
      // search four different data properties for the string, any of which may match for success
      // create a case insensitive RegExp from what the user typed
      var safe = input.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
      var regex = new RegExp(safe, "i");
      var results = this.myDiagram.findNodesByExample(
          { key: regex },
          { description: regex },
          { text: regex },
          { argumentString: regex}
      );
      while (results.next()) {
          if (!results.value.key.includes(this.argumentSuffix)) {
            results.value.isHighlighted = true;
          }
      }

      // try to center the diagram at the first node that was found
      if (results.count > 0) this.myDiagram.centerRect(results.first().actualBounds);
      }
      this.myDiagram.commitTransaction("highlight-search");
  }

  private expandChildren(e: any, obj: any):void {
    this.updateEvent(e,obj);
    this.myDiagram.startTransaction('update');
   
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
                this.myDiagram.model.assignAllDataProperties(this.moduleNodes[objIndex],this.moduleNodes[objIndex]);
                if(this.moduleNodes[objIndex].subgroupEvents.length > 0) {
                    for (let subGroup of this.moduleNodes[objIndex].subgroupEvents) {
                        objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key == subGroup["@id"]);
                        if (objIndex >= 0)
                            this.moduleNodes[objIndex].visible = !this.moduleNodes[objIndex].visible;
                            this.myDiagram.model.assignAllDataProperties(this.moduleNodes[objIndex],this.moduleNodes[objIndex]);
                    }
                }
            }
        }
    }
    this.myDiagram.updateAllTargetBindings();
    this.myDiagram.commitTransaction('update');
  }

  private expandArguments(e: any, obj: any):void {
    this.myDiagram.startTransaction('argument');
    let task = obj.part.data;
    
    var id = task.key;// + this.argumentSuffix;
    this.filterLinks([id]);
    var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === id);
    if (objIndex >= 0) {
      // this.moduleNodes[objIndex].expanded = !this.moduleNodes[objIndex].expanded;
      this.myDiagram.model.setDataProperty(this.moduleNodes[objIndex],"expanded", !this.moduleNodes[objIndex].expanded);
    }
    this.myDiagram.updateAllTargetBindings();
    this.myDiagram.commitTransaction('argument');
  }

  private hideChildren(index: number): void {
    this.moduleNodes[index].visible = false;
    this.myDiagram.model.assignAllDataProperties(this.moduleNodes[index],this.moduleNodes[index]);
    var results =  [];
    if (this.moduleNodes[index].key.includes(this.groupSuffix)) {
        for (let group of this.moduleNodes[index].subgroupEvents) {
            results.push(group["@id"]);
        }
    } else {
        var filtered = this.moduleLinks.filter((x: {from: string}) => x.from === this.moduleNodes[index].key);
        for (let link of filtered) {
            results.push(link.to);
        }
    }
    
    for (let result of results) {
        var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === result);
        if (objIndex >= 0) 
            this.hideChildren(objIndex);
    }
  }

  filterConfidence(input: any) {
      this.myDiagram.startTransaction("confidence-filter");
      var lower = input[0];
      var upper = input[1];
      for (var i = 0; i < this.moduleNodes.length; i++) {
          if ((this.moduleNodes[i].confidence > upper || this.moduleNodes[i].confidence < lower) && !this.moduleNodes[i].key.includes(this.argumentSuffix)) {
              this.moduleNodes[i].opacity = .3;
          } else {
              this.moduleNodes[i].opacity = 1;
          }
          this.myDiagram.model.assignAllDataProperties(this.moduleNodes[i],this.moduleNodes[i]);
      }
      this.myDiagram.updateAllTargetBindings();
      this.myDiagram.commitTransaction("confidence-filter");
  }

  groupClick(e: any, obj: any): void {
      this.myDiagram.startTransaction();
      let task = obj.part.data;
      this.filterLinks([task.key]);
      this.myDiagram.commitTransaction();
      this.updateEvent(e,obj);
      this.updateRelations(e,obj);
      if (task.key === this.rootNode.key) {
        this.buildTopLevelRelations(false);
      }
      
  }

  argumentClick(e: any, obj: any): void {
      this.myDiagram.startTransaction();
      let task = obj.part.data;
      this.filterLinks([task.key]);
      this.myDiagram.commitTransaction();
  }

  highLightAllLinksOut(e: any, obj: any): void {
      this.updateEvent(e,obj);
      this.updateRelations(e,obj);
      this.myDiagram.startTransaction();
      let task = obj.part.data;
      var arrayExt = [];
      arrayExt.push(task.key + this.argumentSuffix);
      arrayExt.push(task.key + this.groupSuffix);
        this.filterLinks(arrayExt);
      this.myDiagram.commitTransaction();
  }

  filterLinks(ModelKey: Array<String>): void {
      for (let i = 0; i < this.moduleLinks.length; i++) {
          if (ModelKey.includes(this.moduleLinks[i].to)) {
              this.myDiagram.model.setDataProperty(this.moduleLinks[i], "highLighted", true);
              this.myDiagram.model.setDataProperty(this.moduleLinks[i], "zOrder", 100);
          }
          else {
              this.myDiagram.model.setDataProperty(this.moduleLinks[i], "highLighted", false);
              this.myDiagram.model.setDataProperty(this.moduleLinks[i], "zOrder", 1);
          }
      }
  }

  private expandAll(): void {
      this.myDiagram.startTransaction("ExpandAll");
      for (var i = 0; i < this.moduleNodes.length; i++) {
        this.moduleNodes[i].visible = true;
        if (this.moduleNodes[i].arguments.length > 0) {
          this.myDiagram.model.assignAllDataProperties(this.moduleNodes[i],this.moduleNodes[i]);
          this.myDiagram.model.setDataProperty(this.moduleNodes[i],"expanded", true);
        }
      }
      this.myDiagram.updateAllTargetBindings();
      this.myDiagram.commitTransaction("ExpandAll");
  }

  private collapseAll(): void {
      this.myDiagram.startTransaction("CollapseAll");
      for (var i = 0; i < this.moduleNodes.length; i++) {
          if (this.moduleNodes[i].key != this.rootNode.key && this.moduleNodes[i].parent != this.rootNode.key &&  this.moduleNodes[i].parent != this.rootNode.key + this.groupSuffix) {
              this.moduleNodes[i].visible = false;
              this.myDiagram.model.assignAllDataProperties(this.moduleNodes[i],this.moduleNodes[i]);
          }
          this.myDiagram.model.setDataProperty(this.moduleNodes[i],"expanded", false);
      }
      this.myDiagram.updateAllTargetBindings();
      this.myDiagram.commitTransaction("CollapseAll");
  }

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
        nodes.push({
              id: nextNodeID++,
              label: relation[ParserCommons.getKairosReference("relationSubject")][0]["@id"],
              shape: "box",
              font: { size: 50 },
        });
        edges.push({
          id: nextNodeID,
          label: "",
          from: nextNodeID -1,
          to: nextNodeID,
          arrows: "to"
        });
        nodes.push({
              id: nextNodeID++,
              label: relation[ParserCommons.getKairosReference("wd_label")][0]["@list"][0]["@value"],
              shape: "box",
              font: { size: 50 },
        });
        edges.push({
          id: nextNodeID,
          label: "",
          from: nextNodeID -1,
          to: nextNodeID,
          arrows: "to"
        });
        nodes.push({
          id: nextNodeID++,
          label: relation[ParserCommons.getKairosReference("relationObject")][0]["@id"],
          shape: "box",
          font: { size: 50 },
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
    Vis.reloadVisualization(nodes, edges);
  }

  private eventBuildTopLevelRelations() {
    this.buildTopLevelRelations(false);
  }

  private buildTopLevelRelations(firstLoad: boolean) {
    this.selectedEvent = this.resetNodeModule();
    this.getRootNode();
    var parentRelations: any = {
      part: {
        data: this.selectedEvent
      }
    }
    if (this.framedRaw[ParserCommons.getKairosReference("relations")]) {
      for (var relation of this.framedRaw[ParserCommons.getKairosReference("relations")]) {
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
  }

   private createArgumentNodes() {
    this.participantArgs = [];
    for (let task of this.moduleNodes) {
      if (task.arguments && task.arguments.length > 0) {
        // For each argument that exists, create a node
        task.arguments.forEach((singleArg: any) => {
         
          var argExists = false;
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
  }

    // Filters Event Complex viusalization when items are selected
  filterArguments(): void {
    this.myDiagram.startTransaction("highlight-filter");
    // Remove all highlights
    this.myDiagram.clearHighlighteds();
    this.search = "";

    this.moduleNodes.forEach((event: any) => {
      let eventArgs: Array<number> = [];
      // If an event has arguments, filter
      if (event.arguments && event.arguments.length > 0) {
        event.arguments.forEach((argument: any) => {
          this.participantArgs.forEach((arg, argIndex) => {
            if (arg.id === argument.id) {
              // Add the index of the arguments found in the selected event to an array (this index value cooresponds with the "selectedArgs" values)
              eventArgs.push(argIndex);
            }
          });
        });
        // if the event contains at least one of the selected args, display it. Otherwise hide it
        const found = this.selectedArgs.some(r => eventArgs.indexOf(r) >= 0);
        if (found) {
          var results = this.myDiagram.findNodesByExample({ key: event.key });
          while (results.next()) {
            results.value.isHighlighted = true;
          }
        }
      }
    });
    this.myDiagram.commitTransaction("highlight-filter");
  }

  private onFileUpload(): void {
      if (this.files != null && this.files.length > 0) {
        this.loading = true;
        var file = this.files[0];
        this.reader.readAsText(file);
        this.reader.onload = res => {
          if (this.reader.result) {
            let data: any = JSON.parse(this.reader.result.toString());
            this.getTransformedData(data);
          }
        };

      }
    }



  prepareEdit(e: any, obj: any): void {
    // this.selectedEvent = obj.part.data;
    // this.additionalNotes = obj.part.data.additionalNotes;
    // this.editModal = true;
  }

  // saveData(): void {
  //   var objIndex = this.moduleNodes.findIndex((obj: { key: any }) => obj.key === this.selectedEvent.key);
  //   if (objIndex >= 0) {
  //     this.myDiagram.model.setDataProperty(this.moduleNodes[objIndex],"additionalNotes", this.additionalNotes);
  //     this.sendSaveToServer();
  //   }
  //   this.editModal = false;
  // }

  // private sendSaveToServer(): void {
  //   var userName = this.$store.getters["user/currentUser"].username;
  //   let request: SaveOrUpdateRequest = {
  //     namedGraph: this.namedGraph,
  //     eventKey: this.selectedEvent.key,
  //     additionalInfo: this.additionalNotes.replace(/\n/g,'\\\\n'),
  //     userName: userName
  //   };
  //   this.$store
  //     .dispatch("visualization/saveOrUpdateNode", request)
  //     .then(() => {
  //       console.log("saving");
  //     })
  //     .catch(errorStatus => {
  //       this.handleErrorStatus(errorStatus);
  //     });
  // }

}
</script>

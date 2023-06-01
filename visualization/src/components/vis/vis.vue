<template>
  <div id="relationVis" style="width:100%;height:100%" ></div>
</template>

<script lang="ts">
import "../../assets/visStyle.css";
import { defineComponent } from "vue";
import * as go from "gojs";

const $ = go.GraphObject.make;  // for conciseness in defining templates

export default defineComponent({
  name: "Vis",
  components: {},
  props: {},
  data() {
    return {
      nodes: <Array<any>>[],
      edges: <Array<any>>[]
    };
  },
  mounted() {
    this.$options.relationDiagram = <go.Diagram>{};
    this.initVis(this.nodes, this.edges);
  },
  methods: {
    initVis(nodes: Array<any>, edges: Array<any>): void {
      if (!this.$options.relationDiagram.div)
        this.loadVisualization(nodes, edges);
      else
        this.$options.relationDiagram.model = new go.GraphLinksModel(nodes, edges);
    },
    loadVisualization(nodes: Array<any>, edges: Array<any>): void {      
      this.$options.relationDiagram = $(
        go.Diagram,
        "relationVis", // Diagram refers to its DIV HTML element by id
        {
          initialAutoScale: go.Diagram.CycleSourceTree,
          layout: $(go.LayeredDigraphLayout, {direction: 90, columnSpacing: 0, layerSpacing: 0}),
          isReadOnly: false, // deny the user permission to alter the diagram or zoom in or out
          isModelReadOnly: false,
          allowZoom: true,
          allowInsert: false,
          allowLink: true,
          allowMove: true,
          allowTextEdit: false,
          "grid.visible": false, // display a grid in the background of the diagram
          "grid.gridCellSize": new go.Size(150, 150)
        }
      );
  
      this.$options.relationDiagram.nodeTemplate = 
        $(go.Node, 
          "Auto",
          {
            selectable: true,
          },
          $(
            go.Shape, 
            { strokeWidth: 1, figure: "Ellipse"},
            new go.Binding("fill", "background"),
            new go.Binding("stroke", "border")
          ),
          $(go.TextBlock,
              { margin: 5 },
              { row: 0, column: 0, margin: 5, textAlign: "center" },
              new go.Binding("text", "label")
              // 
          ),
          {
            toolTip:
            $("ToolTip",
                { "Border.stroke": "black", "Border.strokeWidth": 2 },
                $(go.TextBlock, { margin: 8, stroke: "black", font: "bold 12px sans-serif"},
                new go.Binding("text", "title")
                ))
          },
      );
  
      this.$options.relationDiagram.linkTemplate =
        $(
          go.Link,
          { toShortLength: 6, toEndSegmentLength: 10, selectable : false },
          { routing: go.Link.AvoidsNodes, corner: 10 },
          $(go.Shape, "RoundedRectangle", { stroke: "black", scale: 1, strokeWidth: 2 }),
          $(
            go.Shape, // arrowhead
            { toArrow: "Triangle", stroke: "black", scale: 1, fill: "black" }
          )
        );
  
        // Grouping
      this.$options.relationDiagram.groupTemplate = $(
        go.Group,
        "Auto",
        { layout: $(go.LayeredDigraphLayout, { direction: 90, columnSpacing: 10, layerSpacing: 10 }) },
        $(
          go.Shape,
          "RoundedRectangle", // surrounds everything
          { parameter1: 10, fill: "transparent", stroke: "white" }
        ),
        $(
          go.Panel,
          "Vertical", // position header above the subgraph
          { defaultAlignment: go.Spot.Left },
          $(
            go.Placeholder, // represents area for all member parts
            { padding: new go.Margin(15, 15), background: "transparent" }
          )
        )
      );
  
      this.$options.relationDiagram.model = new go.GraphLinksModel(nodes, edges);
    },
    setRelations(nodes: Array<any>, edges: Array<any>): void {
      this.$options.relationDiagram.model = new go.GraphLinksModel(nodes, edges);
    }
  }
});
</script>

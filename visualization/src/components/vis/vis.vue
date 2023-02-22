<template>
  <div id="vis"></div>
</template>

<script lang="ts">
import { Network, DataSet } from "vis-network/standalone";
import "../../assets/visStyle.css";
import { Component, Vue, Watch, Prop } from "vue-property-decorator";

export default {
  name: "vis",

  reloadVisualization: function(nodes: Array<any>, edges: Array<any>) {
    let options = {
      width: "100%",
      height: "100%",
      edges: {
        width: 5,
        color: "#0288D1"
      },
      layout: {
        improvedLayout: true,
        hierarchical: {
          enabled: true,
          // levelSeparation: 250,
          // nodeSpacing: 100,
          // treeSpacing: 300,
          // blockShifting: true,
          // edgeMinimization: false,
          // parentCentralization: true,
          direction: "UD", // UD, DU, LR, RL
          sortMethod: "directed", // hubsize, directed
          shakeTowards: "leaves" // roots, leaves
        }
      },
      interaction: {
        navigationButtons: true
      },
      physics: {
        hierarchicalRepulsion: {
              avoidOverlap: 0.99,
              // springConstant: 0.25,
              // springLength: 250
          }
      }
    };

    // Building Vis.js window
    let container: HTMLElement = document.getElementById("vis")!;
    
    var data = {
      nodes: nodes,
      edges: edges
    };
    var network = new Network(container, data, options);
    network.on("stabilizationIterationsDone", function() {
      network.setOptions({ physics: false });
    });

    if (nodes.length > 0) {
      var focusOptions = {
        locked: false,
        scale: 1.0
      }
      network.focus(1,focusOptions);

      var moveToOptions = {
        position: {x:0, y:0}
      }
      // network.moveTo(moveToOptions);

      // Fit certain nodes into view
      var fitOptions = {
        minZoomLevel: 25,
        maxZoomLevel: 50,
        animation: false
      }
      // network.fit(fitOptions);
    }
  }
};
</script>

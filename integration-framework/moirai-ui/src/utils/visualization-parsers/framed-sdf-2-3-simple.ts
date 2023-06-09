import { ModelTemplate } from "../../interfaces/vis-node-template";
import { ParsedData } from "../../interfaces/vis-parser-data-object";
import { LinkTemplate } from "../../interfaces/vis-link-template";
import { ParserCommons } from "./parser-commons";
import * as go from "gojs";

export class FramedSDF21Simple {
  static rootNode: ModelTemplate;
  private static groupSuffix = "-caci-group";

    static parse(jsonData: any): any {
      var runtime = true;
        let parsedData: ParsedData = {
          moduleLinks: [],
          moduleNodes: [],
          framedRaw: {}
        };
    
        let link: LinkTemplate = {
          to: "",
          from: "",
          text: "",
          category: "",
          highLighted: false
        };

        parsedData.framedRaw = jsonData[0][ParserCommons.getKairosReference("instances")][0];

        for (let data of parsedData.framedRaw[ParserCommons.getKairosReference("events")][0]["@list"]) {
            var genericEventIncrement = 1;
            let model: ModelTemplate = this.resetModel();

            // Skip records with no ID
            if(!data["@id"])
              continue;

            model.category = this.getCategory(data);
            model.key = data["@id"];
            var repeatable = data[ParserCommons.getKairosReference("repeatable")] ? data[ParserCommons.getKairosReference("repeatable")][0]["@value"] : false as boolean;
            model.text = data["http://schema.org/name"]
              ? data["http://schema.org/name"][0]["@value"] + (repeatable ? " *" : "")
              : "Event " + genericEventIncrement++;
            model.description = data["http://schema.org/description"]
              ? data["http://schema.org/description"][0]["@value"]
              : "<none>";
            model.confidence = data[ParserCommons.getKairosReference("confidence")]
              ? this.checkConfidence(data[ParserCommons.getKairosReference("confidence")][0]["@list"][0]["@value"])
              : 0;
            model.startTime = data[ParserCommons.getKairosReference("startTime")]
              ? data[ParserCommons.getKairosReference("startTime")][0]["@value"]
              : "";
            model.comment = data[ParserCommons.getKairosReference("comment")]
              ? data[ParserCommons.getKairosReference("comment")][0]["@list"]
              : "";
            model.qnode.name = data[ParserCommons.getKairosReference("qnode")]
              ? data[ParserCommons.getKairosReference("qnode")][0]["@list"]
              : [];
            model.qnode.label = data[ParserCommons.getKairosReference("qlabel")]
              ? data[ParserCommons.getKairosReference("qlabel")][0]["@list"]
              : [];
            model.endTime = data[ParserCommons.getKairosReference("endTime")]
              ? data[ParserCommons.getKairosReference("endTime")][0]["@value"]
              : "";
            model.entityId = data[ParserCommons.getKairosReference("entity")]
              ? data[ParserCommons.getKairosReference("entity")][0]["@id"]
              : "";
            model.ta1Explanation =  data[ParserCommons.getKairosReference("ta1explanation")]
            ? data[ParserCommons.getKairosReference("ta1explanation")][0]["@value"]
            : "";
            model.relations = data[ParserCommons.getKairosReference("relations")]
            ? data[ParserCommons.getKairosReference("relations")]
            : [];
            model.parent = data[ParserCommons.getKairosReference("parent")]
            ? data[ParserCommons.getKairosReference("parent")][0]["@id"]
            : "";
            model.participants =  data[ParserCommons.getKairosReference("participants")]
            ? data[ParserCommons.getKairosReference("participants")][0]["@list"]
            : [];
            model.additionalNotes = data[ParserCommons.getKairosReference("additionalNotes")] ?
            data[ParserCommons.getKairosReference("additionalNotes")][0]["@value"] : "";
            // Get Relation reference names
            model = ParserCommons.getRelationReference(model, parsedData.framedRaw);

            model.toSpot =  go.Spot.Top;
            model.fromSpot =  go.Spot.Bottom;
            
            model.subgroupEvents = data[ParserCommons.getKairosReference("subgroup_events")]
            ? data[ParserCommons.getKairosReference("subgroup_events")][0]["@list"]
            : [];
            
            
            model.childrenGate = getChildrenGate(data[ParserCommons.getKairosReference("children_gate")])

            if (runtime && data[ParserCommons.getKairosReference("isTopLevel")] && data[ParserCommons.getKairosReference("isTopLevel")][0]["@value"]) {
              runtime = false;
              this.rootNode = model;
              model.childrenGate = "";
            }
            
            if (data[ParserCommons.getKairosReference("outlinks")]) {
              for (let outlink of data[ParserCommons.getKairosReference("outlinks")][0]["@list"]) {
                link = ParserCommons.resetLink();
                link.from = model.key;
                link.to = outlink["@id"];
                link.category = "outlink";
                parsedData.moduleLinks.push(link);
              }
            }

            // Create copy and de-reference children from original
            if (model.subgroupEvents.length > 0 && this.rootNode.key !== model.key) {
                var copy =this.createDeepCopy(model);
                parsedData.moduleNodes.push(copy);

                link = ParserCommons.resetLink();
                link.from = model.key;
                link.to = copy.key;
                link.category = "noarrow";
                parsedData.moduleLinks.push(link);
               
                model.subgroupEvents = [];
                model.isParent = true;
            }
            

          // Creates a 'floating' node for every event
          var argumentNode = this.createArgumentNode(model, parsedData.framedRaw);
          if (argumentNode) {
            model.argumentString = argumentNode.text;
            model.arguments = argumentNode.arguments;
          }

          if (this.rootNode.key !== model.key) {
            parsedData.moduleNodes.push(model);
          } else {
            model.isGroup = true;
            model.visible = true;
            parsedData.moduleNodes.push(model);
          }
        }
        // Let the top level (under root) be visible
        for (let i =0; i < parsedData.moduleNodes.length; i++) {
          if (parsedData.moduleNodes[i].parent === this.rootNode.key) {
            parsedData.moduleNodes[i].visible = true;
          }
          if (parsedData.moduleNodes[i].childrenGate != "" && parsedData.moduleNodes[i].key.includes(this.groupSuffix)) {
            parsedData.moduleNodes[i].childrenGate = this.getChildGate(parsedData.moduleNodes[i], parsedData.moduleNodes);
          }
          // update groups
          if(parsedData.moduleNodes[i].isGroup)
            parsedData.moduleNodes[i].category = this.checkForOrphanedNodes(parsedData.moduleNodes[i], parsedData);
        }
        return parsedData;
    }


  static checkConfidence(arg0: any): number {
    if (arg0 < 0 || arg0 > 1)
      return 0;
    else 
      return arg0
  }


    static getChildGate(model: ModelTemplate, modelList: Array<ModelTemplate>): String {
      if  (model.subgroupEvents.length > 0) {
        for (let subList of model.subgroupEvents) {
          for (let child of modelList) {
            if (subList["@id"] === child.key && child.category !== "predicted") {
              return "";
            }
          }
        }
      }
      return model.childrenGate;
    }

    static createArgumentNode(model: any, raw: any): any {
      var argumentNode = this.resetModel();
      var entityList = [] as Array<any>;
      var argumentsList = [];
      var stringResult = "";
      for (let participant of model.participants) {
        entityList= [];
        if (participant[ParserCommons.getKairosReference("values")]) {
              for (let entry of participant[ParserCommons.getKairosReference("values")]) {
                if (entry[ParserCommons.getKairosReference("ta2entity")] && entry[ParserCommons.getKairosReference("ta2entity")][0]["@id"].includes("/Entities/")) {
                  for (let entity of raw[ParserCommons.getKairosReference("entities")]) {
                    if ( entity["@id"] === entry[ParserCommons.getKairosReference("ta2entity")][0]["@id"] && entity["http://schema.org/name"]) {
                      entityList.push(entity["http://schema.org/name"][0]["@value"]);
                      argumentsList.push({id: entity["@id"], name: entity["http://schema.org/name"][0]["@value"]});
                    }
                  } 
                } else {
                  for (let data of raw[ParserCommons.getKairosReference("events")][0]["@list"]) { 
                    if (entry[ParserCommons.getKairosReference("ta2entity")] && data["@id"] === entry[ParserCommons.getKairosReference("ta2entity")][0]["@id"] && data["http://schema.org/name"]) {
                      entityList.push(data["http://schema.org/name"][0]["@value"]);
                      argumentsList.push({id: data["@id"], name: data["http://schema.org/name"][0]["@value"]});
                    }
                  }
                } 
              }
              if (entityList.length > 0)
                stringResult += entityList.join(", ") + "\n";
        }
      }
      if (stringResult !== "") {
        argumentNode.text = stringResult.replace(/\n$/, "");
        argumentNode.arguments = argumentsList;
        // argumentNode.group = model.parent.key === this.rootNode.key ? model.parent : model.parent + this.groupSuffix;
        return argumentNode;
      }
      return null;
    }

    static getCategory(node: any): string {
        if (
          node[ParserCommons.getKairosReference("ta1ref")] &&
          node[ParserCommons.getKairosReference("ta1ref")][0]["@value"] === "none"
        ) {
          return "additional";
        } else if (node[ParserCommons.getKairosReference("provenance")]) {
          return "detected";
        }
    
        return "predicted";
      }

    static resetModel(): ModelTemplate{
      return  {
        parent:"",
        key: "",
        text: "",
        group: "",
        isTreeExpanded: true,
        visible: false,
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
        status: true,
        arguments: [],
        argumentString: "",
        qnode: {
          label: [],
          name: [],
          linkedQNodeId: "",
          linkedQLabelId: ""
        },
        comment: [],
        ta1Explanation: "",
        relations: [],
        childrenGate: "",
        isParent: false,
        toSpot: go.Spot.Top,
        fromSpot: go.Spot.Bottom,
        layer: 0,
        opacity: 1,
        expanded: false,
        additionalNotes: ""
      };
    }

    static createDeepCopy(node: any): any {
      let model = this.resetModel();
      model.key = node.key + this.groupSuffix;
      model.subgroupEvents = node.subgroupEvents;
      model.isGroup = true;
      model.childrenGate = node.childrenGate;
      model.text = node.text;
      model.description = node.description;
      model.visible = false;
      model.relations = node.relations;
      return model;
    }

    static checkForOrphanedNodes(node: any, parsedData: any): any {
      var allOrhpaned = false;
      for (let event of node.subgroupEvents) {
        var objIndex = parsedData.moduleLinks.findIndex((obj: { from: any, to: any }) => obj.from == event["@id"] && !obj.to.includes(this.groupSuffix));
        if (objIndex >= 0) {
          // set to false and just stop looping
          allOrhpaned = false;
          break;
        } else {
          allOrhpaned = true;
        }
      }
      return allOrhpaned ? "orphanage" : "";
    }
    
}

function getChildrenGate(child: any): string {
  var validArray= ["AND", "OR", "XOR"];
  var value = child ?  child[0]["@value"].toUpperCase()  : "";
  if (!validArray.includes(value))
    return "";
  return "(" + value + ")";
}

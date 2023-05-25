import type { ModelTemplate } from "../../interfaces/model-template";
import type { ParsedData } from "../../interfaces/parsed-data";
import type { LinkTemplate } from "../../interfaces/link-template";
import { ParserCommons } from "./parser-commons";
import * as go from "gojs";

export class FramedSDF21Simple {
  static rootNode: ModelTemplate;
  private static groupSuffix = "-caci-group";

    static parse(jsonData: any): any {
        var map;
        let runtime = true;
        const parsedData: ParsedData = {
          moduleLinks: [],
          moduleNodes: [],
          framedRaw: {},
          participantsAcrossEvents: new Map<String,Array<String>>(),
          participantsAcrossRelations: new Map
        };
    
        let link: LinkTemplate = {
          to: "",
          from: "",
          text: "",
          category: "",
          highLighted: false
        };

        parsedData.framedRaw = jsonData[0][ParserCommons.getKairosReference("instances")][0];

        for (const data of parsedData.framedRaw[ParserCommons.getKairosReference("events")][0]["@list"]) {
            let genericEventIncrement = 1;
            let model: ModelTemplate = this.resetModel();

            // Skip records with no ID
            if(!data["@id"])
              continue;

            model.category = this.getCategory(data);
            model.categoryStatus = model.category;
            model.key = data["@id"];
            const repeatable = data[ParserCommons.getKairosReference("repeatable")] ? data[ParserCommons.getKairosReference("repeatable")][0]["@value"] : false as boolean;
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
              // ta2wd_node first then wd_node else none
            model.qnode.name = data[ParserCommons.getKairosReference("ta2wd_node")]
              ? data[ParserCommons.getKairosReference("ta2wd_node")][0]["@list"]
              : null;
            if (!model.qnode.name) {
              model.qnode.name = data[ParserCommons.getKairosReference("wd_node")]
              ? data[ParserCommons.getKairosReference("wd_node")][0]["@list"]
              : [{"@id":"None", "@value": "None"}];
            }
            model.qnode.label = data[ParserCommons.getKairosReference("ta2wd_label")]
              ? data[ParserCommons.getKairosReference("ta2wd_label")][0]["@list"]
              : null;
            if (!model.qnode.label) {
              model.qnode.label = data[ParserCommons.getKairosReference("wd_label")]
              ? data[ParserCommons.getKairosReference("wd_label")][0]["@list"]
              : [{"@id":"None", "@value": "None"}];
            }
            model.qnode.description = data[ParserCommons.getKairosReference("ta2wd_description")]
              ? data[ParserCommons.getKairosReference("ta2wd_description")][0]["@list"]
              : null;
            if (!model.qnode.description) {
              model.qnode.description = data[ParserCommons.getKairosReference("wd_description")]
              ? data[ParserCommons.getKairosReference("wd_description")][0]["@list"]
              : [{"@id":"None", "@value": "None"}];
            }
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
            model.repeatable = data[ParserCommons.getKairosReference("repeatable")] ?
            data[ParserCommons.getKairosReference("repeatable")][0]["@value"] : false;
            model.origName = data[ParserCommons.getKairosReference("origName")] ?
            data[ParserCommons.getKairosReference("origName")][0]["@value"] : "";
            model.origDescription = data[ParserCommons.getKairosReference("origDescription")] ?
            data[ParserCommons.getKairosReference("origDescription")][0]["@value"] : "";
            // Get Relation reference names
            model = ParserCommons.getRelationReference(model, parsedData.framedRaw);

            model.toSpot =  go.Spot.Top;
            model.fromSpot =  go.Spot.Bottom;
            
            model.subgroupEvents = data[ParserCommons.getKairosReference("subgroup_events")]
            ? data[ParserCommons.getKairosReference("subgroup_events")][0]["@list"]
            : [];
            model.childrenList = model.subgroupEvents;
            
            model.childrenGate = getChildrenGate(data[ParserCommons.getKairosReference("children_gate")])

            if (runtime && data[ParserCommons.getKairosReference("isTopLevel")] && data[ParserCommons.getKairosReference("isTopLevel")][0]["@value"]) {
              runtime = false;
              this.rootNode = model;
              model.childrenGate = "";
            }
            
            if (data[ParserCommons.getKairosReference("outlinks")]) {
              for (const outlink of data[ParserCommons.getKairosReference("outlinks")][0]["@list"]) {
                link = ParserCommons.resetLink();
                link.from = model.key;
                link.to = outlink["@id"];
                link.category = "outlink";
                parsedData.moduleLinks.push(link);
              }
            }

          // Creates a 'floating' node for every event
          const argumentNode = this.createArgumentNode(model, parsedData.framedRaw);
          if (argumentNode) {
            model.argumentString = argumentNode.text;
            model.arguments = argumentNode.arguments;
            model.customTable = argumentNode.customTable;
            for (let argument of model.arguments) {
              if (!parsedData.participantsAcrossEvents.get(argument.id)){
                parsedData.participantsAcrossEvents.set(argument.id,[model.text]);
              } else {
                var results = parsedData.participantsAcrossEvents.get(argument.id);
                results.push(model.text);
                parsedData.participantsAcrossEvents.set(argument.id,results);
              }
            }
          }

          // Create copy and de-reference children from original
          if (model.subgroupEvents.length > 0 && (!this.rootNode || this.rootNode.key !== model.key)) {
            const copy =this.createDeepCopy(model);
            parsedData.moduleNodes.push(copy);

            link = ParserCommons.resetLink();
            link.from = model.key;
            link.to = copy.key;
            link.category = "noarrow";
            parsedData.moduleLinks.push(link);
           
            model.subgroupEvents = [];
            model.isParent = true;
        }

          if (!this.rootNode || this.rootNode.key !== model.key) {
            parsedData.moduleNodes.push(model);
          } else {
            model.isGroup = true;
            model.visible = true;
            parsedData.moduleNodes.push(model);
          }
        }
        // Let the top level (under root) be visible
        for (let i =0; i < parsedData.moduleNodes.length; i++) {
          if (parsedData.moduleNodes[i].parent === this.rootNode.key && !parsedData.moduleNodes[i].key.includes(this.groupSuffix)) {
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
        for (const subList of model.subgroupEvents) {
          for (const child of modelList) {
            if (subList["@id"] === child.key && child.category !== "predicted") {
              return "";
            }
          }
        }
      }
      return model.childrenGate;
    }

    static createArgumentNode(model: any, raw: any): any {
      const argumentNode = this.resetModel();
      let entityList = [] as Array<any>;
      const argumentsList = [] as Array<any>;
      let stringResult = "";
      let customList = [] as Array<any>;
      let index = 1;
      let keyIndex = 0;
      let entityLimitCounter = 0;
      const entityLimit = 20;
      const argumentLengthLimit = 25;

      for (const participant of model.participants) {
        let customTableElement = {
          roleName: "" as String,
          lookup: "" as String,
          values: [] as Array<any>
        }
        customTableElement.roleName = participant[ParserCommons.getKairosReference("roleName")] ? participant[ParserCommons.getKairosReference("roleName")][0]["@value"] :"Role-" + index;
        customTableElement.lookup = participant[ParserCommons.getKairosReference("roleName")] ? participant[ParserCommons.getKairosReference("roleName")][0]["@value"] : "lookup-" + index++;
        entityList= [];
        if (participant[ParserCommons.getKairosReference("values")]) {
              for (const entry of participant[ParserCommons.getKairosReference("values")]) {
                if (entry[ParserCommons.getKairosReference("ta2entity")] && entry[ParserCommons.getKairosReference("ta2entity")][0]["@id"].includes("/Entities/")) {
                  for (const entity of raw[ParserCommons.getKairosReference("entities")]) {
                    if ( entity["@id"] === entry[ParserCommons.getKairosReference("ta2entity")][0]["@id"] && entity["http://schema.org/name"]) {
                      entityList.push(entity["http://schema.org/name"][0]["@value"]);
                      argumentsList.push({id: entity["@id"], name: entity["http://schema.org/name"][0]["@value"]});
                      customTableElement.values.push(
                        {
                        key: keyIndex++,
                        id: entity["@id"], name: entity["http://schema.org/name"][0]["@value"], 
                        confidence: entry[ParserCommons.getKairosReference("confidence")] ? entry[ParserCommons.getKairosReference("confidence")][0]["@list"][0]["@value"] : 0,
                        roleName: participant[ParserCommons.getKairosReference("roleName")] ? participant[ParserCommons.getKairosReference("roleName")][0]["@value"] :"Role-" + index,
                        lookup: participant[ParserCommons.getKairosReference("roleName")] ? participant[ParserCommons.getKairosReference("roleName")][0]["@value"] : "lookup-" + index++});
                      
                    }
                  } 
                } else {
                  for (const data of raw[ParserCommons.getKairosReference("events")][0]["@list"]) { 
                    if (entry[ParserCommons.getKairosReference("ta2entity")] && data["@id"] === entry[ParserCommons.getKairosReference("ta2entity")][0]["@id"] && data["http://schema.org/name"]) {
                      entityList.push(data["http://schema.org/name"][0]["@value"]);
                      argumentsList.push({id: data["@id"], name: data["http://schema.org/name"][0]["@value"]});
                      customTableElement.values.push({
                      key: keyIndex++,
                      id: data["@id"], name: data["http://schema.org/name"][0]["@value"], 
                      confidence: entry[ParserCommons.getKairosReference("confidence")] ? entry[ParserCommons.getKairosReference("confidence")][0]["@list"][0]["@value"] : 0,
                      roleName: participant[ParserCommons.getKairosReference("roleName")] ? participant[ParserCommons.getKairosReference("roleName")][0]["@value"] :"Role-" + index,
                      lookup: participant[ParserCommons.getKairosReference("roleName")] ? participant[ParserCommons.getKairosReference("roleName")][0]["@value"] : "lookup-" + index++});
                    }
                  }
                } 
              }

              // This limits the total number of arguments shown. 
              // GOJS can't handle ellipsis logic when there are a bunch of newlines.
              // If there are more than 20 add ellipsis to last one and remove the rest.
              if (entityLimitCounter <= entityLimit && entityList.length > 0) {
                var joinString = entityList.join(", ") + "\n";
                stringResult += joinString.length > argumentLengthLimit ? joinString.substring(0, argumentLengthLimit) + "...\n" : joinString;
                entityLimitCounter++;
              }
              if (customTableElement.roleName)
                customList.push(customTableElement);
        }
      }
     
     

      if (stringResult !== "") {
        argumentNode.text = stringResult.replace(/\n$/, "");
        argumentNode.arguments = argumentsList;
        argumentNode.customTable = customList;
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
        parentDisplayName: "",
        childrenDisplayNames: "",
        childrenList: [],
        key: "",
        text: "",
        group: "",
        isTreeExpanded: true,
        visible: false,
        isGroup: false,
        description: "",
        confidence: 0,
        category: "",
        categoryStatus: "",
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
        additionalNotes: "",
        customTable: [],
        repeatable: false,
        origName: "",
        origDescription: ""
      };
    }

    static createDeepCopy(node: any): any {
      const model = this.resetModel();
      model.key = node.key + this.groupSuffix;
      model.subgroupEvents = node.subgroupEvents;
      model.childrenList = node.childrenList;
      model.qnode.name = node.qnode.name;
      model.qnode.label = node.qnode.label;
      model.qnode.description = node.qnode.description;
      model.confidence = node.confidence;
      model.parent = node.parent;
      model.categoryStatus = node.categoryStatus;
      model.isGroup = true;
      model.customTable = node.customTable;
      model.childrenGate = node.childrenGate;
      model.text = node.text;
      model.description = node.description;
      model.visible = false;
      model.relations = node.relations;
      return model;
    }

    static checkForOrphanedNodes(node: any, parsedData: any): any {
      let allOrhpaned = false;
      for (const event of node.subgroupEvents) {
        const objIndex = parsedData.moduleLinks.findIndex((obj: { from: any, to: any }) => obj.from == event["@id"] && !obj.to.includes(this.groupSuffix));
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
  const validArray= ["AND", "OR", "XOR"];
  const value = child ?  child[0]["@value"].toUpperCase()  : "";
  if (!validArray.includes(value))
    return "";
  return "(" + value + ")";
}

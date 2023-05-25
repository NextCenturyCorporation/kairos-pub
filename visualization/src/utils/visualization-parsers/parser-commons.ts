import type { LinkTemplate } from "../../interfaces/link-template";

export class ParserCommons {
  static getKairosReference(type: string) {
    return "https://kairos-sdf.s3.amazonaws.com/context/kairos/" + type;
  }

  static resetLink() {
    const link: LinkTemplate = {
      to: "",
      from: "",
      text: "",
      category: "",
      highLighted: false
    };
    return link;
  }

  static createGraphNameList(rawData: any): Array<string> {
    const parsedResults: string[] = [];
    if (rawData) {
      for (const binding of rawData.results.bindings) {
        parsedResults.push(binding.g.value);
      }
    }
    return parsedResults;
  }

  static createEventComplexList(rawData: any): Array<any> {
    const parsedResults: any[] = [];
    if (rawData && rawData.results) {
      for (const binding of rawData.results.bindings) {
        parsedResults.push(binding.o);
      }
    }
    return parsedResults;
  }

  static lookupGenIdFirst(conf: string, lookupType: string, jsonData: any): any {
    for (const data of jsonData) {
      const currentObj = data;
      if (currentObj["@id"] === conf) {
        return currentObj["http://www.w3.org/1999/02/22-rdf-syntax-ns#first"][0][lookupType];
      }
    }
    return "";
  }

  static getRelationReference(node:any, rawData:any): any {
    for (let i = 0; i < node.relations.length; i++) {
      const subject = node.relations[i][ParserCommons.getKairosReference("relationSubject")] ? node.relations[i][ParserCommons.getKairosReference("relationSubject")][0]["@id"] : null;
      const object = node.relations[i][ParserCommons.getKairosReference("relationObject")] ? node.relations[i][ParserCommons.getKairosReference("relationObject")][0]["@id"] : null;
      
      // Remove and bad relations to avoid issues later on
      if (!subject || !object) {
        node.relations.splice(i,1);
        continue;
      }

      if (subject.includes("Events/") || object.includes("Events/")) {
        for (const data of rawData[ParserCommons.getKairosReference("events")][0]["@list"]) {
          if (subject === data["@id"]) {
            node.relations[i][ParserCommons.getKairosReference("relationSubject")][0]["@id"] = data["http://schema.org/name"][0]["@value"];
          }
          if (object === data["@id"]) {
            node.relations[i][ParserCommons.getKairosReference("relationObject")][0]["@id"] = data["http://schema.org/name"][0]["@value"];
          }
        }
      } 
      if (subject.includes("Entities/") || object.includes("Entities/")) {
        for (const data of (rawData[ParserCommons.getKairosReference("entities")] ? rawData[ParserCommons.getKairosReference("entities")] : [])) {
          if (subject === data["@id"]) {
            node.relations[i][ParserCommons.getKairosReference("relationSubject")][0]["@id"] = data["http://schema.org/name"][0]["@value"];
          }
          if (object === data["@id"]) {
            node.relations[i][ParserCommons.getKairosReference("relationObject")][0]["@id"] = data["http://schema.org/name"][0]["@value"];
          }
        }
      }
    }
    return node;
  }
}

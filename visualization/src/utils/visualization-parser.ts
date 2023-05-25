import { FramedSDF21Simple } from "./visualization-parsers/framed-sdf-2-3-simple"
import { ParserCommons } from "./visualization-parsers/parser-commons";
import * as jsonld from "jsonld";
import frame from "../assets/framed.json";
import context from "../assets/context.json";

/**
 * @name Class: visualizationParser
 * @description functions needed in parsing the raw Neptune data into json objects used by the visualization library.
 */ 
export class visualizationParser {
  visualizationParser() {}

  // Find version and get parser
  
  static async transformData(jsonData: any): Promise<any> {
    const framedData: any = frame;
    // Replace context
    [jsonData][0]["@context"] = context["@context"];
    const result = await jsonld.frame(jsonData, framedData);
    const expand = await jsonld.expand(jsonData);
    return FramedSDF21Simple.parse(expand);
  }

  static resetLink(): any {
    return ParserCommons.resetLink();
  }

  static getKairosReference(type: string) {
    return ParserCommons.getKairosReference(type);
  }

  static createEventComplexList(rawData: any): Array<any> {
    return ParserCommons.createEventComplexList(rawData);
  }

  static createGraphNameList(rawData: any): Array<string> {
    return ParserCommons.createGraphNameList(rawData);
  }
}

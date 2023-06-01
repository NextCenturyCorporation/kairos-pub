/**
 * @name Interface: parsedData
 * @description describes the object returned by class visualizationParser.
 */
export interface ParsedData {
  moduleLinks: Array<any>;
  moduleNodes: Array<any>;
  framedRaw: any;
  participantsAcrossEvents: Map<any,any>;
  participantsAcrossRelations: Map<any,any>;
}

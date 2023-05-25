/**
 * @name Interface: modelTemplate
 * @description describes the object needed for visualizing nodes in GOJS.
 */

export interface ModelTemplate {
  key: string;
  text: string;
  group: string;
  parent: string;
  parentDisplayName: string;
  childrenDisplayNames: string;
  childrenList: Array<any>;
  isTreeExpanded: boolean;
  visible: boolean;
  isGroup: boolean;
  description: string;
  confidence: number;
  category: string;
  categoryStatus: string;
  critical: boolean;
  startTime: number;
  endTime: number;
  entityId: string;
  entityRoleName: string;
  subgroupEvents: Array<any>;
  participants: Array<any>;
  status: boolean;
  arguments: Array<any>;
  argumentString: string;
  qnode: any;
  comment: Array<any>;
  ta1Explanation: string;
  relations: Array<any>;
  childrenGate: string;
  isParent: boolean;
  toSpot: any;
  fromSpot: any;
  layer: number;
  opacity: number;
  expanded: boolean;
  additionalNotes: string;
  customTable: Array<any>;
  repeatable: boolean;
  origName: string;
  origDescription: string;
}

/**
 * @name Interface: linkTemplate
 * @description describes the object needed for visualizing links between nodes in GOJS.
 */
export interface LinkTemplate {
  from: string;
  to: string;
  text: string;
  category: string;
  highLighted: boolean;
}

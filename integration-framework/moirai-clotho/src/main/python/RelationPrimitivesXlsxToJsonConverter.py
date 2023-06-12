import sys
import xlrd
import json
from RelationPrimitive import RelationPrimitive

filePath = sys.argv[1]

wb = xlrd.open_workbook(filePath)
relationsData = wb.sheet_by_name("relations")

outputPath = sys.argv[2]
f = open(outputPath, "w")

relations_array = []

for i in range(1, relationsData.nrows):
    curRelationsDataRow = relationsData.row_values(i)
    
    curRelationDict = {}
    # AnnotIndexID
    curRelationDict["annot_index_id"] = curRelationsDataRow[0].strip()
    # name
    curRelationDict["name"] = "{0}.{1}.{2}".format(curRelationsDataRow[1].strip(),curRelationsDataRow[3].strip(),curRelationsDataRow[5].strip())
     # type output-val
    curRelationDict["type_output"] = curRelationsDataRow[2].strip()
    # subType output-val
    curRelationDict["sub_type_output"] = curRelationsDataRow[4].strip()
    # subSubType output-val
    curRelationDict["sub_sub_type_output"] = curRelationsDataRow[6].strip()
    # description
    curRelationDict["description"] = curRelationsDataRow[7].strip()
    # template
    curRelationDict["template"] = curRelationsDataRow[8].strip()
    # arg1 - 8
    curRelationDict["arg1"] = curRelationsDataRow[9].strip()
    curRelationDict["arg1_output"] = curRelationsDataRow[10].strip()
    curRelationDict["arg1_constraints"] = curRelationsDataRow[11].strip().upper()
    # arg2 - 10
    curRelationDict["arg2"] = curRelationsDataRow[12].strip()
    curRelationDict["arg2_output"] = curRelationsDataRow[13].strip()
    curRelationDict["arg2_constraints"] = curRelationsDataRow[14].strip().upper()

    cur_relation = RelationPrimitive(**curRelationDict)

    relations_array.append(cur_relation)

    #f.write(json.dumps(cur_event.__dict__,default=lambda o:o.__dict__, igit pndent=2))


# Manually add SameAs relation
curRelationDict = {}
# AnnotIndexID
curRelationDict["annot_index_id"] = ''
# name
curRelationDict["name"] = 'Physical.SameAs.SameAs'
# type output-val
curRelationDict["type_output"] = 'Physical'
# subType output-val
curRelationDict["sub_type_output"] = 'SameAs'
# subSubType output-val
curRelationDict["sub_sub_type_output"] = 'SameAs'
# description
curRelationDict["description"] = "Arguments are co-referential.  This is not annotated or assessed, but is needed for entity relation representation"
# template
curRelationDict["template"] = '<arg1> is the same as <arg2>'
# arg1 - 8
curRelationDict["arg1"] = 'CoReferent1'
curRelationDict["arg1_output"] = ''
curRelationDict["arg1_constraints"] = 'NONE'
# arg2 - 10
curRelationDict["arg2"] = 'CoReferent2'
curRelationDict["arg2_output"] = ''
curRelationDict["arg2_constraints"] = 'NONE'

cur_relation = RelationPrimitive(**curRelationDict)

relations_array.append(cur_relation)


# Now write all output
f.write(json.dumps(relations_array, default=lambda o:o.__dict__, indent=2))

f.close()


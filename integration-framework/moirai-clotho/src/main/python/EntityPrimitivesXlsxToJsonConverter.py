import sys
import xlrd
import json

from EntityPrimitive import EntityPrimitive

filePath = sys.argv[1]

wb = xlrd.open_workbook(filePath)
entitiesData = wb.sheet_by_name("entities")

outputPath = sys.argv[2]
f = open(outputPath, "w")

entities_array = []

for i in range(1, entitiesData.nrows):
    curEntityDataRow = entitiesData.row_values(i)

    curEntityDict = {}

    # AnnotIndexId
    curEntityDict["annot_index_id"] = curEntityDataRow[0].strip()
    # name
    curEntityDict["name"] = curEntityDataRow[1].strip()
    # description
    curEntityDict["description"] = curEntityDataRow[3].strip()

    cur_entity = EntityPrimitive(**curEntityDict)
    entities_array.append(cur_entity)

# Manually add EVENT entity type
curEntityDict = {}

# AnnotIndexId
curEntityDict["annot_index_id"] = "NONE"
# name
curEntityDict["name"] = 'EVENT'
# description
curEntityDict["description"] = 'Not strictly an entity type in LDC, but this is needed for tooling to represent arguments that take an event.'

cur_entity = EntityPrimitive(**curEntityDict)
entities_array.append(cur_entity)


# Now write all output
f.write(json.dumps(entities_array, default=lambda o:o.__dict__, indent=2))

f.close()


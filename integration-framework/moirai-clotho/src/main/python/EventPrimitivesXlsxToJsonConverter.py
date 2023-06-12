import sys
import xlrd
import json
from EventPrimitive import EventPrimitive

filePath = sys.argv[1]

wb = xlrd.open_workbook(filePath)
eventsData = wb.sheet_by_name("events")
entitiesData = wb.sheet_by_name("entities")
relationsData = wb.sheet_by_name("relations")

outputPath = sys.argv[2]

f = open(outputPath, "w")

events_array = []

for i in range(1, eventsData.nrows):
    curEventsDataRow = eventsData.row_values(i)
    
    curEventDict = {}
    
    # AnnotIndexID
    curEventDict["annot_index_id"] = curEventsDataRow[0].strip()
    # name
    curEventDict["name"] = "{0}.{1}.{2}".format(curEventsDataRow[1].strip(),curEventsDataRow[3].strip(),curEventsDataRow[5].strip())
    # type output-val
    curEventDict["type_output"] = curEventsDataRow[2].strip()
    # subType output-val
    curEventDict["sub_type_output"] = curEventsDataRow[4].strip()
    # subSubType output-val
    curEventDict["sub_sub_type_output"] = curEventsDataRow[6].strip()
    # template
    curEventDict["template"] = curEventsDataRow[8].strip()
    # description
    curEventDict["description"] = curEventsDataRow[7].strip()
    # arg1 - 9
    curEventDict["arg1"] = curEventsDataRow[9].strip()
    curEventDict["arg1_output"] = curEventsDataRow[10].strip()
    curEventDict["arg1_constraints"] = curEventsDataRow[11].strip().upper()
    # arg2 - 12
    curEventDict["arg2"] = curEventsDataRow[12].strip()
    curEventDict["arg2_output"] = curEventsDataRow[13].strip()
    curEventDict["arg2_constraints"] = curEventsDataRow[14].strip().upper()
    # arg3 - 15
    curEventDict["arg3"] = curEventsDataRow[15].strip()
    curEventDict["arg3_output"] = curEventsDataRow[16].strip()
    curEventDict["arg3_constraints"] = curEventsDataRow[17].strip().upper()
    # arg4 - 18 
    curEventDict["arg4"] = curEventsDataRow[18].strip()
    curEventDict["arg4_output"] = curEventsDataRow[19].strip()
    curEventDict["arg4_constraints"] = curEventsDataRow[20].strip().upper()
    # arg5 - 21 
    curEventDict["arg5"] = curEventsDataRow[21].strip()
    curEventDict["arg5_output"] = curEventsDataRow[22].strip()
    curEventDict["arg5_constraints"] = curEventsDataRow[23].strip().upper()
    # arg6 - 24
    curEventDict["arg6"] = curEventsDataRow[24].strip()
    curEventDict["arg6_output"] = curEventsDataRow[25].strip()
    curEventDict["arg6_constraints"] = curEventsDataRow[26].strip().upper()

    cur_event = EventPrimitive(**curEventDict)

    events_array.append(cur_event)

    #f.write(json.dumps(cur_event.__dict__,default=lambda o:o.__dict__, indent=2))

f.write(json.dumps(events_array, default=lambda o:o.__dict__, indent=2))

f.close()

import json
class EventSlot:

    entities_context = "https://kairos-sdf.s3.amazonaws.com/context/kairos/Primitives/Entities/"

    def __init__(self, atId, argIndex, arg, constraints, outputVal):
        self.atId = atId #event_slot_row_data["atId"]
        self.argIndex = argIndex #event_slot_row_data["argIndex"]
        self.roleName = arg #event_slot_row_data["arg"]
        self.outputValue = outputVal
        self.entityTypes = []

        entityConstraints = constraints.split(",") #event_slot_row_data["constraints"].split(",")

        for i in range(len(entityConstraints)):
            self.entityTypes.append(self.entities_context + entityConstraints[i].strip())

    
    #def __str__(self):
     #   return json.dumps(self.__dict__)



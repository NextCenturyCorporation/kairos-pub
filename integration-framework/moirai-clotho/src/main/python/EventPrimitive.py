from EventSlot import EventSlot

class EventPrimitive:

    kairos_context = "https://kairos-sdf.s3.amazonaws.com/context/kairos/Primitives/Events/"

    def __init__(self, **event_row_data):
        self.annotIndexId = event_row_data["annot_index_id"]
        self.atId = self.kairos_context + event_row_data["name"]
        self.name = event_row_data["name"]
        self.typeOutputValue = event_row_data["type_output"]
        self.subTypeOutputValue = event_row_data["sub_type_output"]
        self.subSubTypeOutputValue = event_row_data["sub_sub_type_output"]
        self.template = event_row_data["template"]
        self.description = event_row_data["description"]

        # Create slots for args
        self.slots = []

        arg1 = event_row_data["arg1"]
        arg2 = event_row_data["arg2"]
        arg3 = event_row_data["arg3"]
        arg4 = event_row_data["arg4"]
        arg5 = event_row_data["arg5"]
        arg6 = event_row_data["arg6"]

        slotAtId = self.atId + "/Slots/"

        if arg1:
            slot = EventSlot(atId=slotAtId + arg1, argIndex=1, arg=arg1, constraints=event_row_data["arg1_constraints"], outputVal=event_row_data["arg1_output"])
            self.slots.append(slot)
        if arg2: 
            slot = EventSlot(atId=slotAtId + arg2, argIndex=2, arg=arg2, constraints=event_row_data["arg2_constraints"], outputVal=event_row_data["arg2_output"])
            self.slots.append(slot)
        if arg3:
            slot = EventSlot(atId=slotAtId + arg3, argIndex=3, arg=arg3, constraints=event_row_data["arg3_constraints"], outputVal=event_row_data["arg3_output"])
            self.slots.append(slot)
        if arg4:
            slot = EventSlot(atId=slotAtId + arg4, argIndex=4, arg=arg4, constraints=event_row_data["arg4_constraints"], outputVal=event_row_data["arg4_output"])
            self.slots.append(slot)
        if arg5:
            slot = EventSlot(atId=slotAtId + arg5, argIndex=5, arg=arg5, constraints=event_row_data["arg5_constraints"], outputVal=event_row_data["arg5_output"])
            self.slots.append(slot)
        if arg6:
            slot = EventSlot(atId=slotAtId + arg6, argIndex=6, arg=arg6, constraints=event_row_data["arg6_constraints"], outputVal=event_row_data["arg6_output"])
            self.slots.append(slot)
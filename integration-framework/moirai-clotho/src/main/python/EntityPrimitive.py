class EntityPrimitive:
    kairos_context = "https://kairos-sdf.s3.amazonaws.com/context/kairos/Primitives/Entities/"
    
    def __init__(self, **entity_row_data):
        self.annotIndexId = entity_row_data["annot_index_id"]
        self.atId = self.kairos_context + entity_row_data["name"]
        self.name = entity_row_data["name"]
        self.description = entity_row_data["description"]
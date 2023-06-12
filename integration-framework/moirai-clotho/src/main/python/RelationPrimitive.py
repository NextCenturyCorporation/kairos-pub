class RelationPrimitive:

    kairos_context = "https://kairos-sdf.s3.amazonaws.com/context/kairos/Primitives/Relations/"
    entities_context = "https://kairos-sdf.s3.amazonaws.com/context/kairos/Primitives/Entities/"

    def __init__(self, **relation_row_data):
        self.annotIndexId = relation_row_data["annot_index_id"]
        self.atId = self.kairos_context + relation_row_data["name"]
        self.name = relation_row_data["name"]
        self.template = relation_row_data["template"]
        self.description = relation_row_data["description"]
        self.argOne = relation_row_data["arg1"]
        self.argOneOutputValue = relation_row_data["arg1_output"]
        self.argOneTypeConstraints = []
        self.argTwo = relation_row_data["arg2"]
        self.argTwoOutputValue = relation_row_data["arg2_output"]
        self.argTwoTypeConstraints = []

        argOneConstraints = relation_row_data["arg1_constraints"].split(",")
        argTwoConstraints = relation_row_data["arg2_constraints"].split(",")

        if argOneConstraints[0] != 'NONE':
            for i in range(len(argOneConstraints)):
                if argOneConstraints[i].strip() != 'AUTHOR_CHECK':
                    self.argOneTypeConstraints.append(self.entities_context + argOneConstraints[i].strip())

        if argTwoConstraints[0] != 'NONE':
            for i in range(len(argTwoConstraints)):
                if argTwoConstraints[i].strip() != 'AUTHOR_CHECK':
                    self.argTwoTypeConstraints.append(self.entities_context + argTwoConstraints[i].strip())


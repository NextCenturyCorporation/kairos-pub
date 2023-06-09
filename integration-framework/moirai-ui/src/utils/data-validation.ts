export var rules = {
  // v is set to any because it can be 2 types
  username: [
    (v: any) => !!v || "Username is required",
    (v: any) => (v && v.length >= 6) || "Username must be at least 6 characters",
    (v: any) => (v && v.length <= 30) || "Username must be less than 30 characters"
  ],
  password: [
    (v: any) => !!v || "Correct current password is required",
    (v: any) => (v && v.length >= 8) || "Password must be at least 8 characters",
    (v: any) => (v && v.length <= 30) || "Password must be less than 30 characters",
    (v: any) =>
      /[A-Z]+/.test(v) ||
      "Password must contain an uppercase character, a lower case character, a number, and a special character",
    (v: any) =>
      /[a-z]+/.test(v) ||
      "Password must contain an uppercase character, a lower case character, a number, and a special character",
    (v: any) =>
      /[0-9]+/.test(v) ||
      "Password must contain an uppercase character, a lower case character, a number, and a special character",
    (v: any) =>
      /[!@#$%^&*)(+=.]+/.test(v) ||
      "Password must contain atleast one of the following special characters !@#$%^&*)(+=."
  ],
  optionalPasswordRules: [
    (v: any) => v == "" || (v && v.length >= 8) || "Password must be at least 8 characters",
    (v: any) => v == "" || (v && v.length <= 30) || "Password must be less than 30 characters",
    (v: any) =>
      v == "" ||
      /[A-Z]+/.test(v) ||
      "Password must contain an uppercase character, a lower case character, a number, and a special character",
    (v: any) =>
      v == "" ||
      /[a-z]+/.test(v) ||
      "Password must contain an uppercase character, a lower case character, a number, and a special character",
    (v: any) =>
      v == "" ||
      /[0-9]+/.test(v) ||
      "Password must contain an uppercase character, a lower case character, a number, and a special character",
    (v: any) =>
      v == "" ||
      /[!@#$%^&*)(+=.]+/.test(v) ||
      "Password must contain atleast one of the following special characters !@#$%^&*)(+=."
  ],
  emailAddress: [
    (v: any) => !!v || "Email is required",
    (v: any) => /.+@.+\..+/.test(v) || "Email must be valid"
  ],
  teamName: [
    (v: any) => !!v || "Team Name is required",
    (v: any) => (v && v.length) >= 3 || "Team Name must be at least 3 characters long",
    (v: any) => (v && v.length <= 30) || "Team Name must be less than 30 characters",
    (v: any) =>
      /^[A-Za-z]+[A-Za-z0-9-]*[A-Za-z0-9]+$/.test(v) ||
      "Team Name must start with letters, end with letters or numbers, and can only container letters, numbers, and a '-'."
  ],
  performerGroup: [(v: any) => !!v || "Performer Group is required"],
  accessCode: [
    (v: any) => !!v || "Access Code is required",
    (v: any) => (v && v.length > 16) || "Access Code must be greater than 16 characters"
  ],
  serviceName: [(v: any) => !!v || "Service Name is required"],
  type: [(v: any) => !!v || "Type is required"],
  details: [(v: any) => !!v || "Details are required"],
  subtype: [(v: any) => !!v || "SubType is required"],
  message: [(v: any) => !!v || "Message is required"],
  topic: [(v: any) => !!v || "Topic is required"],
  notes: [(v: any) => !!v || "Notes are required"],
  roleName: [(v: any) => !!v || "Role Name is required"],
  roleDescription: [(v: any) => !!v || "Role Description is required"],
  experimentType: [(v: any) => !!v || "Experiment Type is required"],
  experimentTargetDataSet: [(v: any) => !!v || "Experiment Data set is required"],
  experimentEvaluator: [(v: any) => !!v || "Experiment Evaluator set is required"]
};

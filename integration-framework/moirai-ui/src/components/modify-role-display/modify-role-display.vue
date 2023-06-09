<template src="./modify-role-display.html"></template>

<script lang="ts">
import mixins from "vue-class-component";
import { Component, Vue } from "vue-property-decorator";
import { deepClone } from "../../utils/otherFunctions";
import { JwtRole } from "zeus-api";
import { rules } from "../../utils/data-validation";

const EditRoleProps = Vue.extend({
  props: {
    tableData: Array
  }
});

@Component
export default class ModifyRoleDisplay extends mixins(EditRoleProps) {
  private performerGroupOptions: string = this.$store.state.performerGroupOptions;
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";
  private editRoles: any = this.getRoles();
  private permissions: any = this.getPermissions();
  private editRole: boolean = false;
  private deleteRole: boolean = false;
  private selectedRoleToEdit: any = null as any;

  /*********  Table Settings *********/
  private defaultHeaders: Array<any> = [
    { text: "Name", align: "left", value: "name" },
    { text: "Description", align: "left", value: "description" }
  ];
  private headers: Array<any> = [];
  private tabidx: number = 0;
  private tableLoadingFlag: boolean = false;
  private defaultDisplayValue: String = "Nothing to display";
  private sortBy: Array<String> = ["name"];
  private search: String = "";

  // Form fields
  private validRoleForm: boolean = true;
  private newRoleDescription: string = "";
  private newRoleName: string = "";
  private newPermissions: Array<any> = [];

  // edit handlers
  private editRoleName: boolean = false;
  private editRoleDescription: boolean = false;

  // Rules
  private roleNameRule: Array<Function> = rules.roleName;
  private roleDescriptionRule: Array<Function> = rules.roleDescription;

  private getRoles() {
    this.$store
      .dispatch("admin/retrieveAllRoles")
      .then(() => {
        this.editRoles = this.$store.getters["admin/roles"];
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    return deepClone(this.$store.getters["admin/roles"]);
  }

  private getPermissions() {
    this.$store
      .dispatch("admin/retrieveAllPermissions")
      .then(() => {
        this.permissions = this.$store.getters["admin/permissions"];
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    return deepClone(this.$store.getters["admin/permissions"]);
  }

  private saveAndUpdateDisplay(role: JwtRole): void {
    this.selectedRoleToEdit.name = this.newRoleName;
    this.selectedRoleToEdit.description = this.newRoleDescription;
    this.selectedRoleToEdit.permissions = this.newPermissions;
    this.closeModal(role);
    // fetch new?
    this.editRoles = this.getRoles();
  }

  private openEditRoleModel(role: JwtRole): void {
    this.selectedRoleToEdit = role;
    this.newRoleDescription = role.description;
    this.newRoleName = role.name;
    this.newPermissions = role.permissions ? role.permissions : [];
    this.editRole = true;
  }

  private openNewRoleModal(): void {
    var emptyRole: JwtRole = {
      name: "",
      id: "",
      description: "",
      permissions: []
    };
    this.openEditRoleModel(emptyRole);
  }

  private closeModal(role: JwtRole): void {
    this.editRole = false;
    this.selectedRoleToEdit = null as any;
  }

  private saveRole(): void {
    let requestBody: JwtRole = this.createRequestBody();
    this.$store
      .dispatch("admin/persistRoleState", requestBody)
      .then(userMessage => {
        this.saveAndUpdateDisplay(requestBody);
        this.$store.dispatch("showAppSnackbarMessage", "Role Saved");
      })
      .catch(obj => {
        if (obj.status == 401) {
          this.$store.dispatch("user/clearAuthentication").then(() => {
            this.$router.push("/");
            this.$store.dispatch("showErrorAppSnackbarMessage", obj.response.message);
          });
        } else {
          this.$store.dispatch("showErrorAppSnackbarMessage", obj.response.message);
        }
      });
  }

  private showDeleteModal(role: JwtRole): void {
    this.deleteRole = true;
    this.selectedRoleToEdit = role;
  }

  private deleteRoleConfirmed(): void {
    this.$store
      .dispatch("admin/deleterole", this.selectedRoleToEdit)
      .then(() => {
        this.deleteRole = false;
        this.selectedRoleToEdit = null as any;
        this.editRoles = this.getRoles();
      })
      .catch();
  }

  private createRequestBody(): JwtRole {
    return {
      id: this.selectedRoleToEdit.id ? this.selectedRoleToEdit.id : "",
      name: this.newRoleName,
      description: this.newRoleDescription,
      permissions: this.newPermissions ? this.newPermissions : []
    };
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }

  private created(): void {
    this.headers = this.getHeaders();
  }

  private getHeaders(): Array<any> {
    let h: Array<any> = this.defaultHeaders;
    h.push({ text: "Permissions", value: "permissions" });
    h.push({ text: "", value: "edit" });
    h.push({ text: "", value: "delete" });
    return this.defaultHeaders;
  }

  private canDeleteEdit(role: JwtRole): boolean {
    return role.name !== "ADMIN";
  }
}
</script>

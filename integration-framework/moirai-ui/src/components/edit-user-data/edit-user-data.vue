<template src="./edit-user-data.html"></template>

<script lang="ts">
import mixins from "vue-class-component";
import { Component, Vue, Watch } from "vue-property-decorator";
import { deepClone } from "../../utils/otherFunctions";
import { UserDataDto, JwtUser, JwtRole, AdminApi } from "zeus-api";
import { rules } from "../../utils/data-validation";

const EditUserDataProps = Vue.extend({
  props: {
    tableData: Array
  }
});

@Component
export default class EditUserData extends mixins(EditUserDataProps) {
  private performerGroupOptions: string = this.$store.state.performerGroupOptions;
  private theme: any = this.$vuetify.theme.dark ? "dark" : "light";
  private users: any = this.getUsers();
  private roles: any = this.getRoles();
  private selectedUserToEdit: JwtUser = null as any;
  private editUser: Boolean = false;
  private validUserForm: boolean = true;

  // Rules
  private emailAddressRules: Array<Function> = rules.emailAddress;
  private performerGroupRules: Array<Function> = rules.performerGroup;

  /*********  Table Settings *********/
  private defaultHeaders: Array<any> = [
    { text: "Username", align: "left", value: "username" },
    { value: "assume", sortable: false },
    { text: "Email", value: "emailAddress" },
    { text: "Performer Group", value: "performerGroup" },
    { text: "Team Name", value: "teamName" },
    { text: "Password Expiration Date", value: "passwordExpiration" }
  ];
  private headers: Array<any> = [];
  private tabidx: number = 0;
  private defaultDisplayValue: String = "Nothing to display";
  private sortBy: Array<String> = ["username"];
  private search: String = "";
  private tableLoadingFlag: boolean = false;

  // Displayed
  private newEmailAddress: string = "";
  private newPerformerGroup: Array<string> = [];
  private newRoles: Array<JwtRole> = [];
  private newActive: boolean = true;
  private toggleUserActivity: boolean = true;

  // edit handlers
  private editEmailAddress: boolean = false;
  private editPerformerGroup: boolean = false;
  private editRoles: Boolean = false;

  private created(): void {
    this.headers = this.getHeaders();
  }

  private getHeaders(): Array<any> {
    let h: Array<any> = this.defaultHeaders;
    h.push({ text: "Active", value: "active" });
    h.push({ text: "", value: "edit", sortable: false });
    return this.defaultHeaders;
  }

  get alternateText(): string {
    return "primary--text " + (this.$vuetify.theme.dark ? "text--darken-1" : "");
  }

  private openUserEditModal(user: JwtUser): void {
    this.selectedUserToEdit = user;
    this.newEmailAddress = user.emailAddress;
    this.newPerformerGroup = user.performerGroup ? user.performerGroup.split(",").sort() : [];
    this.newRoles = user.roles ? user.roles : [];
    this.toggleUserActivity = user.active ? true : false;
    this.newActive = user.active ? true : false;
    this.editUser = true;
  }

  private assumeUser(user: JwtUser): void {
    if (this.$store.getters["user/currentUser"].username === user.username) {
      window.alert("You are already signed in as this user!");
    } else {
      this.$store
        .dispatch("admin/assumeUser", user.username)
        .then(() => {
          console.log("Assuming User " + user.username + " Was Successful");
          this.$store.dispatch("user/changeUser");
          this.$store.dispatch("user/updateUserAccount").then(() => {
            this.$router.push("welcome");
          });
        })
        .catch(error => {
          console.log("Assuming User did not work");
          error = true;
        });
    }
  }

  private closeModal(user: JwtUser): void {
    this.editUser = false;
    this.selectedUserToEdit.active = this.newActive;
    this.selectedUserToEdit = null as any;
  }

  private saveAndUpdateDisplay() {
    this.editUser = false;
    this.selectedUserToEdit.emailAddress = this.newEmailAddress;
    this.selectedUserToEdit.performerGroup = this.newPerformerGroup.sort().toString();
    this.selectedUserToEdit.roles = this.newRoles;
    this.selectedUserToEdit = null as any;
  }

  private getUsers() {
    this.tableLoadingFlag = true;
    this.$store
      .dispatch("admin/retrieveUsers")
      .then(() => {
        this.users = this.$store.getters["admin/users"];
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    this.tableLoadingFlag = false;
    return deepClone(this.$store.getters["admin/users"]);
  }

  private getRoles() {
    this.$store
      .dispatch("admin/retrieveAllRoles")
      .then(() => {
        this.roles = this.$store.getters["admin/roles"];
      })
      .catch(errorStatus => {
        this.handleErrorStatus(errorStatus);
      });
    return deepClone(this.$store.getters["admin/roles"]);
  }

  private saveUser(user: JwtUser): void {
    let settings: UserDataDto = this.createSettingsFromUserInput();
    this.$store
      .dispatch("admin/persistUserSettingsState", settings)
      .then(userMessage => {
        this.saveAndUpdateDisplay();
        this.$store.dispatch("showAppSnackbarMessage", "Settings Saved");
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

  private createSettingsFromUserInput(): UserDataDto {
    return {
      id: this.selectedUserToEdit.id ? this.selectedUserToEdit.id : "",
      username: this.selectedUserToEdit.username,
      emailAddress: this.newEmailAddress,
      performerGroup: this.newPerformerGroup ? this.newPerformerGroup.sort().toString() : "",
      active: this.selectedUserToEdit.active ? true : false,
      roles: this.newRoles
    };
  }

  get currentUserActivity(): string {
    return this.selectedUserToEdit.active ? "Active" : "Inactive";
  }

  @Watch("toggleUserActivity")
  onToggleUserActivity(value: boolean): void {
    this.selectedUserToEdit.active = value || false;
  }

  private handleErrorStatus(errorStatus: any): void {
    let errorMessage =
      errorStatus === 401
        ? "User not logged in"
        : "Unexpected error occured; please try again or contact support";
    this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
  }
}
</script>

<style scoped>
.v-icon--switch {
  margin-left: 15px;
  margin-right: -5px;
}
.v-row--switch {
  margin-top: -35px;
}
.v-card-title--profile-images {
  margin-bottom: -20px;
  margin-top: -20px;
}
.v-switch--theme {
  margin-top: -8px;
  margin-left: -40px;
}
.v-avatar--image {
  margin-right: 20px;
}
.v-avatar--reveal {
  align-items: center;
  bottom: 0;
  justify-content: center;
  opacity: 0.7;
  position: absolute;
  width: 100%;
  height: 100%;
}
.v-img--border {
  border: 6px solid #ffff00;
}
</style>

<template src="./html/settings.html"></template>

<script lang="ts">
import { Component, Watch, Mixins } from "vue-property-decorator";
import { rules } from "../utils/data-validation";
import { SettingsDto } from "zeus-api";
import BaseComponent from "../views/BaseComponent.vue";
import momentTZ from "moment-timezone";

@Component
export default class Settings extends Mixins(BaseComponent) {
  // control
  private performerGroupOptions: string = this.$store.state.performerGroupOptions;
  private window: number = 1;
  private selectedImage: string = "";
  private validSettingsForm: boolean = true;
  private newPassDialog: boolean = false;
  private timeZones: string[] = momentTZ.tz.names();

  // displayed
  private newUsername: string = "";
  private newTeamName: string = "";
  private newPassword: string = "";
  private newEmailAddress: string = "";
  private newPerformerGroup: Array<string> = [];
  private newTimeZone: string = "";
  private toggleDarkMode: boolean = this.$vuetify.theme.dark;
  private profileImage: string = "";
  private currentProfileImage: string = "";

  // edit handlers
  private editEmailAddress: boolean = false;
  private editPerformerGroup: boolean = false;

  // verification
  private currentPassword: string = "";

  // Rules are type any b/c it can either be a boolean or string
  private passwordRules: Array<Function> = rules.password;
  private optionalPasswordRules: Array<Function> = rules.optionalPasswordRules;
  private emailAddressRules: Array<Function> = rules.emailAddress;
  private performerGroupRules: Array<Function> = rules.performerGroup;

  // Methods
  created() {
    this.resetSettings();
  }

  private resetSettings(): void {
    // Resets the settings back to their original state
    let currentUser = this.$store.getters["user/currentUser"];

    // Resets the fields back to their original state
    this.newUsername = currentUser.username;
    this.newPassword = currentUser.password;
    this.currentPassword = "";
    this.newEmailAddress = currentUser.emailAddress;
    this.newTeamName = currentUser.teamName;
    this.newPerformerGroup = currentUser.performerGroup.split(",").sort();
    this.newTimeZone = currentUser.timezone ? currentUser.timezone : "America/New_York";
    this.toggleDarkMode = currentUser.darkMode;
    this.currentProfileImage = currentUser.profilePictureURL
      ? currentUser.profilePictureURL
      : "https://randomuser.me/api/portraits/lego/1.jpg";
    this.selectedImage = this.currentProfileImage;
  }

  private createSettingsFromUserInput(): SettingsDto {
    let currentUser = this.$store.getters["user/currentUser"];
    return {
      id: currentUser.id,
      username: this.newUsername,
      password: this.newPassword,
      emailAddress: this.newEmailAddress,
      teamName: this.newTeamName,
      performerGroup: this.newPerformerGroup.sort().toString(),
      timezone: this.newTimeZone,
      darkMode: this.toggleDarkMode,
      profilePictureURL: this.currentProfileImage,
      currentPassword: this.currentPassword
    };
  }

  get valuesChanged(): boolean {
    this.newPerformerGroup = this.newPerformerGroup.sort();

    let currentUser = this.$store.getters["user/currentUser"];
    let oldPerformerGroupString = currentUser.performerGroup
      .split(",")
      .sort()
      .toString();

    if (
      this.newPassword === currentUser.password &&
      this.newEmailAddress === currentUser.emailAddress &&
      this.toggleDarkMode === currentUser.darkMode &&
      this.currentProfileImage === currentUser.profilePictureURL &&
      this.newPerformerGroup.toString() === oldPerformerGroupString &&
      this.newTimeZone === currentUser.timezone
    ) {
      return false;
    }
    return true;
  }

  private saveSettings(): void {
    let settings: SettingsDto = this.createSettingsFromUserInput();
    this.$store
      .dispatch("user/persistUserSettingsState", settings)
      .then(userMessage => {
        this.resetSettings();
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
    this.closeSaveDialog();
  }

  private openSaveDialog(): void {
    this.currentPassword = "";
    this.newPassDialog = true;
  }

  private closeSaveDialog(): void {
    this.currentPassword = "";
    this.newPassDialog = false;
  }

  private setProfileImage(): void {
    // Temporarily stores the user's selection as the new profile image and returns to the main window
    this.currentProfileImage = this.selectedImage;
    this.window--;
  }

  private cancelProfileImage(): void {
    // Cancels the profile image selection and returns to the main window
    this.window--;
    this.selectedImage = this.currentProfileImage;
  }

  private getClassForImage(img: string): string {
    return img == this.selectedImage ? "v-img--border" : "";
  }

  private selectImage(img: string): void {
    this.selectedImage = img;
  }

  // Returns the human readable theme
  get currentThemeMode(): string {
    return this.$vuetify.theme.dark ? "Dark Mode" : "Light Mode";
  }

  // Returns the corresponding icon for the theme
  get currentThemeIcon(): string {
    return this.$vuetify.theme.dark ? "brightness_low" : "brightness_high";
  }

  // Called whenever toggleDarkMode changes
  @Watch("toggleDarkMode")
  onToggleDarkMode(value: boolean): void {
    this.$vuetify.theme.dark = value || false;
  }
}
</script>

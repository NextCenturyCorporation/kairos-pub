<template src="./moirai-login.html"></template>
<!--<style scoped lang="scss" src="./moirai-login.scss"></style>-->

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import { RegisterRequest, ZeusLoginRequest } from "zeus-api";
import { rules } from "../../utils/data-validation";
import NProgress from "nprogress";
import BaseComponent from "../../views/BaseComponent.vue";

@Component
export default class MoiraiLogin extends Mixins(BaseComponent) {
  private loginTitle: string = "Login to your team page";
  private registerTitle: string = "Register your team";
  private forgotUsernameTitle: string = "Forgot Username";
  private forgotPasswordTitle: string = "Forgot Password";
  private enterAccessCodeTitle: string = "Enter Access Code";
  private enterNewPasswordTitle: string = "Set New Password";
  private username: string = "";
  private password: string = "";
  private emailAddress: string = "";
  private teamName: string = "";
  private performerGroup: Array<string> = [];
  private accessCode: string = "";
  private window: number = 1;
  private loading: boolean = false;
  private showPwd: boolean = false;
  private validLoginForm: boolean = true;
  private validRegisterForm: boolean = true;
  private validForgotUsernameForm: boolean = true;
  private validForgotPasswordForm: boolean = true;
  private validAccessCodeForm: boolean = true;
  private validNewPasswordForm: boolean = true;
  // Rulse are type any b/c it can either be a boolean or string
  private usernameRules: Array<Function> = rules.username;
  private passwordRules: Array<Function> = rules.password;
  private emailAddressRules: Array<Function> = rules.emailAddress;
  private teamNameRules: Array<Function> = rules.teamName;
  private performerGroupRules: Array<Function> = rules.performerGroup;
  private accessCodeRules: Array<Function> = rules.accessCode;

  private beginLoadFromButtonPress(): void {
    this.loading = true;
    NProgress.start();
  }

  private endLoadFromButtonPress(): void {
    this.loading = false;
    NProgress.done();
  }

  private login(): void {
    this.beginLoadFromButtonPress();

    let zeusLoginRequest: ZeusLoginRequest = {
      username: this.username,
      password: this.password
    };

    this.$store
      .dispatch("user/authenticate", zeusLoginRequest)
      .then(() => {
        this.loading = false;
        this.$router.push("welcome");
      })
      .catch(errorStatus => {
        let errorMessage =
          errorStatus === 401
            ? "Incorrect username or password"
            : "Unexpected error occured; please try again or contact support";
        this.$store.dispatch("showErrorAppSnackbarMessage", errorMessage);
        this.endLoadFromButtonPress();
      });
  }

  private register(): void {
    this.loading = true;
    let request: RegisterRequest = {
      registrationDto: {
        username: this.username,
        password: this.password,
        emailAddress: this.emailAddress,
        teamName: this.teamName,
        performerGroup: this.performerGroup.sort().toString()
      }
    };

    this.$store
      .dispatch("user/register", request)
      .then(response => {
        alert(response.value);
        this.viewLogin();
      })
      .catch(error => {
        alert(error.response.message);
      })
      .finally(() => {
        this.loading = false;
      });
  }

  private forgotPassword(): void {
    this.loading = true;
    this.$store
      .dispatch("user/recoverPasswordStep1", this.username)
      .then(response => {
        this.window++;
      })
      .catch(error => {
        console.log(error.response)
        if (error.response.message) {
          alert(error.response.message)
        } else {
          // no account associate with (user input)
          alert(JSON.stringify(error.response.value))
        }
      })
      .finally(() => {
        this.loading = false;
      });
  }

  private resetPassword(): void {
    this.loading = true;
    this.$store
      .dispatch("user/recoverPasswordStep2", this.accessCode)
      .then(response => {
        this.window++;
      })
      .catch(error => {
        alert(error.response.message);
      })
      .finally(() => {
        this.loading = false;
      });
  }

  private setPassword(): void {
    this.loading = true;
    this.$store
      .dispatch("user/recoverPasswordStep3", this.password)
      .then(response => {
        this.viewLogin();
      })
      .catch(error => {
        alert(error.response.message);
      })
      .finally(() => {
        this.loading = false;
      });
  }

  private forgotUsername(): void {
    this.loading = true;
    this.$store
      .dispatch("user/recoverUsername", this.emailAddress)
      .then(response => {
        alert(response.value);
        this.viewLogin();
      })
      .catch(error => {
        if (error.response.message) {
          alert(error.response.message)
        } else {
          alert(error.response.value)
        }
      })
      .finally(() => {
        this.loading = false;
      });
  }

  private viewLogin(): void {
    this.resetForm();
    this.window = 1;
  }

  // Wipes data and resets the loading and show password flag
  private resetForm(): void {
    if (this.$refs.loginForm) {
      //This should be of type VueCompoonent, but there appears to be no good place to import this type from
      (this.$refs.loginForm as any).reset();
    }
    if (this.$refs.registerForm) {
      (this.$refs.registerForm as any).reset();
    }
    if (this.$refs.forgotUsernameForm) {
      (this.$refs.forgotUsernameForm as any).reset();
    }
    if (this.$refs.forgotPasswordForm) {
      (this.$refs.forgotPasswordForm as any).reset();
    }
    if (this.$refs.accessCodeForm) {
      (this.$refs.accessCodeForm as any).reset();
    }
    if (this.$refs.newPasswordForm) {
      (this.$refs.newPasswordForm as any).reset();
    }
    this.loading = false;
    this.showPwd = false;
  }
}
</script>

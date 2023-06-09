<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

@Component
export default class BaseComponent extends Vue {
  get currentTheme(): any {
    return this.$vuetify.theme.themes[this.$vuetify.theme.dark ? "dark" : "light"];
  }
  get cssVars(): object {
    return {
      "--tableBackground": this.currentTheme.linesAndBorders
    };
  }

  beforeCreate() {
    //!! necessary so changing the store variable does not change Vuetify field
    this.$vuetify.theme.dark = !!this.$store.getters["user/currentUser"].darkMode;
  }
}
</script>

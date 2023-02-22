import "@fortawesome/fontawesome-free/css/all.css"; // Ensure you are using css-loader
import Vue from "vue";
import Vuetify from "vuetify";
import "vuetify/dist/vuetify.min.css";

Vue.use(Vuetify);

export default new Vuetify({
  theme: {
    dark: false,
    options: {
      customProperties: true
    },
    themes: {
      light: {
        backdrops: {
          base: "#F5F6FA",
          lighten1: "#FFFFFF"
        },
        moduleBackdrop: "#F5F6FA",
        primary: {
          base: "#42434B",
          lighten1: "#313131",
          lighten2: "#FFFFFF"
        },
        button: {
          base: "#0091EA",
          lighten1: "#29B6F6"
        },
        buttonSecondary: {
          base: "#42434B",
          lighten1: "#8A8F9C"
        },
        headerBar: {
          base: "044F87",
          lighten1: "#266CB4"
        },
        linesAndBorders: "#D8DAE4",
        dropShadow: "#A5C4CE",
        accent: "#000000",
        error: "#E53935",
        info: "#9C27B0",
        success: "#4CAF50",
        warning: "#FF8F00",
        anchor: "#0091EA"
      },
      dark: {
        backdrops: {
          base: "#181B1E",
          lighten1: "#26292B",
          lighten2: "#323639"
        },
        moduleBackdrop: "#323639",
        primary: {
          base: "#FFFFFF",
          darken1: "#D7DDE2",
          darken2: "#8A8f9C"
        },
        button: {
          base: "#0091EA",
          lighten1: "#29B6F6"
        },
        buttonSecondary: {
          base: "#8A8F9C",
          lighten1: "#D7DDE2"
        },
        headerBar: {
          base: "#044F87",
          lighten1: "#266CB4"
        },
        linesAndBorders: "#42434B",
        dropShadow: "#000000",
        accent: "#323639",
        error: "#E53935",
        info: "#9C27B0",
        success: "#4CAF50",
        warning: "#FF8F00",
        anchor: "#0091EA"
      }
    }
  },
  //The following chooses the library for icons (v-icon tags) to be one the icons defined here: https://material.io/resources/icons/
  defaultAssets: {
    font: true,
    icons: "md"
  },
  icons: {
    iconfont: "md"
  }
});

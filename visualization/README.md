# KAIROS SDF Visualizer

The KAIROS SDF Visualizer is a standalone tool extracted from the MOIRAI UI.  It visualizes TA2 SDF v2.3.
Please note that it will only visualize the first `instance` in the SDF you upload.

This is a standalone prototype meant for internal use.  Please see the `LICENSE.md` file for licensing information.

## Project setup

The project was developed and tested with node v18.16.0.  It may work with other versions of Node.js, but does not currently work with the latest release.

To set up the project, you will need the following tools:

-   [nodejs 18.16.0 LTS](https://nodejs.org/en)
    -   this comes with npm command line tool
-   [vue-cli](https://cli.vuejs.org/)
    -   after npm is installed, vue-cli can be installed via the following command:
    ```
    npm install -g @vue/cli
    ```
## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin) to make the TypeScript language service aware of `.vue` types.

If the standalone TypeScript plugin doesn't feel fast enough to you, Volar has also implemented a [Take Over Mode](https://github.com/johnsoncodehk/volar/discussions/471#discussioncomment-1361669) that is more performant. You can enable it by the following steps:

1. Disable the built-in TypeScript Extension
    1) Run `Extensions: Show Built-in Extensions` from VSCode's command palette
    2) Find `TypeScript and JavaScript Language Features`, right click and select `Disable (Workspace)`
2. Reload the VSCode window by running `Developer: Reload Window` from the command palette.

## Customize configuration

See [Vite Configuration Reference](https://vitejs.dev/config/).

### GoJS license key

If you have a current GoJS license key, you can set `VITE_APP_GOJS_LICENSE_KEY` in the `.env` file to a quoted string value containing the key.  Without this setting, you will see a watermark in the Visualization component indicating you're using an evaluation license of GoJS.

## Project Setup

After you have the tools, check out the repo from Git. Change to the top level folder. Then run the following command:

```sh
npm install
```

### Compiles and hot-reloads for development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```
npm run build
```

### deploys ./dist/ to https://validation.kairos.nextcentury.com/visualizer

```
npm run deploy
```

### In case the code does not seem to be using the correct version of dependencies correctly

```

npm ci
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run End-to-End Tests with [Cypress](https://www.cypress.io/)

```sh
npm run test:e2e:dev
```

This runs the end-to-end tests against the Vite development server.
It is much faster than the production build.

But it's still recommended to test the production build with `test:e2e` before deploying (e.g. in CI environments):

```sh
npm run build
npm run test:e2e
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

## context.json
File Location: `moirai-visualization\src\assets\context.json`
Add any custom context configurations to this file that is not included in the SDF.

See [Configuration Reference](https://cli.vuejs.org/config/).

## Libraries used 

### Vue

Although Vue is usually used with JavaScript, our application code uses Vue with TypeScript. Vue documentation advises using Vuex (state management library we're using as an intermediary between the server and components) as well as the Vue router. The code for these features both have their own directory under /src.

There is a great article on how TypeScript and Vue interact here: https://blog.logrocket.com/how-to-write-a-vue-js-app-completely-in-TypeScript/

If you've never used TypeScript before, it's mostly a strongly typed version of JavaScript. You can use JavaScript libraries, but will likely need to bring in supplemental libraries containing types for those libraries: https://medium.com/@steveruiz/using-a-javascript-library-without-type-declarations-in-a-typescript-project-3643490015f3

#### Vuex

Vuex advises that all asynchronous opertations in the application should be encapsulated in store actions, so we've mostly stuck to that paradigm. We reference parts of the store state directly in html such as in v-model attributes but in scripts try to use getters. Setting parts or all of a store state should only happen in mutations through actions. 

#### The .vue file

So far the .vue files have been used 1 .vue file per component. .vue files can contain html, css, and a scripting language all in one file. They allow us to used scoped css (scoping css to a component simply by typing "scoped" next to the style tag). .vue files are the only way to do this. However, keeping all of this together can make a file length, so we often link to an sperate html file for that part. 

### Vuetify
-   Made exclusively for vue by a company that appears to be a sibling to the company which created Vue
-   Set of opinionated tags/attributes with more baked in funcionality than basic html
-   predefined codes for predefined classes: ex. "mb-2" = 8px margin-bottom
-   variables.scss and vuetify.ts define some project-wide customizations
-   One way of identifying vuetify tags is if they are in the format v-*****.

It's a good practice to see if there's any specific Vuetify tags/customs before using basic html and css to create your views. Overriding typical options in the Vue ecosystem can be tedious. Using Vue's system for defining colors and typography, for example, is much easier than overriding using your own css. However, it's acknowldged that to do certain things basic html/css is the only choice. 

An example of how select customizations can be not available in the vuetify framework, yet take tedious adjustments to use in the framework can be found here: https://www.reddit.com/r/vuetifyjs/comments/f6tmw7/how_do_you_change_the_background_color_in_vselect/ . Notice in the link provided in the comment answering the post shows css that required a person looking at the html generated by vuetify itself and making style changes to those tags. This can be dangerous if a new version of vuetify decides to use different tag names, heirarchy etc.

## Additional notes

### Vue variables in Css
We use a trick to use vue data variables in some of the custom Css. How to do something like that is described here: https://www.telerik.com/blogs/passing-variables-to-css-on-a-vue-component

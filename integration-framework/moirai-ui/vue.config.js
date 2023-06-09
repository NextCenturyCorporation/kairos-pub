module.exports = {
  runtimeCompiler: true,
  transpileDependencies: ["vuetify"],
  devServer: {
    proxy: {
      "/zeus": {
        target: process.env.ZEUS_PROXY,
        changeOrigin: true,
        logLevel: "debug",
        pathRewrite: { "^/zeus": "" }
      },
      "/clotho": {
        target: process.env.CLOTHO_PROXY,
        changeOrigin: true,
        logLevel: "debug",
        pathRewrite: { "^/clotho": "" }
      }
    }
  },
  configureWebpack: {
    devtool: "source-map"
  }
};

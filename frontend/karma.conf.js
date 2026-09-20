module.exports = function (config) {
  config.set({
    basePath: "",

    frameworks: ["jasmine", "@angular-devkit/build-angular"],

    plugins: [
      require("karma-jasmine"),
      require("karma-chrome-launcher"),
      require("@angular-devkit/build-angular/plugins/karma"),
      require("karma-junit-reporter"),
      require("karma-coverage"),
    ],

    junitReporter: {
      outputDir: "test-reports",
      outputFile: "reports.xml",
      useBrowserName: false,
    },

    coverageReporter: {
      dir: "coverage/",
      reporters: [
        { type: "html", subdir: "." },
        { type: "lcovonly", subdir: ".", file: "lcov.info" },
        { type: "text-summary", subdir: "." },
      ],
    },

    reporters: ["progress", "junit", "coverage"],

    browsers: ["ChromeHeadless"],

    singleRun: true,
  });
};

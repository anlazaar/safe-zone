module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('@angular-devkit/build-angular/plugins/karma'),
      require('karma-junit-reporter'),
    ],
    junitReporter: {
      outputDir: 'test-reports',
      outputFile: 'reports.xml',
      useBrowserName: false,
    },
    reporters: ['progress', 'junit'],
    browsers: ['ChromeHeadless'],
    singleRun: true,
  });
};

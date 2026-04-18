// Iteration 32 (CVM-74 Nachzug): Karma mit Puppeteer-gebundenem
// Chromium, damit `ng test` auch ohne System-Chrome laeuft. Die
// Sandbox (CI, fresh-Devs) hatte zuvor keinen Browser und konnte
// `ChromeHeadless` nicht starten.
//
// Anwendung: `angular.json` zeigt auf diese Config via
// `karmaConfig`. Zusaetzlich bietet sie den Launcher
// `ChromeHeadlessNoSandbox` fuer rootless Container-Runs.
const path = require('path');

process.env.CHROME_BIN = require('puppeteer').executablePath();

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        random: false
      },
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    coverageReporter: {
      dir: path.join(__dirname, './coverage/cvm-frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ]
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-gpu',
          '--disable-dev-shm-usage',
          '--headless=new'
        ]
      }
    },
    singleRun: false,
    restartOnFileChange: true
  });
};

/* globals module:false process:false */
/* eslint strict:0 */
module.exports = function(config) {

    // karma start --coverage [coverageType]
    // http://karma-runner.github.io/0.8/config/coverage.html

    var karmaConfig = {

            // base path, that will be used to resolve files and exclude
            basePath: '',

            // frameworks to use
            frameworks: ['mocha', 'requirejs'],

            // list of files / patterns to load in the browser
            files: [

                // Source
                {pattern: 'js/**/*.js', included: false},

                // Templates
                {pattern: 'js/**/*.ejs', included: false},
                {pattern: 'js/**/*.hbs', included: false},

                // Images
                //{pattern: 'img/**/*.png', included: false},
                {pattern: 'test/assets/*', included: false},

                // Included libs
                'libs/jquery/dist/jquery.js',
                'libs/underscore/underscore.js',
                'libs/@visallo/bootstrap/docs/assets/js/bootstrap.js',

                // Libraries
                {pattern: 'libs/**/*.js', included: false},
                {pattern: 'node_modules/chai/chai.js', included: false},
                {pattern: 'node_modules/chai-datetime/chai-datetime.js', included: false},
                {pattern: 'node_modules/chai-spies/chai-spies.js', included: false},
                {pattern: 'node_modules/chai-as-promised/lib/chai-as-promised.js', included: false},

                // Test Files
                {pattern: 'test/unit/spec/**/*.js', included: false},
                {pattern: 'test/unit/spec/**/*.jsx', included: false},

                // Test Mocks
                {pattern: 'test/unit/mocks/**/*.js', included: false},
                {pattern: 'test/unit/mocks/**/*.json', included: false},
                {pattern: 'test/unit/utils/**/*.js', included: false},

                // Test runner
                'test/unit/runner/testRunner.js'
            ],

            // list of files to exclude
            exclude: [ ],

            proxies: {
                '/resource': '/base/test/assets/resource',
                '/vertex/thumbnail': '/base/test/assets/resource'
            },

            osxReporter: {
                activate: 'com.apple.Terminal'
            },

            // test results reporter to use
            // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
            reporters: ['mocha'],

            // web server port
            port: 9876,

            // enable / disable colors in the output (reporters and logs)
            colors: true,

            // level of logging
            // possible values: DISABLE || ERROR || WARN || INFO || DEBUG
            logLevel: config.LOG_WARN,

            // enable / disable watching file and executing tests whenever any file changes
            autoWatch: true,

            // Start these browsers, currently available:
            // - Chrome
            // - ChromeCanary
            // - Firefox
            // - Opera
            // - Safari (only Mac)
            // - PhantomJS
            // - IE (only Windows)
            browsers: ['PhantomJS'],

            // If browser does not capture in given timeout [ms], kill it
            captureTimeout: 60000,

            // Continuous Integration mode
            // if true, it capture browsers, run tests and exit
            singleRun: false
        },
        coverageType = 'html',
        coverage = process.argv.filter(function(a, index) {
            if (/--coverage/.test(a)) {
                if ((index + 1) < process.argv.length) {
                    coverageType = process.argv[index + 1];
                }
                return true;
            }
            return false;
        }).length;

    if (coverage) {
        karmaConfig.preprocessors = {
            'js/*.js,!js/require.config.js': 'coverage',
            'js/**/*.js': 'coverage'
        };
        karmaConfig.reporters.push('coverage');
        karmaConfig.coverageReporter = {
            type: coverageType,
            dir: 'build/coverage/'
        };
    }

    try {
        require('karma-osx-reporter');
        karmaConfig.reporters.push('osx')
    } catch (e) {
        console.log('npm install karma-osx-reporter for Notification Center support')
    }

    config.set(karmaConfig);
};

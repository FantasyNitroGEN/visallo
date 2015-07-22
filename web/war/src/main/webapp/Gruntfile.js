/*globals module:false*/
module.exports = function(grunt) {
    'use strict';

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        clean: ['jsc', 'css'],

        bower: {
          install: {
              options: {
                  targetDir: './libs',
                  install: true,
                  copy: false,
                  quiet: true
              }
          },
          prune: {
              options: {
                  targetDir: './libs',
                  copy: false,
                  offline: true,
                  quiet: true
              }
          }
        },

        exec: {
            buildOpenlayers: {
                command: 'python build.py -c none full ../OpenLayers.debug.js',
                stdout: false,
                cwd: 'libs/openlayers/build'
            },
            buildPathFinding: {
                command: 'node ../../node/npm/bin/npm-cli.js install -q && make',
                stdout: false,
                cwd: 'libs/PathFinding.js'
            },
            buildAtmosphere: {
                command: 'mvn clean package',
                stdout: false,
                cwd: 'libs/atmosphere-javascript/modules/javascript'
            }
        },

        less: {
            options: {
                paths: ['less'],
                sourceMap: true,
                sourceMapFilename: 'css/visallo.css.map',
                sourceMapURL: 'visallo.css.map',
                sourceMapRootpath: '/'
            },
            development: {
                files: {
                    'css/visallo.css': 'less/visallo.less'
                }
            },
            developmentContrast: {
                files: {
                    'css/visallo-contrast.css': 'less/visallo.less'
                },
                options: {
                    modifyVars: {
                        'pane-background': '#e4e4e4'
                    }
                }
            },
            productionContrast: {
                files: {
                    'css/visallo-contrast.css': 'less/visallo.less'
                },
                options: {
                    compress: true,
                    modifyVars: {
                        'pane-background': '#e4e4e4'
                    }
                }
            },
            production: {
                files: {
                    'css/visallo.css': 'less/visallo.less'
                },
                options: {
                    compress: true
                }
            }
        },

        requirejs: {
            options: {
                mainConfigFile: 'js/require.config.js',
                dir: 'jsc',
                baseUrl: 'js',
                preserveLicenseComments: false,
                removeCombined: false
            },
            development: {
                options: {
                    logLevel: 2,
                    optimize: 'none',
                    keepBuildDir: true
                }
            },
            production: {
                options: {
                    logLevel: 0,
                    optimize: 'uglify2',
                    generateSourceMaps: true
                }
            }
        },

        eslint: {
            development: {
                src: ['js/**/*.js', 'test/unit/**/*.js']
            },
            ci: {
                src: 'js/**/*.js',
                options: {
                    format: 'checkstyle',
                    outputFile: 'build/checkstyle.xml'
                }
            }
        },

        plato: {
            ci: {
                files: {
                    'build/plato': ['js/**/*.js']
                },
                options: {
                    jshint: {
                        browser: true,
                        '-W033': true,
                        '-W040': true
                    }
                }
            }
        },

        watch: {
            options: {
                dateFormat: function(time) {
                    grunt.log.ok('The watch finished in ' + (time / 1000).toFixed(2) + 's. Waiting...');
                },
                spawn: false,
                interrupt: false
            },
            css: {
                files: ['less/**/*.less', 'libs/**/*.css', 'libs/**/*.less'],
                tasks: ['less:development', 'less:developmentContrast', 'notify:css']
            },
            compiledCss: {
                files: ['css/visallo.css'],
                options: {
                    debounceDelay: 0,
                    livereload: {
                        port: 35729,
                        key: grunt.file.read('test/localhost.key'),
                        cert: grunt.file.read('test/localhost.cert')
                    }
                }
            },
            scripts: {
                files: [
                    'js/**/*.js',
                    'js/**/*.less',
                    'js/**/*.ejs',
                    'js/**/*.hbs',
                    'js/**/*.vsh',
                    'js/**/*.fsh'
                ],
                tasks: ['requirejs:development', 'notify:js'],
                options: {
                    livereload: {
                        port: 35729,
                        key: grunt.file.read('test/localhost.key'),
                        cert: grunt.file.read('test/localhost.cert')
                    }
                }
            },
            lint: {
                files: ['js/**/*.js', 'test/unit/**/*.js'],
                tasks: ['eslint:development']
            }
        },

        notify: {
            js: {
                options: {
                    title: 'Visallo',
                    message: 'RequireJS finished'
                }
            },
            css: {
                options: {
                    title: 'Visallo',
                    message: 'Less finished'
                }
            }
        },

        mochaSelenium: {
            options: {
                screenshotAfterEach: true,
                screenshotDir: 'test/reports',
                reporter: 'spec', // doc for html
                viewport: { width: 900, height: 700 },
                timeout: 30e3,
                slow: 10e3,
                implicitWaitTimeout: 100,
                asyncScriptTimeout: 5000,
                usePromises: true,
                useChaining: true,
                ignoreLeaks: false
            },
            firefox: { src: ['test/functional/spec/**/*.js' ], options: { browserName: 'firefox' } },
            chrome: { src: ['test/functional/spec/**/*.js' ], options: { browserName: 'chrome' } }
        },

        karma: {
            options: {
                configFile: 'karma.conf.js',
                runnerPort: 9999,
                singleRun: true
            },
            unit: { },
            ci: {
                preprocessors: {
                    'js/*.js,!js/require.config.js': 'coverage',
                    'js/**/*.js': 'coverage'
                },
                reporters: ['progress', 'junit', 'coverage'],
                coverageReporter: {
                    type: 'html',
                    dir: 'build/coverage/'
                },
                junitReporter: {
                    outputFile: 'build/test-results.xml',
                    suite: ''
                }
            }
        }
      });

      grunt.loadNpmTasks('grunt-bower-task');
      grunt.loadNpmTasks('grunt-exec');
      grunt.loadNpmTasks('grunt-contrib-clean');
      grunt.loadNpmTasks('grunt-contrib-less');
      grunt.loadNpmTasks('grunt-contrib-watch');
      grunt.loadNpmTasks('grunt-contrib-requirejs');
      grunt.loadNpmTasks('grunt-notify');
      grunt.loadNpmTasks('grunt-mocha-selenium');
      grunt.loadNpmTasks('grunt-karma');
      grunt.loadNpmTasks('grunt-plato');
      grunt.loadNpmTasks('grunt-eslint');

      // Speed up lint by only checking changed files
      // ensure we still ignore files though
      var initialEslintSrc = grunt.config('eslint.development.src');
      grunt.event.on('watch', function(action, filepath) {
          var matchingEslint = grunt.file.match(initialEslintSrc, filepath);
          grunt.config('eslint.development.src', matchingEslint);
      });

      grunt.registerTask('deps', 'Install Webapp Dependencies',
         ['bower:install', 'bower:prune', 'exec']);

      grunt.registerTask('test:functional:chrome', 'Run JavaScript Functional Tests in Chrome',
         ['mochaSelenium:chrome']);
      grunt.registerTask('test:functional:firefox', 'Run JavaScript Functional Tests in Firefox',
         ['mochaSelenium:firefox']);
      grunt.registerTask('test:functional', 'Run JavaScript Functional Tests',
         ['test:functional:chrome', 'test:functional:firefox']);

      grunt.registerTask('test:unit', 'Run JavaScript Unit Tests',
         ['karma:unit']);
      grunt.registerTask('test:style', 'Run JavaScript CodeStyle reports',
         ['eslint:ci', 'plato:ci']);

      grunt.registerTask('jenkins', 'Run tests and style reports for jenkins CI',
         ['deps', 'test:style', 'karma:ci']);

      grunt.registerTask('development', 'Build js/less for development',
         ['clean', 'eslint:development', 'less:development', 'less:developmentContrast', 'requirejs:development']);
      grunt.registerTask('production', 'Build js/less for production',
         ['clean', 'eslint:ci', 'less:production', 'less:productionContrast', 'requirejs:production']);

      grunt.registerTask('default', ['development', 'watch']);
};

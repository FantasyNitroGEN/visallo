/*globals module:false*/

var requireConfig = require('./js/require.config');

module.exports = function(grunt) {
    'use strict';

    require('load-grunt-tasks')(grunt);
    grunt.loadTasks('grunt-tasks');

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        clean: {
            src: ['jsc', 'css'],
            libs: ['libs']
        },

        exec: {
            buildPathFinding: {
                command: 'npm install -q && node node_modules/gulp/bin/gulp.js compile',
                stdout: false,
                cwd: 'node_modules/pathfinding'
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

        babel: {
            'jsx-development': {
                options: {
                    sourceMap: 'inline'
                },
                files: [{ expand: true, cwd: 'js', src: ['**/*.jsx'], dest: 'jsc', ext: '.js' }]
            },
            'jsx-production': {
                options: {
                    sourceMap: true
                },
                files: [{ expand: true, cwd: 'js', src: ['**/*.jsx'], dest: 'jsc', ext: '.js' }]
            }
        },

        requirejs: {
            options: {
                mainConfigFile: 'js/require.config.js',
                dir: 'jsc',
                baseUrl: 'js',
                fileExclusionRegExp: /(^\.|\.jsx$)/,
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

        amdwrap: {
            wrapNodeModules: {
                expand: true,
                cwd: 'libs/',
                src: requireConfig.amdWrap,
                dest: 'libs/amd-wrap'
            }
        },

        eslint: {
            development: {
                src: ['js/**/*.js', 'test/unit/**/*.js', 'js/**/*.jsx']
            },
            ci: {
                src: ['js/**/*.js', 'js/**/*.jsx'],
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
                    'js/**/*.jsx',
                    'js/**/*.less',
                    'js/**/*.ejs',
                    'js/**/*.hbs',
                    'js/**/*.vsh',
                    'js/**/*.fsh'
                ],
                tasks: ['babel:jsx-development', 'requirejs:development', 'notify:js'],
                options: {
                    livereload: {
                        port: 35729,
                        key: grunt.file.read('test/localhost.key'),
                        cert: grunt.file.read('test/localhost.cert')
                    }
                }
            },
            lint: {
                files: ['js/**/*.js', 'js/**/*.jsx', 'test/unit/**/*.js'],
                tasks: ['eslint:development']
            }
        },

        notify: {
            js: {
                options: {
                    title: 'Visallo',
                    message: 'Scripts finished'
                }
            },
            css: {
                options: {
                    title: 'Visallo',
                    message: 'Less finished'
                }
            }
        },

        'copy-frontend': {
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
                    dir: 'build/coverage/',
                    subdir: '.'
                },
                junitReporter: {
                    outputDir: 'build',
                    outputFile: 'test-results.xml',
                    useBrowserName: false,
                    suite: ''
                }
            }
        }
      });


      // Speed up lint by only checking changed files
      // ensure we still ignore files though
      var initialEslintSrc = grunt.config('eslint.development.src');
      grunt.event.on('watch', function(action, filepath) {
          var matchingEslint = grunt.file.match(initialEslintSrc, filepath);
          grunt.config('eslint.development.src', matchingEslint);
      });

      grunt.registerTask('deps', 'Install Webapp Dependencies',
         ['clean:libs', 'exec', 'copy-frontend', 'amdwrap']);

      grunt.registerTask('test:unit', 'Run JavaScript Unit Tests',
         ['karma:unit']);
      grunt.registerTask('test:style', 'Run JavaScript CodeStyle reports',
         ['eslint:ci', 'plato:ci']);

      grunt.registerTask('jenkins', 'Run tests and style reports for jenkins CI',
         ['deps', 'test:style', 'karma:ci']);

      grunt.registerTask('development', 'Build js/less for development',
         ['clean:src', 'eslint:development', 'less:development', 'less:developmentContrast', 'babel:jsx-development', 'requirejs:development']);
      grunt.registerTask('production', 'Build js/less for production',
         ['clean:src', 'eslint:ci', 'less:production', 'less:productionContrast', 'babel:jsx-production', 'requirejs:production']);

      grunt.registerTask('default', ['development', 'watch']);
};

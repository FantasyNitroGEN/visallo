
/* global module:false */
module.exports = function(grunt) {
    'use strict';

    var cp = require('child_process'),
        _ = grunt.util._,
        fs = require('fs'),
        log = grunt.log,
        path = require('path'),
        verbose = grunt.verbose,
        copyOptions = {
            process: function(file, srcPath, destPath) {
                var matches = (/^.*\.(scss|less|css|js|map|png|jpg|eot|svg|ttf|woff)$/).test(srcPath);
                if (!matches) {
                    return false;
                }
                return file;
            }
        };

    grunt.registerTask('copy-frontend', 'Copy Front-End resources', function() {
        var deps = _.keys(grunt.file.readJSON('package.json').dependencies),
            sourceDir = 'node_modules',
            outputDir = 'libs';

        grunt.file.mkdir(outputDir);
        deps.forEach(function(dep) {
            var src = path.join(sourceDir, dep),
                dest = path.join(outputDir, dep);
            log.debug('copying', dep);
            copyDir(src, dest);
        })
    })

    function copyDir(src, dest) {
        grunt.file.mkdir(dest);
        var files = fs.readdirSync(src);
        for (var i = 0; i < files.length; i++) {
            var current = fs.lstatSync(path.join(src, files[i]));
            if (current.isDirectory()) {
                copyDir(path.join(src, files[i]), path.join(dest, files[i]));
            } else {
                grunt.file.copy(path.join(src, files[i]), path.join(dest, files[i]), copyOptions);
            }
        }
    }
}

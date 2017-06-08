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

    grunt.registerMultiTask('transform-sprite', function() {
        var src = this.data;
        var input = grunt.file.readJSON(src);
        var maxWidth = 0, maxHeight = 0;
        var startsNumber = /^\d.*/;
        var list = Object.keys(input).map(function(name) {
                var obj = input[name];
                var px = obj.px;
                maxWidth = Math.max(maxWidth, obj.width);
                maxHeight = Math.max(maxHeight, obj.height);
                return {
                    label: label(name),
                    value: obj.source_image,
                    num: num(name),
                    backgroundPosition: `${px.offset_x} ${px.offset_y}`,
                    backgroundSize: `${px.total_width} ${px.total_height}`,
                    w: obj.width,
                    h: obj.height,
                    width: px.width,
                    height: px.height
                };
            }).sort(function(a, b) {
                if (startsNumber.test(a.label) && !startsNumber.test(b.label)) return 1;
                if (!startsNumber.test(a.label) && startsNumber.test(b.label)) return -1;
                return a.label === b.label ? 0 : a.label < b.label ? -1 : 1;
            });
        var output = { list: list, maxWidth: maxWidth, maxHeight: maxHeight };

        list.forEach(function(obj) {
            obj.offset = maxWidth - obj.w;
            obj.scale = 20 / Math.max(obj.w, obj.h)
        })

        fs.writeFileSync(src + '_array', JSON.stringify(output));
    })

    function num(name) {
        var m = name.match(/[-_](\d+)[-_]/);
        if (m) {
            return m[1]
        }
        return '000';
    }

    function label(name) {
        return name
            .replace(/(^glyphicons[-_](?:halflings[-_])?\d+[-_]|\@2x$)/g, '')
            .replace(/[-_]+/g, ' ')
            .replace(/\w\S*/g, s => s.charAt(0).toUpperCase() + s.substr(1).toLowerCase())
    }
}

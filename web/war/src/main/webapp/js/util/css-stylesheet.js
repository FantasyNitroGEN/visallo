
define([], function() {
    'use strict';

    function Sheet() {
        var style = document.createElement('style');
        // WebKit hack :(
        style.appendChild(document.createTextNode(''));
        document.head.appendChild(style);

        var sheet = style.sheet,
            index = 0;

        this.addRule = function(selector, definition) {
            if (!sheet) throw new Error('No stylesheet found in page');

            if (sheet.insertRule) {
                sheet.insertRule(selector + ' {' + definition + '; }', index++);
            } else {
                sheet.addRule(selector, definition, index++);
            }
            return this;
        };

        this.remove = function() {
            if (style) {
                style.parentNode.removeChild(style);
                style = null;
                sheet = null;
            }
        }
    }

    var defaultSheet;

    return {
        addRule: function(selector, definition) {
            if (!defaultSheet) {
                defaultSheet = new Sheet();
            }
            defaultSheet.addRule(selector, definition);
        },

        addSheet: function() {
            return new Sheet();
        }
    };
});

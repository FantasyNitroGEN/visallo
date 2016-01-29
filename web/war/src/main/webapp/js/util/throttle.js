
define([], function() {
    'use strict';

    var keys = {};

    function throttle(key, throttleMillis, func) {
        if (arguments.length === 1 && key) {
            return clearTimeout(keys[key]);
        }
        return function(event) {
            var self = this;

            clearTimeout(keys[key]);
            keys[key] = setTimeout(function() {
                func.call(self, event);
            }, throttleMillis);
        };
    }

    return throttle;
});

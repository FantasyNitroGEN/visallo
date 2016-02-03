
define([], function() {
    'use strict';
    return function abortPrevious(func) {
        var previousPromise;
        return function() {
            var args = Array.prototype.slice.call(arguments, 0);

            if (previousPromise && previousPromise.cancel) {
                previousPromise.cancel();
            }

            previousPromise = func.apply(null, args);

            return previousPromise;
        }
    };
});

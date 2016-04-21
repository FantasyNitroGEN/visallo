
define([], function() {
    'use strict';
    return function queue(func) {
        var stack = [], promise;
        return function() {
            var args = Array.prototype.slice.call(arguments, 0),
                thisPromise = function() {
                    var p = func.apply(null, args);
                    p.tap(function() {
                        var index = _.findIndex(stack, p);
                        if (index >= 0) {
                            stack.splice(index, 1);
                        }
                        if (stack.length === 0) {
                            promise = null;
                        }
                    })
                    stack.push(p);
                    return p;
                };

            if (promise && !promise.isCancelled()) {
                promise = promise.then(thisPromise)
            } else {
                promise = thisPromise();
            }

            return promise;
        }
    };
});

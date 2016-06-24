define(['util/promise'], function(Promise) {
    'use strict';

    return function() {
        var called = (function() {
                var resolve, promise = new Promise(function(r) { resolve = r; });
                return { resolve: resolve, promise: promise };
            })();
        return called;
    }
})

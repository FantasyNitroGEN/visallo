define(['require'], function(require) {
    'use strict';

    return function() {
        require(['../util/store'], function(store) {
            store.logStatistics();
        })
    }
})

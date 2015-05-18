define(['util/promise', 'jquery'], function() {
    'use strict';

    return new Promise(function(f) {
        if ('visalloData' in window && visalloData.currentUser) {
            return f();
        }

        $(document)
            .on('currentUserVisalloDataUpdated', function() {
                f();
            })
    })
});

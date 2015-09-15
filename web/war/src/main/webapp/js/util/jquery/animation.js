define(['jquery'], function($) {
    'use strict';

    $.fn.animatePop = function(cls, delay) {
        if (!cls) cls = 'pop-fast';
        if (!delay) delay = 250;

        var fields = $(this).removeClass(cls);

        return new Promise(function(fulfill) {
            requestAnimationFrame(function animate() {
                if (!fields.length) {
                    fulfill();
                    return;
                }

                fields.eq(0).addClass(cls);
                fields = fields.slice(1);
                _.delay(animate, delay);
            });
        })
    };
});

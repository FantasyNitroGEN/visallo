
define(['app'], function(app) {
    'use strict';

    // Disable debug mode
    require(['flight/lib/debug'], function(d) {
        d.enable(false);
    });

    describe('app', function() {

        it('should exist', function() {
            expect(app).to.be.a('function');
        });

    });
});

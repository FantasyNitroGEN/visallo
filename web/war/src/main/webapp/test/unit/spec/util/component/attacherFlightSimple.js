define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(FlightSimple);

    function FlightSimple() {
        this.after('initialize', function() {
            this.$node.text('FlightSimple');
        });
    }
});

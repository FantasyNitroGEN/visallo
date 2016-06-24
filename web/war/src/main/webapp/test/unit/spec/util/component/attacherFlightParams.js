define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(FlightParams);

    function FlightParams() {
        this.attributes({
            param: null
        })
        this.after('initialize', function() {
            this.$node.text(this.attr.param);
        });
    }
});

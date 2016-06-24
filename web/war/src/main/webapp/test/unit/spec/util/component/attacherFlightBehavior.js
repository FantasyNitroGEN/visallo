define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(FlightBehavior);

    function FlightBehavior() {
        this.attributes({
            divSelector: 'div',
            spanSelector: 'span'
        })
        this.after('initialize', function() {
            this.$node.html('<div>FlightBehavior<span>mapped</span></div>')
            this.on('click', {
                divSelector: function(event) {
                    $(event.target).trigger('customBehavior', 'param1fromflight');
                },
                spanSelector: function(event) {
                    $(event.target).trigger('legacy', 'param1fromflightMapped');
                }
            })
        });
    }
});

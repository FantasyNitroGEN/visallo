define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(FlightApi);

    function FlightApi() {
        this.attributes({
            visalloApi: null
        })
        this.after('initialize', function() {
            this.$node.text(this.attr.visalloApi.v1.formatters.string.plural(10, 'cat'));
        });
    }
});

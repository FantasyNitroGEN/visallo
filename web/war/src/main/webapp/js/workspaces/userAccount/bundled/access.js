define([
    'flight/lib/component',
    './template.hbs'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(Access);

    function Access() {

        this.after('initialize', function() {
            this.$node.html(template({}));
        });

    }
});

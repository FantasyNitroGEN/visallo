define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(ItemTestLayout);

    function ItemTestLayout() {

        this.after('initialize', function() {
            this.$node.html(this.attr.children.map(function(c) {
                return c.element;
            }))
        });

    }
});

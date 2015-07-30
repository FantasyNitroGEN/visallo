define([
    'flight/lib/component',
    './util'
], function(defineComponent, util) {
    'use strict';

    return defineComponent(VisibilityViewerContainer);

    function VisibilityViewerContainer() {

        this.after('initialize', function() {
            util.attachComponent('viewer', this.node, this.attr);
        });
    }
});



define([
    'flight/lib/component',
    './util'
], function(defineComponent, util) {
    'use strict';

    return defineComponent(VisibilityEditorContainer);

    function VisibilityEditorContainer() {

        this.after('initialize', function() {
            util.attachComponent('editor', this.node, this.attr);
        });
    }
});




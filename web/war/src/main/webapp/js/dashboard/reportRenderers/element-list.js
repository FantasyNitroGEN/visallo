define([
    'flight/lib/component',
    'util/element/list',
    './withRenderer'
], function(
    defineComponent,
    ElementList,
    withRenderer) {
    'use strict';

    return defineComponent(ElementListReportRenderer, withRenderer);

    function ElementListReportRenderer() {
        this.render = function(d3, node, data) {
            $(node)
                .teardownComponent(ElementList)
                .css('overflow', 'auto');
            ElementList.attachTo(node, {
                items: data.root
            })
        }
    }
});

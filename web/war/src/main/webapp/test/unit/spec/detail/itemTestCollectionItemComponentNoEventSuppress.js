define(['flight/lib/component'], function(defineComponent) {
    return defineComponent(ItemTestCollectionItemNoEventSuppress);
    function ItemTestCollectionItemNoEventSuppress() {
        this.attributes({
            ignoreUpdateModelNotImplemented: true,
            model: null
        })
        this.after('initialize', function() {
            this.node.textContent = this.attr.model;
        })
    }
})

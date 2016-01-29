define(['flight/lib/component'], function(defineComponent) {
    return defineComponent(CollectionItemDataSet);
    function CollectionItemDataSet() {
        this.attributes({
            model: null,
            ignoreUpdateModelNotImplemented: true
        })
        this.after('initialize', function() {
            this.node.dataset.componentSet = 'true'
        })
    }
})

define(['flight/lib/component'], function(defineComponent) {
    return defineComponent(ItemTestCollectionItem);
    function ItemTestCollectionItem() {

        this.attributes({
            prefix: '',
            model: null
        });

        this.after('initialize', function() {
            var prefix = this.attr.prefix || '';
            this.node.textContent = prefix + this.attr.model;
            this.on('updateModel', function(event, data) {
                this.node.textContent = prefix + data.model;
            })
        })
    }
})

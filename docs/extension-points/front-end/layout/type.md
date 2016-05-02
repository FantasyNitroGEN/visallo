# Layout Type

## Built-In Layout Types

### flex

Uses flexbox model to layout children vertically or horizontally

## Custom layout types

```js
registry.registerExtension('org.visallo.layout.type', {
    type: 'grid'
    componentPath: 'mygridlayout'
});

define('mygridlayout', ['flight/lib/component'], function(defineComponent) {
    return defineComponent(MyGridLayout);
    function MyGridLayout() {
        this.attributes({
            children: [],
            rows: null,
            col: null
        })
        this.after('initialize', function() {
            var children = this.attr.children;
            this.$node.html(...);
        });
    }
});
```

Graph Options Plugin
=================

Plugin to add custom options components which display in the graph options menu (next to Fit).

To register an option:

```js
require(['configuration/plugins/registry'], function(registry) {

    // Define a custom Flight component
    define('myplugins/hello_world', ['flight/lib/component'], function(defineComponent) {
        return defineComponent(HelloWorld);

        function HelloWorld() {
            this.after('initialize', function() {
                this.$node.html('Hello World!!');
            })
        }
    });

    // Register the component path,
    registry.registerExtension('org.visallo.graph.options', {
        identifier: 'helloWorld',
        optionComponentPath: 'myplugins/hello_world'
    });
});
```

Graph options can access the `cy` (cytoscape) object using `this.attr.cy`

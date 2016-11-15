Map Options Plugin
=================

Plugin to add custom options components (Flight or React) which display in the map options menu (next to Fit).

All registered components will be passed:

* `ol` `[Object]`: [Openlayers Api](http://openlayers.org/en/latest/apidoc/)
* `map` [`[ol.Map]`](http://openlayers.org/en/latest/apidoc/ol.Map.html): Openlayers map instance
* `cluster` `[Object]`: Object with keys:
    * `clusterSource` [`[MultiPointCluster]`](https://github.com/v5analytics/visallo/blob/master/web/plugins/map-product/src/main/resources/org/visallo/web/product/map/multiPointCluster.js): Implements the [`ol.source.Cluster`](http://openlayers.org/en/latest/apidoc/ol.source.Cluster.html) interface to cluster the `source` features.
    * `source` [`[ol.source.Vector]`](http://openlayers.org/en/latest/apidoc/ol.source.Vector.html): The source of all map pins before clustering. 
    * `layer` [`[ol.layer.Vector]`](http://openlayers.org/en/latest/apidoc/ol.layer.Vector.html): The pin vector layer

To register an option:

## React

```js
// Create a MyOption.jsx and use registerJavaScriptComponent in your WebAppPlugin.
require(['react', 'public/v1/api'], function(React, visalloApi) {
    const MyOption = React.createClass({
        render() {
            const { ol, map, cluster } = this.props;

            const myOptionDefault = visalloData.currentUser.uiPreferences['my-option-value'];
            return (
                <label>My Setting
                    <input onChange={this.onChange} type="checkbox" defaultChecked={myOptionDefault} />
                </label>
            );
        },
        onChange(event) {
            visalloData.currentUser.uiPreferences['my-option-value'] = event.target.checked;
            // Save
            visalloApi.dataRequest('user', 'preference', 'my-option-value', event.target.checked);
        }
    })
    return MyOption;
});

// Create plugin.js and use registerJavaScript("plugin.js", true) in your WebAppPlugin
require(['configuration/plugins/registry'], function(registry) {

    // Register the component path,
    registry.registerExtension('org.visallo.map.options', {
        identifier: 'myOption',
        optionComponentPath: 'myplugins/MyOption'
    });
});
```

## Flight

```js
require(['configuration/plugins/registry'], function(registry) {

    // Define a custom Flight component
    define('myplugins/hello_world', ['flight/lib/component'], function(defineComponent) {
        return defineComponent(HelloWorld);

        function HelloWorld() {
            this.after('initialize', function() {
                // this.attr.ol
                // this.attr.map
                // this.attr.cluster
                this.$node.html('Hello World!!');
            })
        }
    });

    // Register the component path,
    registry.registerExtension('org.visallo.map.options', {
        identifier: 'helloWorld',
        optionComponentPath: 'myplugins/hello_world'
    });
});
```


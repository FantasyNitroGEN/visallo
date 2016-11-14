Graph Options Plugin
=================

Plugin to add custom options components (Flight or React) which display in the graph options menu (next to Fit).

To register an option:

## React

```js
// Create a MyOption.jsx and use registerJavaScriptComponent in your WebAppPlugin.
require(['react', 'public/v1/api'], function(React, visalloApi) {
    const MyOption = React.createClass({
        render() {
            // cytoscape instance is available as prop
            const { cy } = this.props;

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
    registry.registerExtension('org.visallo.graph.options', {
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

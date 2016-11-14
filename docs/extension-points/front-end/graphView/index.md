Graph View Plugin
=================

Plugin to add custom view components which overlay the graph. Used for toolbars, etc., that interact with the graph.
Views can be paths to [Flight](https://github.com/flightjs/flight) or [React](https://facebook.github.io/react/) components.

To register a view:

## React
```js
// Create a HelloWorld.jsx and use registerJavaScriptComponent in your WebAppPlugin.
require(['react'], function(React) {
    const HelloWorld = React.createClass({
        render() {
            return (<h1>Hello World!</h1>);
        }
    })
    return HelloWorld;
});

// Create plugin.js and use registerJavaScript("plugin.js", true) in your WebAppPlugin
require(['configuration/plugins/registry'], function(registry) {
    // Register the component path, and optionally, provide a CSS class to ease styling
    // The plugin framework will add a div element with this class to the graph DOM.
    // The Java WebAppPlugin subclass can register a CSS file to supply styling for this element.
    registry.registerExtension('org.visallo.graph.view', {
        componentPath: 'myplugins/HelloWorld',
        className: 'hello-world-react-example'
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

    // Register the component path, and optionally, provide a CSS class to ease styling
    // The plugin framework will add a div element with this class to the graph DOM.
    // The Java WebAppPlugin subclass can register a CSS file to supply styling for this element.
    registry.registerExtension('org.visallo.graph.view', {
        componentPath: 'myplugins/hello_world',
        className: 'hello-world-example'
    });
});
```

All graph views are placed just above the graph in an absolutely positioned container element. This container is automatically resized based on open panes. To position elements use absolute positioning relative to the container. For example, to position a view in the top right, using inline css: (or define in external stylesheet using the className)

```js
// React
render() {
    return (<h1 style={{ position: 'absolute', right: 100, top: 20 }}>Hello World</h1>);
}


// Flight
this.$node.html('Hello World').css({
    position: 'absolute',
    // Move to right of graph view tool items (zoom, fit, pan, etc)
    right: '100px',
    top: '20px'
});
```


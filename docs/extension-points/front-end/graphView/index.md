# Graph View

* [Graph View JavaScript API `org.visallo.graph.view`](../../../javascript/org.visallo.graph.view.html)
* [Graph View Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-view)Graph View Plugin

Plugin to add custom view components which overlay the graph. Used for toolbars, etc., that interact with the graph.

Views can be Flight or React components and should be styled to be absolutely positioned. The absolute position given is relative to the graph. 0,0 is top-left corner of graph. 

<div style="text-align:center">
<img src="./view.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

### Web Plugin

Register the plugin script, React component, and less in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/99c0e686/extension-graph-view/src/main/java/org/visallo/examples/graph_view/GraphViewWebAppPlugin.java#L17-L19" %}{% endgithub_embed %}

### Register Extension

Register the options extension pointing to the React component.

{% github_embed "https://github.com/visallo/doc-examples/blob/99c0e686/extension-graph-view/src/main/resources/org/visallo/examples/graph_view/plugin.js#L3-L5" %}{% endgithub_embed %}

### Component

Create the graph view component. This one will be like a floating toolbar panel.

{% github_embed "https://github.com/visallo/doc-examples/blob/99c0e686/extension-graph-view/src/main/resources/org/visallo/examples/graph_view/View.jsx#L6-L25" %}{% endgithub_embed %}

### Style

The less style file is wrapped in the class name defined in the component to avoid conflicts with other plugins and core Visallo styles.

{% github_embed "https://github.com/visallo/doc-examples/blob/99c0e686/extension-graph-view/src/main/resources/org/visallo/examples/graph_view/style.less" %}{% endgithub_embed %}

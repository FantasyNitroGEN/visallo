# Graph Node Class

* [Graph Node Class JavaScript API `org.visallo.graph.node.class`](../../../javascript/org.visallo.graph.node.class.html)
* [Graph Node Class Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-node-class)

Register a function that can add or remove classes from [Cytoscape](http://js.cytoscape.org/) nodes for custom styling.

<div style="text-align:center">
<img src="./class.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

### Web Plugin

Register the plugin script in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/03c78745/extension-graph-node-class/src/main/java/org/visallo/examples/graph_node_class/GraphNodeClassWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Extension

Register the class extension and apply a `unknownName` class when the vertex is a person with no name property.

{% github_embed "https://github.com/visallo/doc-examples/blob/03c78745/extension-graph-node-class/src/main/resources/org/visallo/examples/graph_node_class/plugin.js#L3-L9" %}{% endgithub_embed %}

Register a style extension to test the behavior by adjusting the opacity.

{% github_embed "https://github.com/visallo/doc-examples/blob/03c78745/extension-graph-node-class/src/main/resources/org/visallo/examples/graph_node_class/plugin.js#L11-L16" %}{% endgithub_embed %}

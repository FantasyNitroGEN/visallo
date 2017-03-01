# Graph Edge Class

* [Graph Edge Class JavaScript API `org.visallo.graph.edge.class`](../../../javascript/org.visallo.graph.edge.class.html)
* [Graph Edge Class Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-edge-class)

Function that can change [Cytoscape](http://js.cytoscape.org/) classes of edges. Useful for customizing the style of edges on the graph.

<div style="text-align:center">
<img src="./class.png" width="100%" style="max-width: 300px;">
</div>

## Tutorial

### Web Plugin

Register a plugin file to register the extensions.

{% github_embed "https://github.com/visallo/doc-examples/blob/238a4ac/extension-graph-edge-class/src/main/java/org/visallo/examples/graph_edge_class/GraphEdgeClassWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Extension

Register the class extension that checks if any of the edges (they are by default bundled together by type) has a comment. If any of them do, add a class.

{% github_embed "https://github.com/visallo/doc-examples/blob/238a4ac/extension-graph-edge-class/src/main/resources/org/visallo/examples/graph_edge_class/plugin.js#L3-L11" %}{% endgithub_embed %}

Register a style extension to test.

{% github_embed "https://github.com/visallo/doc-examples/blob/238a4ac/extension-graph-edge-class/src/main/resources/org/visallo/examples/graph_edge_class/plugin.js#L13-L21" %}{% endgithub_embed %}

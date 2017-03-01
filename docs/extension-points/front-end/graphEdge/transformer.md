# Graph Edge Transformer

* [Graph Edge Transformer JavaScript API `org.visallo.graph.edge.transformer`](../../../javascript/org.visallo.graph.edge.transformer.html)
* [Graph Edge Transformer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-edge-transformer)

Register a function that can modify the [Cytoscape](http://js.cytoscape.org) edge data.

<div style="text-align:center">
<img src="./transformer.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

## Web Plugin

Create a web plugin and register the plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/381ff3b5/extension-graph-edge-transformer/src/main/java/org/visallo/examples/graph_edge_transformer/GraphEdgeTransformerWebAppPlugin.java#L17" %}{% endgithub_embed %}

## Register Extension

Register the transformer that counts all the properties of the collapsed edges and sets a new data parameter called `numProperties`.

{% github_embed "https://github.com/visallo/doc-examples/blob/381ff3b5/extension-graph-edge-transformer/src/main/resources/org/visallo/examples/graph_edge_transformer/plugin.js#L3-L7" %}{% endgithub_embed %}

Create a style extension to test. We use `mapData` to interpolate the number into a color.

{% github_embed "https://github.com/visallo/doc-examples/blob/381ff3b5/extension-graph-edge-transformer/src/main/resources/org/visallo/examples/graph_edge_transformer/plugin.js#L9-L15" %}{% endgithub_embed %}

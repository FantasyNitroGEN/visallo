# Graph Node Transformer

* [Graph Node Transformer JavaScript API `org.visallo.graph.node.transformer`](../../../javascript/org.visallo.graph.node.transformer.html)
* [Graph Node Transformer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-node-transformer)

Allows extensions to adjust the data attribute of [Cytoscape](http://js.cytoscape.org/) nodes.

<div style="text-align:center">
<img src="./transformer.png" width="100%" style="max-width: 260px;">
</div>

## Tutorial

This tutorial will adjust the size of graph nodes based on how many properties they have.

### Web Plugin

Register the plugin script in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/551d4b7f/extension-graph-node-transformer/src/main/java/org/visallo/examples/graph_node_transformer/GraphNodeTransformerWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Extension

Register the transformer extension that just places a property count into the data object.

{% github_embed "https://github.com/visallo/doc-examples/blob/551d4b7f/extension-graph-node-transformer/src/main/resources/org/visallo/examples/graph_node_transformer/plugin.js#L3-L5" %}{% endgithub_embed %}

Register a style extension to test the transformer. The selector checks for the data property and adjusts the size of the node depending on `numProperties`.

{% github_embed "https://github.com/visallo/doc-examples/blob/551d4b7f/extension-graph-node-transformer/src/main/resources/org/visallo/examples/graph_node_transformer/plugin.js#L7-L14" %}{% endgithub_embed %}

The function uses `devicePixelRatio` to be the same perceived size on hidpi and normal displays. The default Cytoscape stylesheet does something similar.

{% github_embed "https://github.com/visallo/visallo/blob/161768ca/web/plugins/graph-product/src/main/resources/org/visallo/web/product/graph/styles.js#L72-L74" %}{% endgithub_embed %}


# Graph Style

* [Graph Style JavaScript API `org.visallo.graph.style`](../../../javascript/org.visallo.graph.style.html)

Apply additional [Cytoscape styles](http://js.cytoscape.org/#style) to the graph. This is used to adjust the styling of all graph elements: Nodes, Edges, Decorations, etc.

* [Node Styles](http://js.cytoscape.org/#style/node-body)
* [Edges Styles](http://js.cytoscape.org/#style/edge-line)
* [Core Styles](http://js.cytoscape.org/#style/core)
* Style values can use [interpolation](http://js.cytoscape.org/#style/mappers) based on data values.

All style extensions have precedence over the built-in style if the selectors are similar specificity. The Visallo stylesheet is defined as:

{% github_embed "https://github.com/visallo/visallo/blob/d7419120/web/plugins/graph-product/src/main/resources/org/visallo/web/product/graph/styles.js#L46-L301", hideLines=['57-74', '77-236', '240-248', '251-299'] %}{% endgithub_embed %}

## Examples

### [Graph Node Class Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-node-class)

Change the opacity of a node based on a class.

{% github_embed "https://github.com/visallo/doc-examples/blob/ba76f66a/extension-graph-node-class/src/main/resources/org/visallo/examples/graph_node_class/plugin.js#L11-L16" %}{% endgithub_embed %}


### [Graph Edge Class Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-edge-class)

Change the line color and thickness of an edge.

{% github_embed "https://github.com/visallo/doc-examples/blob/ba76f66a/extension-graph-edge-class/src/main/resources/org/visallo/examples/graph_edge_class/plugin.js#L14-L21" %}{% endgithub_embed %}


### [Graph Edge Transformer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-edge-transformer)

Change the color of an edge (and edge arrow) based on value of `data` attribute.

{% github_embed "https://github.com/visallo/doc-examples/blob/ba76f66a/extension-graph-edge-transformer/src/main/resources/org/visallo/examples/graph_edge_transformer/plugin.js#L9-L15" %}{% endgithub_embed %}


### [Graph Node Transformer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-node-transformer)

Change the size of a node based on the value of a `data` attribute

{% github_embed "https://github.com/visallo/doc-examples/blob/ba76f66a/extension-graph-node-transformer/src/main/resources/org/visallo/examples/graph_node_transformer/plugin.js" %}{% endgithub_embed %}


### [Graph Node Decoration Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-node-decoration)

Add multiple entries for decorations.

{% github_embed "https://github.com/visallo/doc-examples/blob/ba76f66a/extension-graph-node-decoration/src/main/resources/org/visallo/examples/graph_node_decoration/plugin.js#L77-L100" %}{% endgithub_embed %}

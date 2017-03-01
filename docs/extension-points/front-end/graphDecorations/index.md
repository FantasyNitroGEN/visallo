# Graph Node Decoration

* [Graph Node Decoration JavaScript API `org.visallo.graph.node.decoration`](../../../javascript/org.visallo.graph.node.decoration.html)
* [Graph Node Decoration Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-node-decoration)

Graph node decorations are additional detail to display around a vertex when displayed in a graph. These decorations are implemented as [Cytoscape](http://js.cytoscape.org/) nodes inside of compound nodes. That allows them to be styled just like vertices using `org.visallo.graph.style` extensions.

<div class="alert alert-warning">
<p>Decorations can have performance impact on the graph.

<p>Once a node displays a decoration, another container node is created that is never removed. Also each decoration is a full cytoscape node.
</div>

## Alignment Positions

The figure below shows the available positions. The alignment locations are automatically adjusted based on the placement of the text in a node.

<img width=200 src="alignment-options.png">

Annotated positions and `alignment` configuration value:

1. Top left `{ h: 'left', v: 'top' }`
1. Top center `{ h: 'center', v: 'top' }`
1. Top right `{ h: 'right', v: 'top' }`
1. Center left `{ h: 'left', v: 'center' }`
1. Center center `{ h: 'center', v: 'center' }`
1. Center right `{ h: 'right', v: 'center' }`
1. Bottom left `{ h: 'left', v: 'bottom' }`
1. Bottom center `{ h: 'center', v: 'bottom' }`
1. Bottom right `{ h: 'right', v: 'bottom' }`

_**Note:** There is no collision detection on decorations with equal alignments_.

## Tutorial

<div style="text-align:center">
<img src="./decorations.png" width="100%" style="max-width: 400px;">
</div>

### Web Plugin

Register the plugin script in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/888381f7/extension-graph-node-decoration/src/main/java/org/visallo/examples/graph_node_decoration/GraphNodeDecorationWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Extension

Register the decoration extension for a new decoration in the top-left corner of nodes. This decoration will apply to all vertices that have a comment, and display the number of comments in the decoration.

{% github_embed "https://github.com/visallo/doc-examples/blob/d26ee807/extension-graph-node-decoration/src/main/resources/org/visallo/examples/graph_node_decoration/plugin.js#L34-L54" %}{% endgithub_embed %}

The default graph stylesheet defines `label` as the content of the node.

{% github_embed "https://github.com/visallo/visallo/blob/3709844f/web/plugins/graph-product/src/main/resources/org/visallo/web/product/graph/styles.js#L147" %}{% endgithub_embed %}

Register a style extension to format the decoration. All decorations have the `decoration` class, so append that to the selector to avoid conflicts with node classes.

{% github_embed "https://github.com/visallo/doc-examples/blob/888381f7/extension-graph-node-decoration/src/main/resources/org/visallo/examples/graph_node_decoration/plugin.js#L71-L83" %}{% endgithub_embed %}

## Popover Tutorial

Decorations can have popovers that are opened when the user clicks on the decoration using the `onClick` handler.

<div style="text-align:center">
<img src="./popover.png" width="100%" style="max-width: 325px;">
</div>

### Web Plugin

Register the popover component and template.

{% github_embed "https://github.com/visallo/doc-examples/blob/888381f7/extension-graph-node-decoration/src/main/java/org/visallo/examples/graph_node_decoration/GraphNodeDecorationWebAppPlugin.java#L18-L19" %}{% endgithub_embed %}

### Register Extension

Register the decoration with an `onClick` handler.

{% github_embed "https://github.com/visallo/doc-examples/blob/888381f7/extension-graph-node-decoration/src/main/resources/org/visallo/examples/graph_node_decoration/plugin.js#L59-L68" %}{% endgithub_embed %}

### Popover Component

Create the Flight component to render the popover.

{% github_embed "https://github.com/visallo/doc-examples/blob/888381f7/extension-graph-node-decoration/src/main/resources/org/visallo/examples/graph_node_decoration/popover.js" %}{% endgithub_embed %}

The `withPopover` mixin provides the popover specific handling to attach to the decoration.

Add the template with the necessary markup for the popover.

{% github_embed "https://github.com/visallo/doc-examples/blob/888381f7/extension-graph-node-decoration/src/main/resources/org/visallo/examples/graph_node_decoration/template.hbs" %}{% endgithub_embed %}

The popover mixin calls `setupWithTemplate` to initialize the popover, so if extra work is needed to be done after the template has rendered, use `after('setupWithTemplate')`. `this.dialog` and `this.popover` are instance variables for the popover and the content.

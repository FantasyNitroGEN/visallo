# Graph Layout

* [Graph Layout JavaScript API `org.visallo.graph.layout`](../../../javascript/org.visallo.graph.layout.html)
* [Graph Layout Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-layout)

Plugin to add [Cytoscape layouts](http://js.cytoscape.org/#layouts)

<div style="text-align:center">
<img src="./layout.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

### Web Plugin

Register resources for the plugin and message bundle.

{% github_embed "https://github.com/visallo/doc-examples/blob/ecf9eddca/extension-graph-layout/src/main/java/org/visallo/examples/graph_layout/GraphLayoutWebAppPlugin.java#L17-L18" %}{% endgithub_embed %}

### Register Extension

Register the layout extension.

{% github_embed "https://github.com/visallo/doc-examples/blob/ecf9eddca/extension-graph-layout/src/main/resources/org/visallo/examples/graph_layout/plugin.js#L36" %}{% endgithub_embed %}

### Create the Layout Class

The layout class is initialized with options for the layout. These options include the `cy` instance and the elements (`eles`) to layout. These should be filtered to real vertices `.filter('.v')` so we are not moving decorations.

{% github_embed "https://github.com/visallo/doc-examples/blob/ecf9eddca/extension-graph-layout/src/main/resources/org/visallo/examples/graph_layout/plugin.js#L3-L34" %}{% endgithub_embed %}

The positions are generated using a random number using the current window width. The use of `retina.pointsToPixels` allows transformation from virtual points to actual screen pixels in the <span class="no-glossary">case</span> of a hidpi display.

{% github_embed "https://github.com/visallo/doc-examples/blob/ecf9eddca/extension-graph-layout/src/main/resources/org/visallo/examples/graph_layout/plugin.js#L20-L23" %}{% endgithub_embed %}

### Message Bundle

Add a i18n value in a MessageBundle.properties. This will be displayed in the graph context menu.

{% github_embed "https://github.com/visallo/doc-examples/blob/ecf9eddca/extension-graph-layout/src/main/resources/org/visallo/examples/graph_layout/messages.properties#L1" %}{% endgithub_embed %}


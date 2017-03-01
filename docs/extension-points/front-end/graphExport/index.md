# Graph Export

* [Graph Export JavaScript API `org.visallo.graph.export`](../../../javascript/org.visallo.graph.export.html)
* [Graph Export Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-export)

Register a function that can add a menu option in export context menu. If multiple export plugins are registered, they are placed in an "Export" submenu.

<div style="text-align:center">
<img src="./menu.png" width="100%" style="max-width: 200px;">
<img src="./export.png" width="100%" style="max-width: 300px;">
</div>

## Tutorial

### Web Plugin

Register the resources need for export, a plugin, component, template, stylesheet, and message bundle.

{% github_embed "https://github.com/visallo/doc-examples/blob/d3e4a28e/extension-graph-export/src/main/java/org/visallo/examples/graph_export/GraphExportWebAppPlugin.java#L17-L21" %}{% endgithub_embed %}

### Register Extension

Register the extension to add a menu item to the graph context menu.

{% github_embed "https://github.com/visallo/doc-examples/blob/d3e4a28e/extension-graph-export/src/main/resources/org/visallo/examples/graph_export/plugin.js#L3-L6" %}{% endgithub_embed %}

### Define Component

Create the Flight component that logs the [Cytoscape](http://js.cytoscape.org/) `json` backup.

{% github_embed "https://github.com/visallo/doc-examples/blob/d3e4a28e/extension-graph-export/src/main/resources/org/visallo/examples/graph_export/configuration.js#L9-L19" %}{% endgithub_embed %}


### Style

Add some stylesheet declarations in a wrapper class to avoid collisions with other plugins.

{% github_embed "https://github.com/visallo/doc-examples/blob/d3e4a28e/extension-graph-export/src/main/resources/org/visallo/examples/graph_export/style.less", hideLines=['4-13', '17-23'] %}{% endgithub_embed %}


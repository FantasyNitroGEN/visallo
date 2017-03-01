# Graph Options

* [Graph Options JavaScript API `org.visallo.graph.options`](../../../javascript/org.visallo.graph.options.html)
* [Graph Options Example Code](https://github.com/visallo/doc-examples/tree/master/extension-graph-options)

Plugin to add custom options components (Flight or React) which display in the graph options menu (next to Fit) when the menu is opened.

<div style="text-align:center">
<img src="./options.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

For this tutorial we'll create a new options extension that adds a preferenced-backed checkbox. This could be used for toggling some built-in graph styles, for example.

### Web Plugin

Register the plugin script and React component in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/23baf73/extension-graph-options/src/main/java/org/visallo/examples/graph_options/GraphOptionsWebAppPlugin.java#L17-L18" %}{% endgithub_embed %}

### Register Extension

Register the options extension pointing to the React component.

{% github_embed "https://github.com/visallo/doc-examples/blob/23baf73/extension-graph-options/src/main/resources/org/visallo/examples/graph_options/plugin.js#L3-L6" %}{% endgithub_embed %}

### Component

Create the component that renders a checkbox, and looks up user preferences.

{% github_embed "https://github.com/visallo/doc-examples/blob/23baf73/extension-graph-options/src/main/resources/org/visallo/examples/graph_options/React.jsx#L1-L17" %}{% endgithub_embed %}

Implement the saving of the preference when the user clicks the checkbox. This updates the in memory user object, and updates the server.

{% github_embed "https://github.com/visallo/doc-examples/blob/23baf73/extension-graph-options/src/main/resources/org/visallo/examples/graph_options/React.jsx#L18-L24" %}{% endgithub_embed %}

* [`dataRequest API`](../../../javascript/module-dataRequest.html)
* [`User Service -> Set Preference API`](../../../javascript/module-services_user.html#.preference)

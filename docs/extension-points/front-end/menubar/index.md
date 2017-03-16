# Menu Bar

* [Menu Bar JavaScript API `org.visallo.menubar`](../../../javascript/org.visallo.menubar.html)
* [Menu Bar Example Code](https://github.com/visallo/doc-examples/tree/master/extension-menubar)

Add additional icons into the menu bar that can open a slide out panel or display a component in the content area like the built in dashboard.

The icon can refer to an existing icon [bundled with Visallo](https://github.com/visallo/visallo/tree/master/web/war/src/main/webapp/img/glyphicons/white), or one registered with `registerFile` in a plugin. For best results, use a white mono-chromatic icon.

<div style="text-align:center">
<img src="./menubar.png" width="100%" style="max-width: 250px;">
<img src="./welcome.png" width="100%" style="max-width: 450px;">
</div>

## Tutorial

### Web Plugin

Register the plugin, a component for the pane, and a template for the _Welcome to Visallo_ card.

{% github_embed "https://github.com/visallo/doc-examples/blob/3b6ac03e/extension-menubar/src/main/java/org/visallo/examples/menubar/MenubarWebAppPlugin.java#L17-L19" %}{% endgithub_embed %}

### Register Extension

Register the menu bar extension by pointing to the component and template. This one will use a bundled icon.

{% github_embed "https://github.com/visallo/doc-examples/blob/3b6ac03e/extension-menubar/src/main/resources/org/visallo/examples/menubar/plugin.js#L3-L16" %}{% endgithub_embed %}

### Component

Create a basic React component that displays some text and a button. Add some padding around the panel to match other panes.

{% github_embed "https://github.com/visallo/doc-examples/blob/3b6ac03e/extension-menubar/src/main/resources/org/visallo/examples/menubar/Pane.jsx" %}{% endgithub_embed %}


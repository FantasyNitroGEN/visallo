# Map Options

* [Map Options JavaScript API `org.visallo.map.options`](../../../javascript/org.visallo.map.options.html)
* [Map Options Example Code](https://github.com/visallo/doc-examples/tree/master/extension-map-options)

Add custom options components (Flight or React) which display in the map options menu (next to Fit).

<div style="text-align:center">
<img src="./options.png" width="100%" style="max-width: 450px;">
</div>

## Tutorial

### Web Plugin

Register the plugin, and component scripts. Then register a file reference to the [geojson](https://github.com/visallo/doc-examples/blob/6e8b6766/extension-map-options/src/main/resources/org/visallo/examples/map_options/countries.geojson) of countries.

{% github_embed "https://github.com/visallo/doc-examples/blob/6e8b6766/extension-map-options/src/main/java/org/visallo/examples/map_options/MapOptionsWebAppPlugin.java#L17-L19" %}{% endgithub_embed %}

### Register Extension

Register the map options extension and point the path the the React component.

{% github_embed "https://github.com/visallo/doc-examples/blob/6e8b6766/extension-map-options/src/main/resources/org/visallo/examples/map_options/plugin.js#L3-L6" %}{% endgithub_embed %}

### Component

The react component manages the state of the geojson layer (visible/hidden) using a user preference, and uses the OpenLayers API to add/remove the vector layer.

{% github_embed "https://github.com/visallo/doc-examples/blob/6e8b6766/extension-map-options/src/main/resources/org/visallo/examples/map_options/CountryBorders.jsx" %}{% endgithub_embed %}

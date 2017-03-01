# Dashboard Layout

* [Dashboard Layout JavaScript API `org.visallo.dashboard.layout`](../../../javascript/org.visallo.dashboard.layout.html)
* [Dashboard Layout Example Code](https://github.com/visallo/doc-examples/tree/master/extension-dashboard-layout)

Specifies the dashboard items, their sizes, and positions as the default dashboard configuration. Currently, only the first extension registered will be used. If no extension is registered, the system uses the default template as defined in [`defaultLayout.js`](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/dashboard/defaultLayout.js).

All dashboard cards are placed in a grid system. The default grid is `12` columns and unbounded rows. If `metrics` is not defined or overlaps a previous definition, then the layout system will place the card automatically.

<div class="alert alert-warning">
The console will show a warning if multiple extensions are found. The extension used is non-deterministic.
</div>

## Tutorial

### Web Plugin

Register the plugin in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/1ea91e17/extension-dashboard-layout/src/main/java/org/visallo/examples/dashboard_layout/DashboardLayoutWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Layout

A layout is just the extension and initial configuration metrics for the items.

{% github_embed "https://github.com/visallo/doc-examples/blob/1ea91e17/extension-dashboard-layout/src/main/resources/org/visallo/examples/dashboard_layout/plugin.js#L3-L12" %}{% endgithub_embed %}


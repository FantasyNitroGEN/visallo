# Dashboard Report Renderer

* [Dashboard Report Renderer JavaScript API `org.visallo.dashboard.reportrenderer`](../../../javascript/org.visallo.dashboard.reportrenderer.html)
* [Dashboard Report Renderer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-dashboard-reportrenderer)


Adds additional output types for dashboard items that define a `report` or `item.configuration.report`.

There are several built-in renderers defined in [`reportRenderers.js`](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/dashboard/reportRenderers.js).

<div style="text-align:center">
<img src="./visualizations.png" width="100%" style="max-width: 200px;">
</div>

## Tutorial

For this tutorial, we'll create a new JSON renderer that simply takes the result, formats it, then prints it.

<div style="text-align:center">
<img src="./jsonrenderer.png" width="100%" style="max-width: 400px;">
</div>

### Create Web Plugin

Register the resources to define the extension and the referenced component.

{% github_embed "https://github.com/visallo/doc-examples/blob/d69501f2/extension-dashboard-reportrenderer/src/main/java/org/visallo/examples/dashboard_reportrenderer/DashboardReportrendererWebAppPlugin.java#L16-L20" %}{% endgithub_embed %}

### Register the Extension

Register the new report renderer. It will accept any response.

{% github_embed "https://github.com/visallo/doc-examples/blob/d69501f2/extension-dashboard-reportrenderer/src/main/resources/org/visallo/examples/dashboard_reportrenderer/plugin.js#L3-L10" %}{% endgithub_embed %}

### Define the Renderer

Create the renderer component and include the mixin.

{% github_embed "https://github.com/visallo/doc-examples/blob/d69501f2/extension-dashboard-reportrenderer/src/main/resources/org/visallo/examples/dashboard_reportrenderer/renderer.js#L1-L8" %}{% endgithub_embed %}

Now, implement the processData and render functions

{% github_embed "https://github.com/visallo/doc-examples/blob/d69501f2/extension-dashboard-reportrenderer/src/main/resources/org/visallo/examples/dashboard_reportrenderer/renderer.js#L11-L23" %}{% endgithub_embed %}    

## Renderer Mixin

The custom report renderer can mixin [`dashboard/reportRenderers/withReportRenderer.js`](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/dashboard/reportRenderers/withRenderer.js) which provides:
* Automatically requesting data using endpoint configuration
* Handling refresh and reflow events
* Basic click handling if aggregations found
* Error handling

If the renderer uses the mixin, the only function required is `render`. Optionally, a `processData` function can be defined to transform the <span class="no-glossary">raw</span> server results. It's better to process the data in `processData` function instead of `render` because it will run once on `refreshData` events, instead of on every `reflow` event.

## Built-In Report Renderers

<style>
figure { clear: both; }
figure img { float: left; margin-right: 0.5em; } 
</style>

<figure>
    <img src="renderer-bar-h.png" width="200">
    <figcaption>
        <code>org-visallo-bar-horizontal</code>
        <p>Horizontal bar chart, also supports stacked bars if two aggregations provided.
    </figcaption>
</figure>

<figure>
    <img src="renderer-bar-v.png" width="200">
    <figcaption>
        <code>org-visallo-bar-vertical</code>
        <p>Vertical bar chart, also supports stacked bars if two aggregations provided.
    </figcaption>
</figure>

<figure>
    <img src="renderer-pie.png" width="200">
    <figcaption>
        <code>org-visallo-pie</code>
        <p>A classic pie chart.
    </figcaption>
</figure>

<figure>
    <img src="renderer-text-overview.png" width="200">
    <figcaption>
        <code>org-visallo-text-overview</code>
        <p>Text cards that show number and text.
    </figcaption>
</figure>

<figure>
    <img src="renderer-element-list.png" width="200">
    <figcaption>
        <code>org-visallo-element-list</code>
        <p>Standard list of elements, used in search results.
    </figcaption>
</figure>


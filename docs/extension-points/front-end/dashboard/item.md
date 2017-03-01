# Dashboard Item

* [Dashboard Item JavaScript API `org.visallo.dashboard.item`](../../../javascript/org.visallo.dashboard.item.html)
* [Dashboard Item Example Code](https://github.com/visallo/doc-examples/tree/master/extension-dashboard-item)

Dashboard items are user-selectable content placed on the dashboard. These could be charts using the `report` configuration or custom components in React or FlightJS.

<div style="text-align:center">
<img src="./pie.png" width="100%" style="max-width: 300px;">
<img src="./example-item.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

In this tutorial we will create two new dashboard items:

* Component that defines a report to show concept type counts 
* Custom React component that renders a number and configuration to increment

### Web Plugin

Register the resources.

{% github_embed "https://github.com/visallo/doc-examples/blob/7d006a2e/extension-dashboard-item/src/main/java/org/visallo/examples/dashboard_item/DashboardItemWebAppPlugin.java#L17-L22" %}{% endgithub_embed %}

### Register Extension

Register two extensions, a report-style card, and a custom component.

{% github_embed "https://github.com/visallo/doc-examples/blob/7d006a2e/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/plugin.js#L3-L31", hideLines=['8-21'] %}{% endgithub_embed %}

#### Report Configuration

The report configuration uses [Visallo element search](../../../javascript/org.visallo.dashboard.item.html#~reportParametersForSearch)

{% github_embed "https://github.com/visallo/doc-examples/blob/7d006a2e/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/plugin.js#L8-L21" %}{% endgithub_embed %}

### Create the Custom Component

The custom component will also register a configuration component. It will access a `count` <span class="no-glossary">property</span> in the configuration and display the current value.

{% github_embed "https://github.com/visallo/doc-examples/blob/7d006a2e/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/React.jsx#L11-L14" %}{% endgithub_embed %}

In the configuration component, we increment the count (creating if needed) when the button is clicked.

{% github_embed "https://github.com/visallo/doc-examples/blob/e013c675/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/Config.jsx#L15-L22" %}{% endgithub_embed %}

### Wiring Refresh

Dashboard triggers `refreshData` on all items when the user clicks the refresh button in the top-right corner. To wire this message in a React component we need the DOM Element of the item, so first register a `ref` in `render()`

{% github_embed "https://github.com/visallo/doc-examples/blob/1ea91e17/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/React.jsx#L23" %}{% endgithub_embed %}

Then, listen for the event, we must use Jquery to listen as Flight uses non-standard event triggering.

{% github_embed "https://github.com/visallo/doc-examples/blob/1ea91e17/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/React.jsx#L7-L15" %}{% endgithub_embed %}

Finally, unregister the listener on teardown

{% github_embed "https://github.com/visallo/doc-examples/blob/1ea91e17/extension-dashboard-item/src/main/resources/org/visallo/examples/dashboard_item/React.jsx#L16-L18" %}{% endgithub_embed %}

## Custom Configuration Interface Elements


<img src="config.png" width="450" style="float:right">

Provide a `configurationPath` to the extension to add an additional user interface component in the configure popover. The figure describes how the configuration interface is generated for the saved search dashboard item. 

The possible configuration can come from:
* Default configuration (edit title)
* Extension specific (configuration defined in extension `configurationPath`)
* Report configuration (choose which reportRenderer)
* Report chosen configuration (Report defined `configurationPath`)

The configuration component gets attributes of the item when opened.

* `extension` The extension registered
* `item` The item instance which includes `configuration`

To update an items configuration, trigger `configurationChanged` in FlightJS or call `configurationChanged` from `props` in React.

```js
// Flight Example

this.attr.item.configuration.myConfigOption = 'newValue';
this.trigger('configurationChanged', {
    extension: this.attr.extension,
    item: this.attr.item
});
```

```js
// React Example

var { item, extension } = this.props,
    configuration = { ...item.configuration, newStateValue: true };

item = { ...item, configuration }
this.props.configurationChanged({ item, extension })
```

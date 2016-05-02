## Dashboard Layout

The `org.visallo.web.dashboard.layout` extension specifies the dashboard items, their sizes, and positions as the default dashboard configuration. Currently, only the first extension registered will be used. If no extension is registered, the system uses the default template as defined in [`defaultLayout.js`](https://github.com/v5analytics/visallo/blob/master/web/war/src/main/webapp/js/dashboard/defaultLayout.js).

The extension accepts only an `[Array]` of card configurations.

```js
registry.registerExtension('org.visallo.web.dashboard.layout', [
    {
        extensionId: 'org-example-card-default',
        configuration: { metrics: { x: 0, y: 0, width: 6, height: 5 } }
    }
]);
```

### Configuration

* `extensionId` _(required)_ `[String]`: Reference to a `org.visallo.web.dashboard.item`'s `identifier`.
* `configuration` _(optional)_ `[Object]`: Customize the configuration for this item. 

### Metrics

All dashboard cards are placed in a grid system. The default grid is 12 columns and unbounded rows. If `metrics` is not defined or overlaps a previous definition, then the layout system will place the card automatically.


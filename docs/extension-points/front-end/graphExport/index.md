## Graph Export

Register a function that can add a menu option in export context menu.

## Configuration Options

* `menuItem` _(required)_ `[String]`: Export name that is displayed in context menu.
* `componentPath` _(required)_ `[String]`: FlightJS component attached in popover when menu item is activated.
* `attributes` _(optional)_ `[Function]`: Function to transform the attributes to send to component.

    * Function argument `attrs` with keys:
        * `workspaceId` `[String]` Workspace id
        * `exporter` `[Object]` This export extension config
        * `cy` `[Object]` The cytoscape object
* `showPopoverTitle` _(optional)_ `[Boolean]` Whether to show the popover title as _Export as [menuItem]_. Defaults to `true`
* `showPopoverCancel` _(optional)_ `[Boolean]` Whether to show the popover cancel button. Defaults to `true`

## Example

```js
define('com/example/myExport', ['flight/lib/component'], function(defineComponent) {
    return MyExport;
    function MyExport() { ... }
});

registry.registerExtension('org.visallo.graph.export', {
    menuItem: 'My Export',
    componentPath: 'com/example/myExport'
});
```

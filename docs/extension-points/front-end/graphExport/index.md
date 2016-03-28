## Graph Export

Register a function that can add a menu option in export context menu.

## Configuration Options

* `menuItem` `[String]`: Export name that is displayed in context menu.
* `componentPath` `[String]`: FlightJS component attached in popover when menu item is activated.

## Example

    define('com/example/myExport', ['flight/lib/component'], function(defineComponent) {
        return MyExport;
        function MyExport() { ... }
    })

    registry.registerExtension('org.visallo.graph.export', {
        menuItem: 'My Export',
        componentPath: 'com/example/myExport'
    });

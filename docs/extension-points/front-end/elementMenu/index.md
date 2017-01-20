Element Menu Plugin
===================

Plugin to add new items to vertex or edge context menu.

## Required parameters:

* label: The text to display.
* event: The event to trigger.

## Optional parameters:

* shortcut: string of shortcut to show in menu. Doesn't actually listen for shortcut, just places the text in the label.
* args: other values to pass to event handler.
* shouldDisable: function to disable / enable the field which will receive the following arguments:
    * `currentSelection`: object with currently selected element ids as keys and their element objects as values
    * `vertexId`: the vertex id of the click target's corresponding vertex
    * `edgeIds`: the edge ids of the click target's corresponding edges
    * `element`: the DOM element target
    * `vertex`: the corresponding vertex object of the click target
* selection: number of how many selected items this supports.
* options: (object)
    * insertIntoMenuItems: function to place the item in existing items.


## Example

To register an item:

```js
require([
    'configuration/plugins/registry',
    'util/messages'
], function(registry, i18n) {

    registry.registerExtension('org.visallo.vertex.menu', {
        label: i18n('com.myplugin.menu.label'),
        shortcut: 'alt+i',
        event: 'searchSimilar',
        selection: 2,
        options: {
            insertIntoMenuItems: function(item, items) {
                // Add item as fourth in list
                items.splice(3, 0, item);
            }
        }
    });
});
```

To create a `shouldDisable` handler:

```js
shouldDisable: function(currentSelection, vertexId, element, vertex) {
    // Disable this menu option if multiple vertices are selected
    return Object.keys(currentSelection).length > 1;
}
```

To add a divider:

```js
registry.registerExtension('org.visallo.vertex.menu', 'DIVIDER');
```

To add an edge context menu item:

```js
registry.registerExtension('org.visallo.edge.menu', ...);
```

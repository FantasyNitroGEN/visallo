Vertex Menu Plugin
=================

Plugin to add new items to vertex context menu.

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

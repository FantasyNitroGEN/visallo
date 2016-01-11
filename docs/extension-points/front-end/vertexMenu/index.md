Vertex Menu Plugin
=================

Plugin to add new items to vertex context menu

## Required parameters:

* label: The text to display
* event: The event to trigger

## Optional parameters:

* shortcut: string of shortcut to show in menu. Doesn't actually listen for shortcut, just places the text in the label.
* args: other values to pass to event handler
* shouldDisable: function to disable / enable the field
* selection: number of how many selected items this supports
* options: (object)
    * insertIntoMenuItems: function to place the item in existing items


## Example

To register an item:

        require([
            'configuration/plugins/vertexMenu/plugin',
            'util/messages'
        ], function(VertexMenuExtension, i18n) {

            VertexMenuExtension.registerVertexMenuItem({
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
        })


To add a divider:

        VertexMenuExtension.registerVertexMenuItem(VertexMenuExtension.DIVIDER);

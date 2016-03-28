# Detail Toolbar

Plugin to add toolbar items to the detail pane.

## Configuration

* `title` _(required)_ `[String]`: The text to display.
* `event` _(required)_ `[String]`: The event to trigger on click of toolbar item.
* `subtitle` _(optional)_ `[String]`: The text to display underneath the title.
* `cls` _(optional)_ `[String]`: A CSS classname to add to the items element. Add `disabled` to prevent events from firing.
* `canHandle` _(optional)_ `[Function]`: Whether this item should be added based on what's in the detail pane. If not provided, it assumes it can handle all inputs. Passed one argument `objects`, which is a list of all objects displayed in detail pane.
* `divider` _(optional)_ `[Boolean]`: Specify `true` for a toolbar menu divider instead of an actionable item.
* `submenu` _(optional)_ `[Array]`: Specify list of submenu toolbar items. Only one level supported.
* `right` _(optional)_ `[Boolean]`: Specify `true` to float item to the right.
* `options` _(optional)_ `[Object]`:
    * `insertIntoMenuItems` _(optional)_ `[Function]`: function to place the item in a specific location/order.

            insertIntoMenuItems: function(item, items) {
                // Insert item into specific position in items list
                items.splice(3, 0, item);
            }


## Example

To register an item:

        require([
            'configuration/plugins/registry',
            'util/messages'
        ], function(registry, i18n) {

            registry.registerExtension('org.visallo.detail.toolbar', {
                title: i18n('com.myplugin.toolbar.title'),
                subtitle: i18n('com.myplugin.toolbar.subtitle'),
                event: 'mypluginClick',
                canHandle: function(objects) {
                    return objects.length === 1 && objects[0].properties > 10;
                },
                options: {
                    insertIntoMenuItems: function(item, items) {
                        // Add item as fourth in list
                        items.splice(3, 0, item);
                    }
                }
            });
        })


To add a divider:

        registry.registerExtension('org.visallo.detail.toolbar', { divider: true });

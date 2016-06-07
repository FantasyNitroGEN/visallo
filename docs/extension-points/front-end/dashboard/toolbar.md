## Dashboard Toolbar Items

The `org.visallo.dashboard.toolbar.item` extension allows custom buttons to be rendered next to the cards configuration button. These buttons (displayed as icons) can send an event on click, or specify content to be rendered in a popover.

### Configuration Options

* `identifier` _(required)_ `[String]` Unique identifier for this type of toolbar item. Only used internally, not exposed to user.
* `icon` _(required)_ `[String]` Path to icon to render in button
* `action` _(required)_ `[Object]` The type of action when clicked
    * `type` _(required)_ `[String]` Must be either `popover`, or `event`
        * When type is `popover`
            * `componentPath` _(required)_ `[String]` Component path for content to render in popover
        * When type is `event`
            * `name` _(required)_ `[String]` Event to trigger
* `tooltip` _(optional)_ `[String]` Help text to display when user hovers over button
* `canHandle` _(optional)_ `[Function]` Function to decide if this item should be added to this card
    * The `canHandle` function is called with one `options` parameter with these keys:
        * `item` `[Object]` The dashboard item json
        * `extension` `[Object]` The dashboard extension json
        * `element`: `[Element]` The cards dom element

### Example

```js
registry.registerExtension('org.visallo.dashboard.toolbar.item', {
    identifier: 'com-example-toolbar',

    // Only add toolbar to my other custom card
    canHandle: function(options) {
        return options.extension.identifier === 'com-example-my-card'
    },
    tooltip: 'My Example Action',
    icon: 'myIcon.png',
    action: {
        type: 'popover',
        componentPath: 'com/example/toolbar/configComponent'
    }
});
```


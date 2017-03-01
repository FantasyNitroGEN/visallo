## Search Toolbar

Search toolbar items display below the search query input field. They have access to the current search query (if available), and can react to click events with content in a popover, or a custom event.

<img width=350 src="toolbar.png">

### Example

```js
    registry.registerExtension('org.visallo.search.toolbar', {
        tooltip: 'My Search Toolbar Item',
        icon: 'myIcon.png',
        canHandle: function(currentSearch) {
            // Only show the icon if there's a search
            if (currentSearch) return true;
            return false;
        },
        action: {
            type: 'popover',
            componentPath: 'com/example/js/content'
        }
    });
```


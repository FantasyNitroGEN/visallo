Graph Selector Plugin
=====================

Plugin to add custom node selection menu items. Graph provides select all, none, and invert by default.

To register a selector:

```js
require(['configuration/plugins/registry'], function(registry) {

    doRandomLayout.identifier = 'myRandomSelector';
    // optionally
    // doRandomLayout.visibility = 'always';

    registry.registerExtension('org.visallo.graph.selection', doRandomLayout);

    // Randomly select a node
    function doRandomLayout(cy) {
        var nodes = cy.nodes().unselect(),
            randomIndex = Math.floor(Math.random() * nodes.length);

        nodes[randomIndex].select();
    });
});
```

Optional configuration:

`visibility`: (String) Specifies when selector should display. One of: always, selected, none-selected.

Remember to add a i18n value in a MessageBundle.properties. This will be displayed in the graph context menu.

    graph.selector.[Selector Identifier Name].displayName=[String to display]

For example:

    graph.selector.myRandomSelector.displayName=Random Node

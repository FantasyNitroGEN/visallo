# Element Inspector Toolbar

* [Element Inspector Toolbar JavaScript API `org.visallo.detail.toolbar`](../../../javascript/org.visallo.detail.toolbar.html)
* [Element Inspector Toolbar Example Code](https://github.com/visallo/doc-examples/tree/master/extension-detail-toolbar)

Allows additional toolbar items to be added to the Element Inspector.

<div style="text-align:center">
<img src="./item.png" width="100%" style="max-width: 335px;">
</div>

## Tutorial

### Web Plugin

Register the resources needed.

{% github_embed "https://github.com/visallo/doc-examples/blob/ac5f5428/extension-detail-toolbar/src/main/java/org/visallo/examples/detail_toolbar/DetailToolbarWebAppPlugin.java#L17-L18" %}{% endgithub_embed %}

### Register Extension

Now, register the toolbar item.

{% github_embed "https://github.com/visallo/doc-examples/blob/ac5f5428/extension-detail-toolbar/src/main/resources/org/visallo/examples/detail_toolbar/plugin.js#L3-L10" %}{% endgithub_embed %}

### Listen

Register a document-level listener for the event specified in the extension. The [`formatters.vertex.title`](../../../javascript/module-formatters.vertex.html#.title) function transforms an element into a title string using the ontology title formula.

{% github_embed "https://github.com/visallo/doc-examples/blob/ac5f5428/extension-detail-toolbar/src/main/resources/org/visallo/examples/detail_toolbar/plugin.js#L12-L19" %}{% endgithub_embed %}

# Element List Item Renderer

* [Element List Item Renderer JavaScript API `org.visallo.entity.listItemRenderer`](../../../javascript/org.visallo.entity.listItemRenderer.html)
* [Element List Item Renderer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-entity-listitemrenderer)

This allows plugins to adjust how list items are displayed in search results, details panels, or anywhere else the lists are used.

<div style="text-align:center">
<img src="./items.png" width="100%" style="max-width: 500px;">
</div>

## Tutorial

### Web Plugin

Register the resources needed.

{% github_embed "https://github.com/visallo/doc-examples/blob/6a945993/extension-entity-listitemrenderer/src/main/java/org/visallo/examples/entity_listitemrenderer/EntityListItemRendererWebAppPlugin.java#L17-L21" %}{% endgithub_embed %}

### Register Extension

Now, register the item renderer. It will only override when the `usageContext` is `searchresults`. Some contexts send different input as the `item` parameter, so its recommended to accept specific contexts, rather than support all contexts.

{% github_embed "https://github.com/visallo/doc-examples/blob/74863513/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/plugin.js#L3-L8" %}{% endgithub_embed %}


### Create the Component

Create a Flight component that will render each row.

{% github_embed "https://github.com/visallo/doc-examples/blob/6a945993/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L8-L28", hideLines=['13-16'] %}{% endgithub_embed %}

Render the template using the item, and `formatters` to get the title.

{% github_embed "https://github.com/visallo/doc-examples/blob/6a945993/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L17-L23" %}{% endgithub_embed %}

Remember to set the `vertexId` in data of the element, for selection to work correctly.

{% github_embed "https://github.com/visallo/doc-examples/blob/6a945993/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L25" %}{% endgithub_embed %}


### Custom CSS

By default the element list uses fixed-height rows, this could be modified by using a CSS override.

{% github_embed "https://github.com/visallo/doc-examples/blob/6a945993/extension-entity-listitemrenderer/src/main/java/org/visallo/examples/entity_listitemrenderer/EntityListItemRendererWebAppPlugin.java#L21" %}{% endgithub_embed %}

{% github_embed "https://github.com/visallo/doc-examples/blob/6a945993/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/style.css" %}{% endgithub_embed %}

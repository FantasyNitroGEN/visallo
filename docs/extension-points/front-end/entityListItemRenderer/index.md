# Element List Item Renderer

* [Element List Item Renderer JavaScript API `org.visallo.entity.listItemRenderer`](../../../javascript/org.visallo.entity.listItemRenderer.html)
* [Element List Item Renderer Example Code](https://github.com/visallo/doc-examples/tree/master/extension-entity-listitemrenderer)

This allows plugins to adjust how list items are displayed in search results, details panels, or anywhere else the [Element List Component](../../../javascript/module-components_List.html) is used.

<div style="text-align:center">
<img src="./items.png" width="100%" style="max-width: 500px;">
</div>

## Tutorial

### Web Plugin

Register the resources needed.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/java/org/visallo/examples/entity_listitemrenderer/EntityListItemRendererWebAppPlugin.java#L17-L21" %}{% endgithub_embed %}

### Register Extension

Now, register the item renderer. It will only override when the `usageContext` is `searchresults`. Some contexts send different input as the `item` parameter, so its recommended to accept specific contexts, rather than support all contexts.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/plugin.js#L3-L8" %}{% endgithub_embed %}

### Create the Component

Create a Flight component that will render each row.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js", hideLines=['12-16', '23-35', '38-50'] %}{% endgithub_embed %}

Render the template using the item, and [`formatters.vertex.title`](../../../javascript/module-formatters.vertex.html#.title) to get the title.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L26-L30" %}{% endgithub_embed %}

Remember to set the `vertexId` (or `edgeId`) in data of the element, for selection to work correctly.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L35" %}{% endgithub_embed %}

To display an image in the row, wait for the `loadPreview` event that notifies the component that it has scrolled into view. As this event might be called many times, we ensure `onLoadPreview` is only ever called once using underscore.js `_.once`.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L33" %}{% endgithub_embed %}

The image path returned from [`formatters.vertex.image`](../../../javascript/module-formatters.vertex.html#.image) function might be the concept icon, check if it is using [`formatters.vertex.imageIsFromConcept`](../../../javascript/module-formatters.vertex.html#.imageIsFromConcept) is so we can style it differently.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L38-L50" %}{% endgithub_embed %}

### Custom CSS

To customize styling, add a class to the `node`.

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/component.js#L25" %}{% endgithub_embed %}

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/style.less#L1-L29", hideLines=['2-6', '13-17', '20-23'] %}{% endgithub_embed %}

When the row is selected the list element will have an `active` class. Certain elements may need to adjust to be visible with the blue selection background. The example changes the image border color:

{% github_embed "https://github.com/visallo/doc-examples/blob/05d911d4/extension-entity-listitemrenderer/src/main/resources/org/visallo/examples/entity_listitemrenderer/style.less#L31-L35" %}{% endgithub_embed %}

# Layout Component

* [Layout Component JavaScript API `org.visallo.layout.component`](../../../javascript/org.visallo.layout.component.html)
* [Layout Component Example Code](https://github.com/visallo/doc-examples/tree/master/extension-layout-component)


The Element Inspector is rendered using a custom layout engine consisting of a tree of layout components. This layout engine can be used elsewhere to display elements using different `contexts` also.

*Layout Components* are nodes in the layout tree that define the type of layout and what children are included in the layout. 

The `children` defined in a layout component can be references (`ref`) to other components, or FlightJS components specified with a `componentPath`. Layout components can also specify a flight component to be attached to the node for implementing behavior.


<div style="text-align:center">
<img src="./inspector.png" width="100%" style="max-width: 366px;">
<img src="./popup.png" width="100%" style="max-width: 360px;">
</div>

## Tutorial

This tutorial will use layout components to override the body of all elements to include a new section above properties. Another extension will be created to modify the body of `popup` views in the graph (open from the entity's context menu or long press on the entity to activate.)

<div style="text-align:center">
<img src="./components.png" width="100%" style="max-width: 822px;">
</div>

### Web Plugin

Register the plugin script.

{% github_embed "https://github.com/visallo/doc-examples/blob/0b62ae91/extension-layout-component/src/main/java/org/visallo/examples/layout_component/LayoutComponentWebAppPlugin.java#L17" %}{% endgithub_embed %}

### Register Extension

The extension will need to replace the `org.visallo.layout.body` layout component with a custom one. All plugins layout components have precedence over the built-in components unless the plugin component uses a less specific `applyTo`.

{% github_embed "https://github.com/visallo/doc-examples/blob/0b62ae91/extension-layout-component/src/main/resources/org/visallo/examples/layout_component/plugin.js#L3-L16" %}{% endgithub_embed %}

The component is identical to the one built-in to visallo with an additional child prepended.

{% github_embed "https://github.com/visallo/visallo/blob/3709844f/web/war/src/main/webapp/js/detail/item/layoutComponents/generic.js#L64-L72" %}{% endgithub_embed %}

The additional component uses a `ref` to reference a helper string component. The string component renders whatever model is passed to it as a string. The only modifications are an additional class that we use to style.

{% github_embed "https://github.com/visallo/doc-examples/blob/0b62ae91/extension-layout-component/src/main/resources/org/visallo/examples/layout_component/style.less#L11-L18" %}{% endgithub_embed %}

### Popup Extension

The popup extension overrides the same identifier `org.visallo.layout.body`, except with constraints and context of the graph popoup.

{% github_embed "https://github.com/visallo/doc-examples/blob/0b62ae91/extension-layout-component/src/main/resources/org/visallo/examples/layout_component/plugin.js#L19-L31" %}{% endgithub_embed %}

This extension uses a custom `componentPath` instead of a `ref`. The component is a FlightJS component that renders how many properties are on the element and a button to search for related entities. The component triggers `positionDialog` because it renders async. This event will reposition the popup based on its size.

{% github_embed "https://github.com/visallo/doc-examples/blob/0b62ae91/extension-layout-component/src/main/resources/org/visallo/examples/layout_component/popupBody.js#L27-L38" %}{% endgithub_embed %}

All popups have interaction disabled with `pointer-events: none`, so one change is needed to make the button clickable. Add a `pointer-events: all` to the button in a stylesheet.

{% github_embed "https://github.com/visallo/doc-examples/blob/0b62ae91/extension-layout-component/src/main/resources/org/visallo/examples/layout_component/style.less#L4-L9" %}{% endgithub_embed %}

## String Component

`org.visallo.layout.text` is defined as a helper to render string models. The model passed to it is transformed to a string using `String(model)`. You can also specify a text `style`, which sets a css class with builtin text styling.

Valid Style options: `title`, `subtitle`, `heading1`, `heading2`, `heading3`, `body`, `footnote`

```js
children: [
    {
        ref: 'org.visallo.layout.text',
        model: 'hello world',
        style: 'title'
    }
]
```

## CollectionItem

Instead of setting a fixed number of `children`, specify `collectionItem` to render a dynamic number of child elements based on the model. For each item in a model array, the collection item is duplicated as a child. This requires the model to be an array, or an error is thrown.

```js
registry.registerExtension('org.visallo.layout.component', {
    identifier: 'com.example.using.collection',
    children: [
        { ref: 'com.example.my.collection', model: ['First', 'Second'] }
    ]
});

registry.registerExtension('org.visallo.layout.component', {
    identifier: 'com.example.my.collection',
    // model: function(model) { /* optionally transform model */ return model; },
    collectionItem: { ref: 'org.visallo.layout.text' }
});
```

```html
<!-- Output -->
<div>First</div>
<div>Second</div>
```

### Using the Layout

The layout can be used to render any single element or list of items, it's useful to specify context to allow custom rendering for that instance.

To initialize the renderer, attach the `Item` flight component to a DOM element, and pass a model object.

```js
require(['detail/item/item'], function(Item) {
    Item.attachTo(domElement, {
        model: model,
        // Optional Array
        constraints: ['width'],
        // Optional String
        context: 'mycontext'
    });
});
```

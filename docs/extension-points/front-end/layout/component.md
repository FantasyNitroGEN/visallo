## Layout Component

The detail pane is rendered using a custom layout engine consisting of a tree of layout components. 

*Layout Components* are nodes in the layout tree that define the type of layout and what children are included in the layout. They are defined using the extension point `org.visallo.layout.component`.

The `children` defined in a layout component can be references (`ref`) to other components, or FlightJS components specified with a `componentPath`. Layout components can also specify a flight component to be attached to the node for implementing behavior.


### Configuration Options

* `applyTo` _(optional)_ `[Object|Function]` One of:

    * `Object` 

        *Only one of these allowed: conceptIri, edgeLabel, displayType, type*

        * `conceptIri`: `[String]` Implies vertices, only those whose concept (or ancestor) matches this Iri.
        * `edgeLabel`: `[String]` Implies edges, only those whose edgeLabel matches this Iri.
        * `displayType`: `[String]` Match ontological `displayType` option.
        * `type`: `[String]` Possible values: `vertex`, `edge`, `element`, `element[]`.
        * `constraints`: `[Array]` Possible values: `width`, `height`. Match based on the view container, instead of matching model data.
            Constraints control the layout selection for views that are width, and/or height constrained. The detail pane is set to be width constrained, whereas the full screen view has no constraints.
        * `contexts`: `[String]` For named templates. `popup` is defined by default for graph previews.

    * `Function` Set a function to determine the layout container for a specific model. If it returns true it will take precedence over any other applyTo specification. The function parameters are `model`, the current model in tree, and `match` which contains `constraints`, or `context` or both.

    ```js
    function(model, match) {
        return isHandled;
    }
    ```
        
    
* `identifier` _(required)_ `[string]` Identifier of this component for use in other components in package syntax. Also transforms into css class – replacing package periods with dashes.
* `layout` _(optional)_ `[Object]`
    * `type` _(required)_ `[String]` Which layout type to render `children`. 
    * `options` _(optional)_ `[Object]` Layout-specific options
* `componentPath` _(optional)_ `[String]` Additional FlightJS component to attach to this node for behavior.
* `className` _(optional)_ `[String]` Additional classname to add to DOM
* _Exactly one of the following is required: `render`, `collectionItem` or `children` (i.e., you cannot supply both render and collectionItem)._
    * `render` _(required*)_ `[Function]` Function that renders the content, is passed the `model`, and `match` configuration.
    * `collectionItem` _(required*)_ `[Object]` Reference to layout component to render for each item in model (requires model to be array.)
    * `children` _(required*)_ `[Array]`
        * `style` _(optional)_ `[Object]` CSS attributes to set on DOM.
        * `modelAttribute` _(optional)_ `[String]` Use this attribute name instead of `model` when attaching FlightJS component.
        * `attributes` _(optional)_ `[Function]` Transform attributes using function when attaching FlightJS component.
        * `model` _(optional)_ `[Function|Object]` Change the model passed to this child, either a function or object.
        
        	* `Function`: Transforms the state of the model at this level in the tree to the child. Return either the transformed model or a `Promise`.
        	* `Object`: Static object to pass as the model
        	
        * _Exactly one of the following is required: `ref`, or `componentPath`._

            * `ref` _(required*)_ `[String]` Reference to identifier of layout component to render
                - or -
            * `componentPath` _(required*)_ `[String]` RequireJS path to FlightJS component to render

### Example

This example creates a root component for all vertices that are derived from `http://visallo.org#thing`. It defines four children that render in flexbox column layout. The first is a reference (`ref`) to another layout component, while the others are FlightJS components.

```js
registry.registerExtension('org.visallo.layout.component', {
    identifier: 'org.visallo.detail.root',
    applyTo: { conceptIri: 'http://visallo.org#thing' },
    layout: { type: 'flex' options: { direction: 'column' } },
    children: [
        { ref: 'org.visallo.detail.header' },
        { componentPath: 'toolbar' },
        { componentPath: 'properties', model: function(v) { return v.properties; } },
        { componentPath: 'relationships' }
    ]
});
```

Here the header layout component is defined with children for the title and concept of vertex.

```js
registry.registerExtension('org.visallo.layout.component', {
    identifier: 'org.visallo.detail.header',
    layout: { type: 'flex', direction: 'column' },
    // Draw entity image in background
    componentPath: 'detail/components/header',
    children: [

        // org.visallo.layout.text is built in for displaying simple strings
        { ref: 'org.visallo.layout.text', model: F.vertex.title },
        {
            ref: 'org.visallo.layout.text',
            model: function(v) {
                // Model transformers can return promises
                return Promise.require('util/vertex/formatters')
                    .then(function(F) {
                        return F.vertex.concept(v).displayName;
                    });
            }
        }
    ]
});
```

### String Component

`org.visallo.layout.text` is defined as a helper to render string models. The model passed to it is transformed to a string using `String(model)`. You can also specify a text `style`, which sets a css class with builtin text styling.

```js
children: [
    {
        ref: 'org.visallo.layout.text',
        model: 'hello world',
        style: 'title'
    }
]
```

Valid Style options: `title`, `subtitle`, `heading1`, `heading2`, `heading3`, `body`, `footnote`

### CollectionItem Example

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

To initialize the renderer, attach the `Item` flight component to a dom element, and pass a model object.

```js
require(['detail/item/item'], function(Item) {
    Item.attachTo(domElement, {
        model: model,
        // Optional [Array]
        constraints: ['width'],
        // Optional [String]
        context: 'mycontext'
    });
});
```

### Layout Engine Psuedocode

This is the flow that the layout engine takes when passed a model and match constraints/contexts.

1. A `model` object is requested to render using optional `constraints` and `context`.
2. The layout engine finds matching components using:
    1. Find all possible components with `org.visallo.layout.root` identifier. That match optionally specified `context` and `constraints`.
    2. Call `applyTo` functions (if available)
    3. If no matches, displayType is used to match components with `applyTo: { displayType: '[a display type]'}`.
    4. If no matches, Concept/Edge Types are used to match components with `applyTo` having keys of `conceptIri|edgeLabel` set to iri.
    5. If no matches, Check concept/edge ancestors for `applyTo` match
    6. If no matches, Check type for `applyTo` match
    7. If no matches, Check components with no defined `applyTo`
    8. If no matches, throw error
3. Initialize all children
4. Initialize new `Layout Type` according to `layout.type` and `layout.options` configuration
5. Render children using layout
    1. If child includes `ref`, Recursively repeat `2` using this identifier (as opposed to root.)
    2. If child includes `layout` and `children`, Recursively repeat `3` using this configuration.
    3. If child includes `componentPath`, require and attach it to the node.


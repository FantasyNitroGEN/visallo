## Layout Component

The detail pane is rendered using a custom layout engine consisting of layout components. 

Layout Components are nodes in the layout tree that define the type of layout and what children are included in the layout. Defined using the extension point `org.visallo.layout.component`.


### Configuration Options

* `applyTo` _(optional)_ `[object|function]` One of: `{ [concept|edgeLabel|displayType]: [string] }`, or `function(model) { return isHandled; }`
* `identifier` _(required)_ `[string]` Identifier of this component for use in other components in package syntax. Also transforms into css class (replacing /./-/g)
* `layout` _(optional)_ `[object]`
    * `type` _(required)_ `[string]` Layout type to render `children`. 
    * `options` _(optional)_ `[object]` Layout-specific options
* `componentPath` _(optional)_ `[string]` Additional component to render in this node for behavior
* `className` _(optional)_ `[string]` Additional classname to add to DOM
* _Only one of `render`, `collectionItem`, or `children` is required/allowed:_
    * `render` _(required*)_ `[function]` Function that renders the content, is passed the model
    * `collectionItem` _(required*)_ `[object]` Item to render for each item in model (requires model to be array)
    * `children` _(required*)_ `[array]`
        * `style` _(optional)_ `[object]` Css attributes on DOM
        * `modelAttribute` _(optional)_ `[string]` Use this attribute name instead of `model`
        * `attributes` _(optional)_ `[function]` Transform attributes using function
        * `model` _(optional)_ `[function]` Function that transforms the current components model to the model for this child
        * _One of the following:_
            * `ref` _(optional)_ `[string]` Reference to identifier of layout component to render
                - or -
            * `componentPath` _(optional)_ `[string]` RequireJS path of component to render


### Example

Creates a root component for all vertices derived from `http://visallo.org#thing`. Has 4 children that render in flexbox column layout.

    registry.registerExtension('org.visallo.layout.component', {
        identifier: 'org.visallo.detail.root',
        applyTo: { concept: 'http://visallo.org#thing' },
        layout: { type: 'flex' options: { direction: 'column' } },
        children: [
            { ref: 'org.visallo.detail.header' },
            { componentPath: 'toolbar' },
            { componentPath: 'properties', model: function(v) { return v.properties; } },
            { componentPath: 'relationships' }
        ]
    });

Define the header layout component to be title and concept of vertex.

    registry.registerExtension('org.visallo.layout.component', {
        identifier: 'org.visallo.detail.header',
        layout: { type: 'flex', direction: 'column' },
        // Draw entity image in background
        componentPath: 'detail/components/header',
        children: [
            // Assume F is required from util/vertex/formatters
            // org.visallo.layout.text is built in for displaying simple strings
            { identifier: 'org.visallo.layout.text', model: F.vertex.title },
            {
                identifier: 'org.visallo.layout.text',
                model: function(v) {
                    return F.vertex.concept(v).displayName;
                } 
            }
        ]
    });

### Collection Item Example

Setting a `collectionItem` instead of children will duplicate the `collectionItem` object for every item in `model`. Requires model to be an array.

    registry.registerExtension('org.visallo.layout.component', {
        identifier: 'com.example.using.collection',
        children: [
            { ref: 'com.example.my.collection', model: ['First', 'Second'] }
        ]
    })

    registry.registerExtension('org.visallo.layout.component', {
        identifier: 'com.example.my.collection',
        // model: function(model) { /* optionally transform model */ return model; },
        collectionItem: { ref: 'org.visallo.layout.text' }
    })

    // Output
    <div>First</div>
    <div>Second</div>


### Layout Engine Psuedocode

1. JSON object given to layout engine
2. Engine finds matches calling `applyTo` functions (if available) on all registered `Layout Component`s with identifier of `org.visallo.layout.root` and checking for truthiness.
    1. If no matches, Concept/Edge Types are used to match components with `applyTo` set to iri. (and identifier matches)
    2. If no matches, Check concept/edge ancestors for `applyTo` match (and identifier matches)
    3. If no matches, Check components with no defined `applyTo` (and identifier matches)
    4. If no matches, throw error
3. Initialize all children
4. Initialize new `Layout Type` according to `layout.type` and `layout.options` configuration
5. Render children using layout
    1. If child includes `ref`, Recursively repeat `2` using this identifier
    2. If child includes `layout` and `children`, Recursively repeat `3` using this configuration.
    3. If child includes `componentPath`, require and render it


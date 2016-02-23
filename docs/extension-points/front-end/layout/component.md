## Layout Component

The detail pane is rendered using a custom layout engine consisting of a tree of layout components. 

Layout Components are nodes in the layout tree that define the type of layout and what children are included in the layout. Defined using the extension point `org.visallo.layout.component`.

The children defined in a layout component can be references (`ref`) to other components, or flight components specified with a `componentPath`. Components themselves can also specify a flight component to be attached to the node.


### Configuration Options

* `applyTo` _(optional)_ `[Object|Function]` One of:

    * `Object` 

        *Only one of these allowed: conceptIri, edgeLabel, displayType, type*

        * `conceptIri`: `[String]` Implies vertices, only those whose concept (or ancestor) matches this Iri
        * `edgeLabel`: `[String]` Implies edges, only those whose edgeLabel matches this Iri
        * `displayType`: `[String]` Match ontological displayType option
        * `type`: `[String]` vertex, edge, element, element[]
        * `constraints`: `[Array]` width, height. Instead of matching model data, match based on view container
            Constraints control the layout selection for views that are width, and/or height constrained. The detail pane is set to be width constrained, whereas the full screen view has no constraints.
        * `contexts`: `[String]` For named templates


    * `Function` Set a function to determine the layout container for a specific model. If it returns true it will take precedence over any other applyTo specification.
    
                function(model) {
                    return isHandled;
                }
        
    
* `identifier` _(required)_ `[string]` Identifier of this component for use in other components in package syntax. Also transforms into css class (replacing /./-/g)
* `layout` _(optional)_ `[Object]`
    * `type` _(required)_ `[String]` Layout type to render `children`. 
    * `options` _(optional)_ `[Object]` Layout-specific options
* `componentPath` _(optional)_ `[String]` Additional component to render in this node for behavior
* `className` _(optional)_ `[String]` Additional classname to add to DOM
* _Only one of `render`, `collectionItem`, or `children` is required/allowed:_
    * `render` _(required*)_ `[Function]` Function that renders the content, is passed the model
    * `collectionItem` _(required*)_ `[Object]` Item to render for each item in model (requires model to be array)
    * `children` _(required*)_ `[Array]`
        * `style` _(optional)_ `[Object]` Css attributes on DOM
        * `modelAttribute` _(optional)_ `[String]` Use this attribute name instead of `model`
        * `attributes` _(optional)_ `[Function]` Transform attributes using function
        * `model` _(optional)_ `[Function]` Function that transforms the current components model to the model for this child
        * _One of the following:_
            * `ref` _(optional)_ `[String]` Reference to identifier of layout component to render
                - or -
            * `componentPath` _(optional)_ `[String]` RequireJS path of component to render

### Example

Creates a root component for all vertices derived from `http://visallo.org#thing`. Has 4 children that render in flexbox column layout.

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

Define the header layout component to be title and concept of vertex.

    registry.registerExtension('org.visallo.layout.component', {
        identifier: 'org.visallo.detail.header',
        layout: { type: 'flex', direction: 'column' },
        // Draw entity image in background
        componentPath: 'detail/components/header',
        children: [
            // Assume F is required from util/vertex/formatters
            // org.visallo.layout.text is built in for displaying simple strings
            { ref: 'org.visallo.layout.text', model: F.vertex.title },
            {
                ref: 'org.visallo.layout.text',
                model: function(v) {
                    return F.vertex.concept(v).displayName;
                } 
            }
        ]
    });

### String Component

'org.visallo.layout.text' is defined as a helper to render string models. The model passed to it is transformed to a `String(model)`. You can also specify a text `style`, which sets a css class with builtin text styling.

        children: [
            {
                ref: 'org.visallo.layout.text',
                model: 'hello world',
                style: 'title'
            }
        ]

Valid Style options: `title`, `subtitle`, `heading1`, `heading2`, `heading3`, `body`, `footnote`

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
2. Engine begins finding component matches:
    1. Find possible components that match `org.visallo.layout.root` identifier, model, constraints, and context if specified.
    2. Call `applyTo` functions (if available) on all registered `Layout Component`s
    3. If no matches, displayType is used to match components with `applyTo: { displayType: '[a display type]'}`.
    4. If no matches, Concept/Edge Types are used to match components with `applyTo` having keys of `conceptIri|edgeLabel` set to iri.
    5. If no matches, Check concept/edge ancestors for `applyTo` match
    6. If no matches, Check type for `applyTo` match
    7. If no matches, Check components with no defined `applyTo`
    8. If no matches, throw error
3. Initialize all children
4. Initialize new `Layout Type` according to `layout.type` and `layout.options` configuration
5. Render children using layout
    1. If child includes `ref`, Recursively repeat `2` using this identifier
    2. If child includes `layout` and `children`, Recursively repeat `3` using this configuration.
    3. If child includes `componentPath`, require and render it


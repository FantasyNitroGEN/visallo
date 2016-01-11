## Layout Component

The detail pane is rendered using a custom layout engine consisting of layout components. 

Layout Components are nodes in the layout tree that define the type of layout and what children are included in the layout. Defined using the extension point `org.visallo.layout.component`.


### Configuration Options

* `applyTo` (optional) [object|function] One of: `{ [concept|edgeLabel|displayType]: [string] }`, or `function(model) { return isHandled; }`
* `identifier` (required) [string] Identifier of this component for use in other components in package syntax. Also transforms into css class (replacing /./-/g)
* `layout` (required) [object]
    * `type` (required) [string] Layout type to render `children`. 
    * `options` (optional) [object] Layout-specific options
* `componentPath` (optional) [string] Additional component to render in this node for behavior
* `className` (optional) [string] Additional classname to add to DOM
* `children` (required) [array]
    * `style` (optional) [object] Css attributes on DOM
    * `modelAttribute` (optional) [string] Use this attribute name instead of `model`
    * `attributes` (optional) [function] Transform attributes using function
    * `model` (optional) [function] Function that transforms the current components model to the model for this child
    * Finally, one of the following:
        * `ref` (optional) [string] Reference to identifier of layout component to render
            - or -
        * `componentPath` (optional) [string] RequireJS path of component to render


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


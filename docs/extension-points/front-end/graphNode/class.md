## Graph Node Class

Register a function that can add or remove classes from cytoscape nodes for custom styling.

## Function Arguments

* `vertex` `[Object]`: Vertex that this cytoscape node represents.
* `classes` `[Array]`: List of classes that will be added to cytoscape node.

## Example

    registry.registerExtension('org.visallo.graph.node.class', function(vertex, classes) {
        if (vertex.properties.length > 10) {
            classes.push('many_properties')
        }
    });

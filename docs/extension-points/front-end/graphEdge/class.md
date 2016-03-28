## Graph Edge Class

Register a function that can add or remove classes from cytoscape edges for custom styling.

## Function Arguments

* `edges` `[Array]`: List of edges that are collapsed into the drawn line. `length >= 1`.
* `type` `[String]`: EdgeLabel of the collapsed edges.
* `classes` `[Array]`: List of classes that will be added to cytoscape edge.

## Example

    registry.registerExtension('org.visallo.graph.edge.class', function(edges, type, classes) {
        if (type === 'myEdgeType') {
            classes.push('myEdge')
        }
    });

## Graph Node Transformer

Register a function that can modify the cytoscape node data.

## Function Arguments

* `vertex` `[Object]`: Vertex Object.
* `data` `[Object]`: Cytoscape node data object.

## Example

    registry.registerExtension('org.visallo.graph.node.transformer', function(vertex, data) {
        var customProp = _.findWhere(vertex.properties, { name: 'http://example.com#customProp' });
        if (customProp) {
            data.custPropValue = customProp.value;
        }
    });

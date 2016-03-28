
## Graph Edge Transformer

Register a function that can modify the cytoscape edge data.

## Function Arguments

* `data` `[Object]`: Edge cytoscape data object.

    Default data object:

        {
            id: [cyEdgeId],
            type: [edgeLabel],
            source: [sourceCyNodeId],
            target: [targetCyNodeId],
            label: '[edgeLabel displayName] ([collapse count])'
            edges: [Array of collapsed edges]
        }

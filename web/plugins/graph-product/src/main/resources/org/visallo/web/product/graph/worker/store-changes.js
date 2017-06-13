define([
    'data/web-worker/store/product/selectors'
], function(productSelectors) {

     publicData.storePromise.then(store => {
         checkUpdatedEdgesForProductInclusion(store);
     })

    function checkUpdatedEdgesForProductInclusion(store) {
        var prevEdges;
        store.subscribe(function() {
            const state = store.getState();
            const workspaceId = state.workspace.currentId;

            if (workspaceId && state.element[workspaceId]) {
                const newEdges = state.element[workspaceId].edges;
                if (prevEdges !== newEdges) {
                    productSelectors.getProducts(state).forEach(product => {
                        if (product.extendedData) {
                            const { vertices, edges } = product.extendedData;
                            const addEdges = {};
                            _.each(newEdges, (edge, id) => {
                                if (edge !== null && (!(id in edges) || !edges[id].inVertexId || !edges[id].outVertexId)) {
                                    if (edge.inVertexId in vertices && edge.outVertexId in vertices) {
                                        addEdges[id] = {
                                            edgeId: id,
                                            ..._.pick(edge, 'inVertexId', 'outVertexId', 'label')
                                        };
                                    }
                                }
                            })
                            if (!_.isEmpty(addEdges)) {
                                _.defer(() => {
                                    store.dispatch({
                                        type: 'PRODUCT_ADD_EDGE_IDS',
                                        payload: { workspaceId, productId: product.id, edges: addEdges }
                                    })
                                })
                            }
                        }
                    })
                }
                prevEdges = newEdges;
            }
        })
    }
});

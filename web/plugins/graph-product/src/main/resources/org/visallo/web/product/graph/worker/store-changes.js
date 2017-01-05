define([
    'data/web-worker/store/product/selectors'
], function(productSelectors) {

    // publicData.storePromise.then(store => {
    //     checkUpdatedEdgesForProductInclusion(store);
    // })

    function checkUpdatedEdgesForProductInclusion(store) {
        var prevEdges;
        store.subscribe(function() {
            const state = store.getState();
            const workspaceId = state.workspace.currentId;

            if (workspaceId && state.element[workspaceId]) {
                const newEdges = state.element[workspaceId].edges;
                if (prevEdges !== newEdges) {
                    productSelectors.getProducts(state).forEach(item => {
                        if (item.extendedData) {
                            const { vertices, edges } = item.extendedData;
                            const verticesById = _.indexBy(vertices, 'id');
                            const edgesById = _.indexBy(edges, 'edgeId');
                            const addEdges = [];
                            _.each(newEdges, (edge, id) => {
                                if (edge !== null && !(id in edgesById)) {
                                    if (edge.inVertexId in verticesById && edge.outVertexId in verticesById) {
                                        addEdges.push({
                                            edgeId: id,
                                            ..._.pick(edge, 'inVertexId', 'outVertexId', 'label')
                                        });
                                    }
                                }
                            })
                            if (addEdges.length) {
                                _.defer(() => {
                                    store.dispatch({
                                        type: 'PRODUCT_ADD_EDGE_IDS',
                                        payload: { workspaceId, productId: item.id, edges: addEdges }
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

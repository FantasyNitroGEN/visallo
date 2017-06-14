define([
    'data/web-worker/store/product/selectors'
], function(productSelectors) {

     publicData.storePromise.then(store => {
         var prevEdges, prevVertices;

         store.subscribe(function() {
             const state = store.getState();
             const workspaceId = state.workspace.currentId;

             if (workspaceId && state.element[workspaceId]) {
                 const { edges: newEdges, vertices: newVertices } = state.element[workspaceId];

                 if (prevEdges !== newEdges) {
                    checkUpdatedEdgesForProductInclusion(store.dispatch, state);
                 }
                 if (prevVertices !== newVertices) {
                    updateVisibleCollapsedNodes(store.dispatch, state);
                 }

                 prevEdges = newEdges;
                 prevVertices = newVertices;
             }
         });
     })

    function checkUpdatedEdgesForProductInclusion(dispatch, state) {
        const workspaceId = state.workspace.currentId;
        const newEdges = state.element[workspaceId].edges;

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
                        dispatch({
                            type: 'PRODUCT_ADD_EDGE_IDS',
                            payload: { workspaceId, productId: product.id, edges: addEdges }
                        })
                    })
                }
            }
        })
    }

    function updateVisibleCollapsedNodes(state) {}

//    function updateVisibleCollapsedNodes(store) {
//TODO update visibility of collapsedNodes (may also could do it in graph reducer?)
//        var prevVertices;
//        store.subscribe(function() {
//            const state = store.getState();
//            const workspaceId = state.workspace.currentId;
//
//            if (workspaceId && state.element[workspaceId]) {
//                const newVertices = state.element[workspaceId].vertices;
//                if (prevVertices !== newVertices) {
//                    productSelectors.getProducts(state)
//                        .filter(product => product.kind === 'org.visallo.web.product.graph.GraphWorkProduct')
//                        .forEach(product => {
//                            if (product.extendedData) {
//                                const { vertices, collapsedNodes } = product.extendedData.vertices;
//                                const addEdges = {};
//                                _.each(newEdges, (edge, id) => {
//                                    if (edge !== null && (!(id in edges) || !edges[id].inVertexId || !edges[id].outVertexId)) {
//                                        if (edge.inVertexId in vertices && edge.outVertexId in vertices) {
//                                            addEdges[id] = {
//                                                edgeId: id,
//                                                ..._.pick(edge, 'inVertexId', 'outVertexId', 'label')
//                                            };
//                                        }
//                                    }
//                                })
//                                if (!_.isEmpty(addEdges)) {
//                                    _.defer(() => {
//                                        store.dispatch({
//                                            type: 'PRODUCT_ADD_EDGE_IDS',
//                                            payload: { workspaceId, productId: product.id, edges: addEdges }
//                                        })
//                                    })
//                                }
//                            }
//                        })
//                }
//
//                prevEdges = newEdges;
//                prevVertices = newVertices;
//            }
//        })
//    }
});
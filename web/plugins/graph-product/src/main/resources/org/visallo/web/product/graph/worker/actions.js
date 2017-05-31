define(['data/web-worker/store/actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'org/visallo/web/product/graph/dist/actions-impl',
        actions: {
            updateRootId: (productId, nodeId) => ({ productId, nodeId }),
            snapToGrid: (snap) => ({ snap }),
            updatePositions: (productId, updateVertices, { undoable }) => ({ productId, updateVertices, undoable }),
            dropElements: (productId, elements, position) => ({ productId, elements, position }),
            removeElements: (productId, elements, undoable) => ({ productId, elements, undoable }),
            removeGhost: (id) => ({ id }),
            addRelated: (productId, vertices) => ({ productId, vertices }),

            collapseNodes: (productId, collapseData, { undoable }) => ({ productId, collapseData, undoable }),
            uncollapseNodes: (productId, collapsedNodeIds, { undoable }) => ({ productId, collapsedNodeIds, undoable })
        }
    })
})

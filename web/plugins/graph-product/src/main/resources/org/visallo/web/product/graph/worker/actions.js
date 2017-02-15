define(['data/web-worker/store/actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'org/visallo/web/product/graph/dist/actions-impl',
        actions: {
            snapToGrid: (snap) => ({ snap }),
            updatePositions: (productId, updateVertices, { undoable }) => ({ productId, updateVertices, undoable }),
            dropElements: (productId, elements, position) => ({ productId, elements, position }),
            removeGhost: (id) => ({ id }),
            addRelated: (productId, vertices) => ({ productId, vertices })
        }
    })
})

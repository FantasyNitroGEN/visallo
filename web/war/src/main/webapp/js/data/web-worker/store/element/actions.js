define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/element/actions-impl',
        actions: {
            get: ({ workspaceId, vertices, edges }) => ({ workspaceId, vertices, edges }),
            setFocus: ({ vertexIds, edgeIds, elementIds }) => ({ vertexIds, edgeIds, elementIds }),
            updateElement: (workspaceId, vertex) => ({ workspaceId, vertex })
        }
    })
})


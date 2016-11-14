define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/workspace/actions-impl',
        actions: {
            setCurrent: (workspaceId) => ({ workspaceId })
        }
    })
})


define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/ontology/actions-impl',
        actions: {
            get: (workspaceId) => ({ workspaceId }),
            addConcept: (concept) => ({ concept })
        }
    })
})


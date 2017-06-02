define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/ontology/actions-impl',
        actions: {
            get: (workspaceId) => ({ workspaceId }),
            addConcept: (concept, { key, workspaceId } = {}) => ({ concept, key, workspaceId }),
            addRelationship: (relationship, { key, workspaceId } = {}) => ({ relationship, key, workspaceId }),
            addProperty: (property, { key, workspaceId } = {}) => ({ property, key, workspaceId })
        }
    })
})


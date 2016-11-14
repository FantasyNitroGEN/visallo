define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/configuration/actions-impl',
        actions: {
            get: (locale) => ({ locale })
        }
    })
})


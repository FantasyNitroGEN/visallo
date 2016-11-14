define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/panel/actions-impl',
        actions: {
            setPadding: ({ top, left, right, bottom }) => ({ top, left, right, bottom })
        }
    })
})


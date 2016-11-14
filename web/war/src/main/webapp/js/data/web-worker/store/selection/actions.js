define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/selection/actions-impl',
        actions: {
            add: (selection) => ({ selection }),
            remove: (selection) => ({ selection }),
            set: (selection) => ({ selection }),
            clear: () => ({})
        }
    })
})

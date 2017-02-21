define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/undo/actions-impl',
        actions: {
            undoForProduct: () => {},
            redoForProduct: () => {}
        }
    });
});

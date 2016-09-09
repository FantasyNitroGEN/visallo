define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/screen/actions-impl',
        actions: {
            setPixelRatio: (pixelRatio) => ({ pixelRatio })
        }
    })
})


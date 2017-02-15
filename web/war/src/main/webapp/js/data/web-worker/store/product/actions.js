define(['../actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'data/web-worker/store/product/actions-impl',
        actions: {
            list: (initialProductId) => ({ initialProductId }),
            get: (productId) => ({ productId }),
            create: (title, kind, params) => ({ title, kind, params }),
            select: (productId) => ({ productId }),
            delete: (productId) => ({ productId }),
            updateTitle: (productId, title) => ({ productId, title }),
            updateData: (productId, key, value) => ({ productId, key, value }),
            updateExtendedData: (productId, key, value) => ({ productId, key, value }),

            updatePreview: (productId, dataUrl) => ({ productId, dataUrl }),
            updateViewport: (productId, { pan, zoom }) => ({ productId, pan, zoom }),
            removeElements: (productId, elements, { undoable } = {}) => ({ productId, elements, undoable }),
            selectAll: (productId) => ({ productId })
        }
    })
});

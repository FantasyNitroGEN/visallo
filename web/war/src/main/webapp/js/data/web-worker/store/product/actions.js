define(['../actions', './selectors', 'configuration/plugins/registry'], function(actions, selectors, registry) {
    actions.protectFromWorker();

    const productActions = actions.createActions({
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
            updateLocalData: (productId, key, value) => ({ productId, key, value }),

            updatePreview: (productId, dataUrl) => ({ productId, dataUrl }),
            updateViewport: (productId, { pan, zoom }) => ({ productId, pan, zoom }),
            selectAll: (productId) => ({ productId })
        }
    });

    const productExtendedActions = {
        removeElements: (productId, elements, { undoable } = {}) => {
            visalloData.storePromise.then(store => {
                const products = selectors.getProducts(store.getState());
                const product = products.find(product => product.id === productId);
                const productExtensions = registry.extensionsForPoint('org.visallo.workproduct');
                const extension = productExtensions.find(e => e.identifier === product.kind);

                if (_.isFunction(extension.storeActions.removeElements)) {
                    store.dispatch(extension.storeActions.removeElements(productId, elements, undoable));
                } else if (actions.isValidAction(extension.storeActions.removeElements)) {
                    store.dispatch(extension.storeActions.removeElements);
                }
            });
        }
    };

    return {
        ...productActions,
        ...productExtendedActions
    }
});

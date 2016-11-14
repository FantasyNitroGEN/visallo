define([
    '../actions',
    '../../util/ajax',
    '../element/actions-impl',
    '../selection/actions-impl',
    './selectors'
], function(actions, ajax, elementActions, selectionActions, selectors) {
    actions.protectFromMain();

    const api = {
        get: ({productId, invalidate }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const { products } = state.product.workspaces[workspaceId];
            const product = products[productId];
            var request;

            if (invalidate || !product || !product.extendedData) {
                request = ajax('GET', '/product', {
                    productId,
                    includeExtended: true,
                    params: {
                        includeVertices: true,
                        includeEdges: true
                    }
                })
            }

            if (request) {
                request.then(function(product) {
                    dispatch(api.update(product))

                    const { vertices, edges } = JSON.parse(product.extendedData)
                    const vertexIds = _.pluck(vertices, 'id');
                    const edgeIds = _.pluck(edges, 'edgeId');

                    dispatch(elementActions.get({ workspaceId, vertexIds, edgeIds }));
                })
            }
        },

        previewChanged: ({ productId, workspaceId, md5 }) => (dispatch, getState) => dispatch({
            type: 'PRODUCT_PREVIEW_UPDATE',
            payload: { productId, md5, workspaceId }
        }),

        changedOnServer: (productId) => (dispatch, getState) => {
            // TODO: Should check current workspace
            // not current ? just mark it invalid
            // is current  ? is it selected ? get : load list
            dispatch(api.get({ productId, invalidate: true }));
        },

        update: (product) => ({
            type: 'PRODUCT_UPDATE',
            payload: {
                product
            }
        }),

        updatePreview: ({ productId, dataUrl }) => (dispatch, getState) => {
            ajax('POST', '/product', { productId, preview: dataUrl })
        },

        updateViewport: ({ productId, pan, zoom }) => (dispatch, getState) => dispatch({
            type: 'PRODUCT_UPDATE_VIEWPORT',
            payload: {
                productId,
                viewport: { pan, zoom },
                workspaceId: getState().workspace.currentId
            }
        }),

        removeElements: ({ productId, elements }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const workspace = state.workspace.byId[workspaceId];
            if (workspace.editable && elements && elements.vertexIds && elements.vertexIds.length) {
                const removeVertices = elements.vertexIds;
                dispatch({
                    type: 'PRODUCT_REMOVE_ELEMENTS',
                    payload: { elements: { vertexIds: removeVertices }, productId, workspaceId }
                })
                dispatch(selectionActions.remove({
                    selection: { vertices: removeVertices }
                }));
                if (removeVertices.length) {
                    ajax('POST', '/product', { productId, params: { removeVertices }})
                }
            }
        },

        selectAll: ({ productId }) => (dispatch, getState) => {
            var state = getState(),
                workspaceId = state.workspace.currentId,
                product = state.product.workspaces[workspaceId].products[productId];

            dispatch(selectionActions.set({
                selection: {
                    vertices: product.extendedData.vertices.map(v => v.id),
                    edges: product.extendedData.edges.map(e => e.id)
                }
            }));
        },

        list: ({ initialProductId }) => function handler(dispatch, getState) {
            const state = getState();
            const workspaceId = state.workspace.currentId
            const workspaceProduct = state.product.workspaces[workspaceId]

            if (!workspaceId) {
                _.delay(handler, 250, dispatch, getState);
                return
            }

            if (!workspaceProduct || (!workspaceProduct.loaded && !workspaceProduct.loading)) {
                dispatch({ type: 'PRODUCT_LIST', payload: { loading: true, loaded: false, workspaceId } })
                ajax('GET', '/product/all').then(({types, products}) => {
                    dispatch({type: 'PRODUCT_UPDATE_TYPES', payload: { types }})
                    dispatch({type: 'PRODUCT_LIST', payload: { workspaceId, loading: false, loaded: true, products }})
                    if (initialProductId) {
                        dispatch(api.select({ productId: initialProductId }))
                    } else if (!getState().product.workspaces[workspaceId].selected) {
                        dispatch(api.select({ productId: null }))
                    }
                })
            }
        },

        create: ({title, kind}) => (dispatch, getState) => {
            const products = selectors.getProducts(getState())
            const shouldSelectProduct = _.isEmpty(products);

            ajax('POST', '/product', { title, kind })
                .then(product => {
                    dispatch(api.update(product))
                    dispatch(api.select({ productId: product.id }))
                })
        },

        delete: ({ productId }) => (dispatch) => {
            return ajax('DELETE', '/product', { productId })
                .then(() => dispatch(api.remove(productId)));
        },

        updateTitle: ({ productId, title }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const product = state.product.workspaces[workspaceId].products[productId];
            const { kind } = product;

            dispatch({
                type: 'PRODUCT_UPDATE_TITLE',
                payload: { productId, loading: true, workspaceId }
            })
            ajax('POST', '/product', { title, kind, productId })
                .then(result => {
                    dispatch({
                        type: 'PRODUCT_UPDATE_TITLE',
                        payload: { loading: false, productId, workspaceId, title }
                    })
                });
        },

        remove: (productId) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const { products, selected } = state.product.workspaces[workspaceId];
            const product = products[productId];
            if (product) {
                dispatch({
                    type: 'PRODUCT_REMOVE',
                    payload: { productId, workspaceId }
                });
                if (productId === selected) {
                    dispatch(api.select({ productId: null }));
                }
            }
        },

        select: ({ productId }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;

            if (!productId) {
                const nextProduct = _.first(selectors.getProducts(state));
                if (nextProduct) {
                    productId = nextProduct.id;
                }
            }

            dispatch({
                type: 'PRODUCT_SELECT',
                payload: { workspaceId, productId }
            })
        }

    }

    return api;
})

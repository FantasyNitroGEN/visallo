define([
    'data/web-worker/store/actions',
    'data/web-worker/store/product/actions-impl',
    'data/web-worker/store/product/selectors',
    'data/web-worker/store/selection/actions-impl',
    'data/web-worker/util/ajax'
], function(actions, productActions, productSelectors, selectionActions, ajax) {
    actions.protectFromMain();

    const api = {
        dropElements: ({ productId, elements, undoable }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const { vertexIds, edgeIds } = elements;
            // TODO: get edges from store first
            var edges = (edgeIds && edgeIds.length) ? (
                ajax('POST', '/edge/multiple', { edgeIds })
                    .then(function({ edges }) {
                        return _.flatten(edges.map(e => [e.inVertexId, e.outVertexId]));
                    })
                ) : Promise.resolve([]);

            edges.then(function(edgeVertexIds) {
                const product = productSelectors.getProductsById(getState())[productId];
                const existing = product.extendedData ? Object.keys(product.extendedData.vertices) : [];
                const combined = _.without(_.uniq(edgeVertexIds.concat(vertexIds)), ..._.pluck(existing, 'id'));
                if (!combined.length) return;

                let undoPayload = {};
                if (undoable) {
                    undoPayload = {
                        undoScope: productId,
                        undo: {
                            productId,
                            elements: { vertexIds: combined }
                        },
                        redo: {
                            productId,
                            elements
                        }
                    };
                }

                dispatch({
                    type: 'PRODUCT_MAP_ADD_ELEMENTS',
                    payload: { workspaceId, productId, vertexIds, ...undoPayload }
                });

                ajax('POST', '/product', {
                    productId,
                    params: {
                        updateVertices: _.object(combined.map(id => [id, {}]))
                    }
                })
                dispatch(productActions.select({ productId }));
            })
        },

        removeElements: ({ productId, elements, undoable }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const workspace = state.workspace.byId[workspaceId];
            if (workspace.editable && elements && elements.vertexIds && elements.vertexIds.length) {
                const product = state.product.workspaces[workspaceId].products[productId];
                const byId = _.indexBy(product.extendedData.vertices, 'id');

                let undoPayload = {};
                if (undoable) {
                    undoPayload = {
                        undoScope: productId,
                        undo: {
                            productId,
                            elements
                        },
                        redo: {
                            productId,
                            elements
                        }
                    };
                }

                dispatch({
                    type: 'PRODUCT_MAP_REMOVE_ELEMENTS',
                    payload: {
                        elements,
                        productId,
                        workspaceId,
                        ...undoPayload
                    }
                });
                dispatch(selectionActions.remove({
                    selection: { vertices: elements.vertexIds }
                }));

                if (elements.vertexIds.length) {
                    ajax('POST', '/product', { productId, params: { removeVertices: elements.vertexIds }})
                }
            }
        }
    };

    return api;
})


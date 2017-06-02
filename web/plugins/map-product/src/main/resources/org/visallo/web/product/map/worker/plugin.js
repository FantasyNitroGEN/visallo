define([
    'configuration/plugins/registry',
    'updeep',
    'org/visallo/web/product/map/dist/actions-impl'
], function(registry, u, actions) {

    registry.registerExtension('org.visallo.store', {
        key: 'product',
        reducer: function(state, { type, payload }) {
            switch (type) {
                case 'PRODUCT_MAP_ADD_ELEMENTS': return addElements(state, payload);
                case 'PRODUCT_MAP_REMOVE_ELEMENTS': return removeElements(state, payload);
            }

            return state;
        },
        undoActions: {
            PRODUCT_MAP_ADD_ELEMENTS: {
                undo: (undo) => actions.removeElements(undo),
                redo: (redo) => actions.dropElements(redo)
            },
            PRODUCT_MAP_REMOVE_ELEMENTS: {
                undo: (undo) => actions.dropElements(undo),
                redo: (redo) => actions.removeElements(redo)
            }
        }
    })

    function addElements(state, { workspaceId, productId, vertexIds }) {
        const product = state.workspaces[workspaceId].products[productId];
        const vertices = product && product.extendedData && product.extendedData.vertices;
        if (vertices) {
            return u({
                workspaces: {
                    [workspaceId]: {
                        products: {
                            [productId]: {
                                extendedData: {
                                    vertices: u.constant(_.extend({}, vertices, vertexIds))
                                }
                            }
                        }
                    }
                }
            }, state);
        }

        return state;
    }

    function removeElements(state, { workspaceId, productId, elements }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            extendedData: {
                                vertices: u.omitBy(v => elements.vertexIds.includes(v.id))
                            }
                        }
                    }
                }
            }
        }, state);
    }
});

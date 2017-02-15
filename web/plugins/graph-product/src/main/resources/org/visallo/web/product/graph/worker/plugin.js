define([
    'configuration/plugins/registry',
    'updeep',
    'org/visallo/web/product/graph/dist/actions-impl'
], function(registry, u, actions) {

    registry.registerExtension('org.visallo.store', {
        key: 'product',
        reducer: function(state, { type, payload }) {
            switch (type) {
                case 'PRODUCT_GRAPH_SET_POSITIONS': return updatePositions(state, payload);
                case 'PRODUCT_ADD_EDGE_IDS': return addEdges(state, payload);
            }

            return state;
        },
        undoActions: {
            PRODUCT_GRAPH_SET_POSITIONS: {
                undo: (undo) => actions.undoSetPositions(undo),
                redo: (redo) => actions.redoSetPositions(redo)
            },
            PRODUCT_REMOVE_ELEMENTS: {
                undo: (undo) => actions.undoRemoveElements(undo),
                redo: (redo) => actions.redoRemoveElements(redo)
            }
        }
    })
    registry.registerExtension('org.visallo.store', {
        key: 'org-visallo-graph',
        reducer: function(state, { type, payload }) {
            if (!state) return { animatingGhosts: {} }
            switch (type) {
                case 'PRODUCT_GRAPH_ADD_GHOSTS': return addGhosts(state, payload);
                case 'PRODUCT_GRAPH_REMOVE_GHOST': return removeGhost(state, payload);
            }

            return state;
        }
    })

    function addGhosts(state, { ids, position }) {
        return u({
            animatingGhosts: _.object(ids.map(id => [id, u.constant(position)]))
        }, state)
    }
    function removeGhost(state, { id }) {
        return u({ animatingGhosts: u.omit(id) }, state);
    }

    function addEdges(state, { productId, edges, workspaceId }) {
        const product = state.workspaces[workspaceId].products[productId];
        if (product && product.extendedData && product.extendedData.edges) {
            let newIndex = product.extendedData.edges.length;
            const byId = _.indexBy(product.extendedData.edges, 'edgeId')
            const update = _.object(_.compact(edges.map(edgeInfo => {
                if (edgeInfo.edgeId in byId) return;
                return [newIndex++, edgeInfo];
            })));

            return u.updateIn(`workspaces.${workspaceId}.products.${productId}.extendedData.edges`, update, state);
        }

        return state;
    }

    function updatePositions(state, { workspaceId, productId, updateVertices }) {
        const product = state.workspaces[workspaceId].products[productId];

        if (product && product.extendedData && product.extendedData.vertices) {
            const updatedIds = [];
            var updated = u.updateIn(
                `workspaces.${workspaceId}.products.${productId}.extendedData.vertices.*`,
                function(vertexPosition) {
                    if (vertexPosition.id in updateVertices) {
                        const pos = updateVertices[vertexPosition.id];
                        updatedIds.push([vertexPosition.id]);
                        return {
                            id: vertexPosition.id,
                            pos
                        }
                    }
                    return vertexPosition;
                },
                state
            );

            const additionalVertices = _.omit(updateVertices, updatedIds)
            if (!_.isEmpty(additionalVertices)) {
                var nextIndex = product.extendedData.vertices.length;
                const additions = _.object(_.map(additionalVertices, (pos, id) => {
                    return [nextIndex++, { id, pos }];
                }));

                updated = u.updateIn(
                    `workspaces.${workspaceId}.products.${productId}.extendedData.vertices`,
                    additions,
                    updated
                )
            }

            return updated;
        }

        return state;
    }
});

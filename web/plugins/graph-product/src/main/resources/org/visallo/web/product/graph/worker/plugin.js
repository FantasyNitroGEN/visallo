define([
    'configuration/plugins/registry',
    'updeep',
    'org/visallo/web/product/graph/dist/actions-impl'
], function(registry, u, actions) {

    registry.registerExtension('org.visallo.store', {
        key: 'product',
        reducer: function(state, { type, payload }) {
            switch (type) {
                case 'PRODUCT_GRAPH_SET_POSITIONS': return updateOrAddElements(state, payload);
                case 'PRODUCT_GRAPH_REMOVE_ELEMENTS': return removeElements(state, payload);
                case 'PRODUCT_ADD_EDGE_IDS': return addEdges(state, payload);
            }

            return state;
        },
        undoActions: {
            PRODUCT_GRAPH_SET_POSITIONS: {
                undo: (undo) => actions.undoSetPositions(undo),
                redo: (redo) => actions.redoSetPositions(redo)
            },
            PRODUCT_GRAPH_REMOVE_ELEMENTS: {
                undo: (undo) => actions.undoRemoveElements(undo),
                redo: (redo) => actions.redoRemoveElements(redo)
            },
            PRODUCT_GRAPH_COLLAPSE_NODES: {
                undo: (undo) => actions.uncollapseNodes(undo),
                redo: (redo) => actions.collapseNodes(redo)
            },
            PRODUCT_GRAPH_UNCOLLAPSE_NODES: {
                undo: (undo) => actions.collapseNodes(undo),
                redo: (redo) => actions.uncollapseNodes(redo)
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
            //TODO: change this from returning [edgeInfos] to {edgeInfos}, then we should correctly update product.extendedData.edges from WS messages
            return u.updateIn(`workspaces.${workspaceId}.products.${productId}.extendedData.edges`, )
            const byId = _.indexBy(product.extendedData.edges, 'edgeId')
            const update = _.object(_.compact(edges.map(edgeInfo => {
                if (edgeInfo.edgeId in byId) return;
                return [newIndex++, edgeInfo];
            })));

            return u.updateIn(`workspaces.${workspaceId}.products.${productId}.extendedData.edges`, update, state);
        }

        return state;
    }

    function updateOrAddElements(state, { workspaceId, productId, updateVertices }) {
        const product = state.workspaces[workspaceId].products[productId];

        if (product && product.extendedData && product.extendedData.vertices) {
            const updatedIds = [];
            var updated = u.updateIn(
                `workspaces.${workspaceId}.products.${productId}.extendedData.vertices`,
                function(elements) { return applyUpdates(elements, updatedIds) },
                state
            );
            updated = u.updateIn(
                `workspaces.${workspaceId}.products.${productId}.extendedData.compoundNodes`,
                function(elements) { return applyUpdates(elements, updatedIds) },
                updated
            );

            const additionalVertices = _.omit(updateVertices, updatedIds)
            if (!_.isEmpty(additionalVertices)) {
                updated = u.updateIn(
                    `workspaces.${workspaceId}.products.${productId}.extendedData.vertices`,
                    function(elements) { return addElements(elements, additionalVertices, 'vertex') },
                    updated
                )
                updated = u.updateIn(
                    `workspaces.${workspaceId}.products.${productId}.extendedData.compoundNodes`,
                    function(elements) { return addElements(elements, additionalVertices, 'compoundNode') },
                    updated
                )
            }

            return updated;
        }

        return state;

        function applyUpdates(elements, updatedIds) {
            return _.mapObject(elements, (element) => {
                if (element.id in updateVertices) {
                    updatedIds.push(element.id);
                    return updateVertices[element.id];
                }
                return element;
            })
        }

        function addElements(elements, adding, type) {
            Object.keys(adding).forEach(id => {
                const newElement = adding[id];
                if (newElement.type === type) {
                    elements = {
                        ...elements,
                        [id]: newElement
                    };
                }
            });
            return elements;
        }
    }

    function removeElements(state, { workspaceId, productId, elements }) {
        const { vertexIds, edgeIds, collapsedNodeIds } = elements;
        const updates = {};

        if (vertexIds) updates.vertices = u.omitBy(v => vertexIds.includes(v.id));
        if (edgeIds) updates.edges = u.omitBy(e => edgeIds.includes(e.edgeId));
        if (collapsedNodeIds) updates.compoundNodes = u.omitBy(c => collapsedNodeIds.includes(c.id));

        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            extendedData: updates
                        }
                    }
                }
            }
        }, state);
    }
});

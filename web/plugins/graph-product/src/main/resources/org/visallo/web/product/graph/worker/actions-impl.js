define([
    'underscore',
    'data/web-worker/store/actions',
    'data/web-worker/util/ajax',
    'data/web-worker/store/product/actions-impl',
    'data/web-worker/store/product/selectors',
    'data/web-worker/store/user/actions-impl',
    'data/web-worker/store/element/actions-impl',
    'data/web-worker/store/selection/actions-impl',
    'data/web-worker/store/workspace/actions-impl',
    './snap'
], function(
    _,
    actions,
    ajax,
    productActions,
    productSelectors,
    userActions,
    elementActions,
    selectionActions,
    workspaceActions,
    snapPosition) {
    actions.protectFromMain();

    const workspaceReadonly = (state) => !state.workspace.byId[state.workspace.currentId].editable;

    const api = {
        updateRootId: ({ productId, nodeId }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const product = state.product.workspaces[workspaceId].products[productId];

            productActions.updateLocalData(productId, 'rootId', nodeId);
        },

        snapToGrid: ({ snap }) => (dispatch, getState) => {
            const state = getState();
            if (workspaceReadonly(state)) {
                return;
            }

            const workspaceId = state.workspace.currentId;
            const productId = state.product.workspaces[workspaceId].selected;
            if (productId) {
                const product = state.product.workspaces[workspaceId].products[productId];
                if (product && product.extendedData && snap) {
                    const updateVertices = _.object(product.extendedData.vertices.map(vPos => {
                        return [vPos.id, vPos.pos];
                    }));
                    dispatch(api.setPositions({ productId, updateVertices, snapToGrid: snap }));
                }

                dispatch(
                  userActions.setUserPreference({
                      name: 'snapToGrid',
                      value: String(snap)
                  })
                );
            }
        },

        removeGhost: ({ id }) => ({
            type: 'PRODUCT_GRAPH_REMOVE_GHOST',
            payload: { id }
        }),

        setPositions: ({ productId, updateVertices, snapToGrid, undoable }) => (dispatch, getState) => {
            const state = getState();
            if (workspaceReadonly(state)) {
                return;
            }

            const workspaceId = state.workspace.currentId;
            const product = state.product.workspaces[workspaceId].products[productId];
            const productNodes = { ...product.extendedData.vertices, ...product.extendedData.compoundNodes };
            const byType = _.groupBy(updateVertices, (update) => update.type || 'vertex');
            const newVertices = _.pick(updateVertices, (update, id) => !(id in productNodes));
            const addingNewVertices = Object.keys(newVertices).length > 0;

            updateVertices = _.mapObject(updateVertices, ({ pos, ...rest }) => {
                return {
                    pos: _.mapObject(pos, v => Math.round(v)),
                    ...rest
                }
            });

            let undoPayload = {};
            if (undoable) {
                undoPayload = {
                    undoScope: productId,
                    undo: {
                        productId
                    },
                    redo: {
                        productId,
                        updateVertices
                    }
                };
                if (Object.keys(updateVertices).length !== newVertices.length) {
                    undoPayload.undo.updateVertices = _.chain(updateVertices)
                                                        .mapObject((update, id) => productNodes[id])
                                                        .pick((node) => !!node)
                                                        .value();
                }
                if (addingNewVertices) {
                    undoPayload.undo.removeElements = {
                        vertexIds: Object.keys(byType, 'vertex'),
                        collapsedNodeIds: Object.keys(byType, 'compoundNode')
                    };
                }
            }

            dispatch({
                type: 'PRODUCT_GRAPH_SET_POSITIONS',
                payload: {
                    productId,
                    updateVertices,
                    workspaceId,
                    ...undoPayload
                }
            });

            if (snapToGrid) {
                updateVertices = _.mapObject(updateVertices, ({ pos, ...rest }) => ({ pos: snapPosition(pos), ...rest }));
                dispatch({
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        updateVertices,
                        workspaceId
                    }
                });
            }

            return ajax('POST', '/product/graph/vertices/update', { productId, updates: updateVertices }).then(() => {
                if (addingNewVertices) {
                    return ajax('GET', '/product', { productId,
                        includeExtended: true,
                        params: {
                            includeVertices: true,
                            includeEdges: true
                        }
                    }).then(product => {
                        dispatch(productActions.update(product));

                        const { edges } = product.extendedData;
                        const vertexIds = Object.keys(byType.vertex);
                        const edgeIds = _.pluck(edges, 'edgeId');
                        return dispatch(elementActions.get({ workspaceId, vertexIds, edgeIds }));
                    });
                }
            });
        },

        undoSetPositions: ({ productId, updateVertices, removeElements }) => (dispatch, getState) => {
            if (updateVertices) {
                dispatch(api.updatePositions({ productId, updateVertices }));
            }
            if (removeElements) {
                dispatch(api.removeElements({ productId, elements: removeElements }));
            }
        },

        redoSetPositions: ({ productId, updateVertices }) =>
            api.updatePositions({ productId, updateVertices }),

        updatePositions: ({ productId, updateVertices, existingVertices, undoable }) => (dispatch, getState) => {
            if (workspaceReadonly(getState())) {
                return;
            }

            if (!_.isEmpty(updateVertices)) {
                const state = getState();
                const snapToGrid = state.user.current.uiPreferences.snapToGrid === 'true';
                dispatch(api.setPositions({ productId, snapToGrid, updateVertices, undoable }));
            }
            if (!_.isEmpty(existingVertices)) {
                dispatch({
                    type: 'PRODUCT_GRAPH_ADD_GHOSTS',
                    payload: { ids: existingVertices.vertices, position: existingVertices.position }
                })
            }
        },

        dropElements: ({ productId, elements, position }) => (dispatch, getState) => {
            const { vertexIds, edgeIds } = elements;
            const state = getState();
            if (workspaceReadonly(state)) {
                return;
            }

            var edges = (edgeIds && edgeIds.length) ? (
                ajax('POST', '/edge/multiple', { edgeIds })
                    .then(function({ edges }) {
                        return _.flatten(edges.map(e => [e.inVertexId, e.outVertexId]));
                    })
                ) : Promise.resolve([]);

            edges.then(function(edgeVertexIds) {
                const state = getState();
                const workspaceId = state.workspace.currentId;
                const product = state.product.workspaces[workspaceId].products[productId];
                const parentId = product.localData && product.localData.rootId || 'root';
                const existingById = product.extendedData ? product.extendedData.vertices : {};
                const [existingVertices, newVertices] = _.partition(_.uniq(edgeVertexIds.concat(vertexIds)), id => id in existingById);

                if (!newVertices.length && (!position || !existingVertices.length)) return;

                const nextPosition = positionGeneratorFrom(position, newVertices.length, product);
                const updateVertices = {};
                newVertices.forEach(id => {
                    updateVertices[id] = {
                        parent: parentId,
                        pos: nextPosition(),
                        id,
                        type: 'vertex'
                    };
                });
                dispatch(api.updatePositions({
                    productId,
                    updateVertices,
                    existingVertices: { position, vertices: existingVertices },
                    undoable: true
                }))
                dispatch(productActions.select({ productId }));
            })
        },

        addRelated: ({ productId, vertices }) => (dispatch, getState) => {
            const state = getState();
            if (workspaceReadonly(state)) {
                return;
            }
            const workspaceId = state.workspace.currentId;
            const product = state.product.workspaces[workspaceId].products[productId];
            const byId = _.indexBy(product.extendedData.vertices, 'id');
            const updateVertices = _.object(_.compact(vertices.map(vertex => {
                if (vertex.id in byId) {
                    return;
                }

                return [vertex.id, {}]
            })));
            const length = _.size(updateVertices);
            if (length) {
                const nextPosition = positionGeneratorFrom(null, length, product);
                dispatch(elementActions.update({
                    workspaceId,
                    vertices: vertices
                }));
                dispatch(api.updatePositions({
                    productId,
                    updateVertices: _.mapObject(updateVertices, p => nextPosition())
                }));
            }

            dispatch(selectionActions.set({
                selection: {
                    vertices: Object.keys(updateVertices)
                }
            }));
        },

        removeElements: ({ productId, elements, undoable}) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const workspace = state.workspace.byId[workspaceId];

            if (workspace.editable && elements &&
                (elements.vertexIds && elements.vertexIds.length) ||
                (elements.collapsedNodeIds && elements.collapsedNodeIds.length)
            ) {
                const product = state.product.workspaces[workspaceId].products[productId];
                const { vertices: productVertices, compoundNodes: productCompoundNodes } = product.extendedData;

                const removeElements = getAdditionalRemovedElementIds(product, {
                    vertexIds: removeVertices,
                    collapsedNodeIds: removeCollapsedNodes
                }, true);
                const removeCollapsedNodes = removeElements.collapsedNodeIds || [];
                const removeVertices = removeElements.vertexIds || [];

                let undoPayload = {};
                if (undoable) {
                    const updateVertices = removeVertices
                        .map(id => productVertices[id])
                        .reduce(
                            (vertices, productVertex) => ({
                                [productVertex.id]: productVertex,
                                ...vertices
                            }),
                            {}
                        );
                    const updateCollapsedNodes = removeCollapsedNodes
                        .map(({ id }) => productCompoundNodes[id])
                        .reduce(
                            (nodes, productNode) => ({
                                [productNode.id]: productNode,
                                ...nodes
                            }),
                            {}
                        );
                    undoPayload = {
                        undoScope: productId,
                        undo: {
                            productId,
                            updateVertices: { ...updateVertices, ...updateCollapsedNodes}
                        },
                        redo: {
                            productId,
                            removeElements: elements
                        }
                    };
                }

                dispatch({
                    type: 'PRODUCT_GRAPH_REMOVE_ELEMENTS',
                    payload: {
                        elements: removeElements,
                        removeChildren: true,
                        productId,
                        workspaceId,
                        ...undoPayload
                    }
                });
                dispatch(selectionActions.remove({
                    selection: { vertices: removeVertices }
                }));

                if (removeVertices.length) {
                    ajax('POST', '/product/graph/vertices/remove', {
                        productId,
                        vertexIds: removeVertices.concat(removeCollapsedNodes),
                        params: {
                            removeChildren: true,
                            preventBroadcastToSourceGuid: true
                        }
                    });
                }
            }
        },

        undoRemoveElements: ({ productId, updateVertices }) => api.updatePositions({ productId, updateVertices }),

        redoRemoveElements: ({ productId, removeElements }) => api.removeElements({ productId, elements: removeElements }),

        collapseNodes: ({ productId, collapseData, undoable }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const { vertices, compoundNodes: collapsedNodes } = state.product.workspaces[workspaceId].products[productId].extendedData;
            const { id, ...params } = collapseData;
            const requestData = {
                params,
                productId
            };
            if (id) {
                requestData.vertexId = id;
            }

            ajax('POST', '/product/graph/vertices/collapse', requestData).then(collapsedNode => {
                const updateVertices = { [collapsedNode.id]: collapsedNode };
                collapsedNode.children.forEach(id => {
                    const child = vertices[id] || collapsedNodes[id];
                    child.parent = collapsedNode.id;
                    updateVertices[child.id] = child
                });

                dispatch({
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        updateVertices,
                        workspaceId
                    }
                })

                if (undoable) {
                    dispatch({
                        type: 'PUSH_UNDO',
                        payload: {
                            undoActionType: 'PRODUCT_GRAPH_COLLAPSE_NODES',
                            undoScope: productId,
                            undo: { productId, collapsedNodeIds: [collapsedNode.id] },
                            redo: { productId, collapseData }
                        }
                    })
                }
            });
        },

        uncollapseNodes: ({ productId, collapsedNodeId, undoable }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            const product = state.product.workspaces[workspaceId].products[productId];
            const { compoundNodes: collapsedNodes, vertices } = product.extendedData;
            const collapseData = collapsedNodes[collapsedNodeId];

//            const positionUpdates = _.flatten(collapsedNodeIds.map(id => collapsedNodes[id].children)
//            dispatch({
//                type: 'PRODUCT_GRAPH_SET_POSITIONS',
//                payload: {
//                    productId,
//                    updateVertices: { [collapsedNode.id]: collapsedNode },
//                    workspaceId
//                }
//            });
//
//            const removedElements = getAdditionalRemovedElementIds(product, { collapsedNodeIds }, false);
//            dispatch({
//                type: 'PRODUCT_GRAPH_REMOVE_ELEMENTS',
//                payload: {
//                    elements: removedElements,
//                    productId,
//                    workspaceId
//                }
//            });

            ajax('POST', '/product/graph/vertices/remove', { productId, vertexIds: [collapsedNodeId] }).then(product => {
                if (undoable) {
                    dispatch({
                        type: 'PUSH_UNDO',
                        payload: {
                            undoActionType: 'PRODUCT_GRAPH_UNCOLLAPSE_NODES',
                            undoScope: productId,
                            undo: { productId, collapseData },
                            redo: { productId, collapsedNodeId }
                        }
                    })
                }
            });
        }
    };

    return api;

    function getAdditionalRemovedElementIds(product, removedElements, removeChildren) {
        const { compoundNodes, vertices } = product.extendedData;
        const collapsedNodeIds = removeElements.collapsedNodeIds || [];
        const additionalVertexIds = [];
        const additionalCollapsedNodeIds = [];

        if (removeChildren) {
            const childQueue = [...collapsedNodeIds];

            while (childQueue.length) {
                const id = childQueue.shift();
                const node = compoundNodes[id];
                node.children.forEach(childId => {
                    if (childId in compoundNodes) {
                        childQueue.push(childId);
                    } else {
                        additionalVertexIds.push(childId);
                    }
                });
                additionalCollapsedNodeIds.push(id);
            }
        }

        collapsedNodeIds.forEach(id => {
            const node = compoundNodes[id];
            let parent = productCompoundNodes[node.parent];
            while (parent) {
                const children = parent.children.filter(childId => childId !== id);
                if (children.length === 0) { //TODO: change visible status if necessary
                    additionalCollapsedNodeIds.push(parent.id);
                }
                parent = compoundNodes[parent.parent];
            }
        });


        return {
            vertexIds: (removedElements.vertexIds || []).concat(additionalVertexIds),
            collapsedNodeIds: (removedElements.collapsedNodeIds || []).concat(additionalCollapsedNodeIds)
        }
    }

    function calculatePositionFromParents(child, oldParent, newParent, nesting) {
        const { x, y } = child.pos;

        return {
            x: nesting ? x - newParent.pos.x : x + oldParent.pos.x,
            y: nesting ? y - newParent.pos.y : y + oldParent.pos.y
        }
    }

    function positionGeneratorFrom(position, number, product) {
        const xInc = 175;
        const yInc = 75;
        const maxX = Math.round(Math.sqrt(number)) * xInc;

        if (!position) {
            const vertices = product.extendedData && product.extendedData.vertices || [];
            const maxY = vertices.length ? _.max(vertices, v => v.pos.y).pos.y + yInc : 0;
            const minX = vertices.length ? _.min(vertices, v => v.pos.x).pos.x : 0;
            position = { x: minX, y: maxY }
        }

        var currentPosition;
        return () => {
            if (currentPosition) {
                currentPosition.x += xInc;
                if ((currentPosition.x - position.x) > maxX) {
                    currentPosition.x = position.x;
                    currentPosition.y += yInc;
                }
            } else {
                currentPosition = {...position} || { x: 0, y: 0 };
            }
            return {...currentPosition}
        }
    }
})

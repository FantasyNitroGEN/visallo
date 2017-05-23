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
            const byId = _.indexBy(product.extendedData.vertices, 'id');
            const newVertices = Object.keys(updateVertices)
                .filter(id => !(id in byId));
            const addingNewVertices = newVertices.length > 0;

            updateVertices = _.mapObject(updateVertices, pos =>
                _.mapObject(pos, v => Math.round(v))
            );

            let undoPayload = {};
            if (undoable) {
                undoPayload = {
                    undoScope: productId,
                    undo: {
                        productId,
                    },
                    redo: {
                        productId,
                        updateVertices
                    }
                };
                if (Object.keys(updateVertices).length !== newVertices.length) {
                    undoPayload.undo.updateVertices = _.chain(updateVertices)
                                                        .mapObject((pos, id) => byId[id] && byId[id].pos)
                                                        .pick((pos, id) => !!pos)
                                                        .value();
                }
                if (addingNewVertices) {
                    undoPayload.undo.removeElements = {
                        vertexIds: newVertices
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
                updateVertices = _.mapObject(updateVertices, pos => snapPosition(pos));
                dispatch({
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        updateVertices,
                        workspaceId
                    }
                });
            }

            return ajax('POST', '/product', {
                productId,
                params: {
                    updateVertices: _.mapObject(updateVertices, (v) => ({ pos: v })),
                    broadcastOptions: {
                        preventBroadcastToSourceGuid: true
                    }
                },
            }).then(() => {
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
                        const vertexIds = Object.keys(updateVertices);
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
                dispatch(productActions.removeElements({ productId, elements: removeElements }));
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
                const existingById = _.indexBy(product.extendedData ? product.extendedData.vertices : [], 'id');
                const [existingVertices, newVertices] = _.partition(_.uniq(edgeVertexIds.concat(vertexIds)), id => id in existingById);

                if (!newVertices.length && (!position || !existingVertices.length)) return;

                const nextPosition = positionGeneratorFrom(position, newVertices.length, product);
                dispatch(api.updatePositions({
                    productId,
                    updateVertices: _.object(newVertices.map(id => [id, nextPosition()])),
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

        undoRemoveElements: ({ productId, updateVertices }) => api.updatePositions({ productId, updateVertices }),

        redoRemoveElements: ({ productId, removeElements }) => productActions.removeElements({ productId, elements: removeElements })
    };

    return api;

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

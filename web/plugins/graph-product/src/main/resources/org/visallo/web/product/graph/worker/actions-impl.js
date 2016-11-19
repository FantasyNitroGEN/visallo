define([
    'data/web-worker/store/actions',
    'data/web-worker/util/ajax',
    'data/web-worker/store/product/actions-impl',
    'data/web-worker/store/product/selectors',
    'data/web-worker/store/user/actions-impl',
    'data/web-worker/store/element/actions-impl',
    'data/web-worker/store/selection/actions-impl',
    './snap'
], function(
    actions,
    ajax,
    productActions,
    productSelectors,
    userActions,
    elementActions,
    selectionActions,
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

                dispatch(userActions.setUserPreference('snapToGrid', String(snap)));
            }
        },

        setPositions: ({ productId, updateVertices, snapToGrid }) => (dispatch, getState) => {
            if (workspaceReadonly(getState())) {
                return;
            }

            const workspaceId = getState().workspace.currentId;
            updateVertices = _.mapObject(updateVertices, pos => {
                return _.mapObject(pos, v => Math.round(v));
            });
            dispatch({
                type: 'PRODUCT_GRAPH_SET_POSITIONS',
                payload: {
                    productId,
                    updateVertices,
                    snapToGrid,
                    workspaceId
                }
            })
            if (snapToGrid) {
                const product = getState().product.workspaces[workspaceId].products[productId];
                const byId = _.indexBy(product.extendedData.vertices, 'id');
                updateVertices = _.mapObject(updateVertices, (pos, id) => {
                    return byId[id].pos;
                });
            }

            ajax('POST', '/product', {
                productId,
                params: {
                    updateVertices,
                    broadcastOptions: {
                        preventBroadcastToSourceGuid: true
                    } 
                },
            });
        },

        updatePositions: ({ productId, updateVertices }) => (dispatch, getState) => {
            if (workspaceReadonly(getState())) {
                return;
            }

            if (!_.isEmpty(updateVertices)) {
                const state = getState();
                const snapToGrid = state.user.current.uiPreferences.snapToGrid === 'true';
                const params = { updateVertices };
                dispatch(api.setPositions({ productId, updateVertices: params.updateVertices, snapToGrid }));
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

                const existing = product.extendedData ? product.extendedData.vertices : [];
                const combined = _.without(_.uniq(edgeVertexIds.concat(vertexIds)), ..._.pluck(existing, 'id'));
                if (!combined.length) return;
                const nextPosition = positionGeneratorFrom(position, combined.length, product);
                dispatch(api.updatePositions({
                    productId,
                    updateVertices: _.object(combined.map(id => [id, nextPosition()]))
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
                    vertices: _.pluck(vertices, 'id')
                }
            }));
        }
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

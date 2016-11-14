define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();


    const setFocusDebounce = _.debounce((dispatch, elementIds) => {
        dispatch({
            type: 'ELEMENT_SET_FOCUS',
            payload: elementIds
        })
    }, 250);

    const api = {
        get: ({ workspaceId, vertexIds, edgeIds, invalidate }) => (dispatch, getState) => {
            if ((vertexIds || edgeIds || []).length) {
                const state = getState();
                const elements = state.element[workspaceId]
                const toRequest = { vertexIds: vertexIds || [], edgeIds: edgeIds || [] };

                if (invalidate !== true && elements) {
                    if (elements.vertices) toRequest.vertexIds = _.reject(toRequest.vertexIds, vId => vId in elements.vertices);
                    if (elements.edges) toRequest.edgeIds = _.reject(toRequest.edgeIds, eId => eId in elements.edges);
                }

                ['vertex', 'edge'].forEach(type => {
                    const typeIds = type + 'Ids';
                    const resultType = type === 'vertex' ? 'vertices' : 'edges';

                    if (toRequest[typeIds].length) {
                        ajax('POST', `/${type}/multiple`, { workspaceId, [typeIds]: toRequest[typeIds] })
                            .then((result) => {
                                const byId = _.indexBy(result[resultType], 'id');
                                const elements = toRequest[typeIds].map(id => {
                                    if (id in byId) {
                                        return byId[id];
                                    }
                                    return { id, _DELETED: true };
                                })
                                const updates = {
                                     vertices: [],
                                    edges: [],
                                    workspaceId,
                                    ...{[resultType]:elements}
                                };
                                dispatch(api.update(updates));
                            })
                    }
                });
            }
        },

        setFocus: (elementIds) => (dispatch, getState) => {
            setFocusDebounce(dispatch, elementIds)
        },

        update: ({ vertices, edges, workspaceId }) => ({
            type: 'ELEMENT_UPDATE',
            payload: { vertices, edges, workspaceId }
        }),

        updateElement: (workspaceId, element) => ({
            type: 'ELEMENT_UPDATE',
            payload: {
                [element.type === 'vertex' ? 'vertices' : 'edges']: [element],
                workspaceId
            }
        }),

        propertyChange: (change) => (dispatch, getState) => {
            const { graphEdgeId, graphVertexId } = change;
            const isEdge = 'graphEdgeId' in change;
            const isVertex = 'graphVertexId' in change;
            const state = getState();
            const workspaceId = change.workspaceId || state.workspace.currentId;
            const vertexInStore = (...ids) => {
                return _.all(ids, id => workspaceId in state.element && (id in state.element[workspaceId].vertices));
            }

            if (isVertex) {
                if (vertexInStore(graphVertexId)) {
                    dispatch(api.get({ workspaceId, vertexIds: [graphVertexId], invalidate: true }));
                }
            } else if (isEdge) {
                const { inVertexId, outVertexId } = change;
                if (!inVertexId || !outVertexId || vertexInStore(inVertexId, outVertexId)) {
                    dispatch(api.get({ workspaceId, edgeIds: [graphEdgeId], invalidate: true }));
                }
            }
        },

        deleteElements: ({ vertexIds, edgeIds }) => (dispatch, getState) => {
            const state = getState();
            const workspaceId = state.workspace.currentId;
            if (!state.element[workspaceId]) {
                return;
            }
            const { vertices, edges } = state.element[workspaceId];
            const update = (key, list, api, storeKey, otherStoreKey) => {
                const inStore = list.filter(id => vertices[id]);
                if (inStore.length) {
                    ajax('POST', `/${api}/exists`, { [key]: inStore })
                        .then(({ exists }) => {
                            const elements = [];
                            _.map(exists, (exists, id) => {
                                if (!exists) {
                                    elements.push({ id, _DELETED: true })
                                }
                            })
                            dispatch(api.update({ [storeKey]: elements, [otherStoreKey]: [], workspaceId }));
                        })
                }
            };

            if (vertices && vertexIds && vertexIds.length) {
                update('vertexIds', vertexIds, 'vertex', 'vertices', 'edges');
            }
            if (edges && edgeIds && edgeIds.length) {
                update('edgeIds', edgeIds, 'edge', 'edges', 'vertices');
            }
        },

        putSearchResults: (elements) => (dispatch, getState) => {
            if (elements.length) {
                const workspaceId = getState().workspace.currentId;
                const withoutScore = elements.map(element => {
                    const {score, ...rest} = element;
                    return rest;
                });
                const grouped = _.groupBy(withoutScore, 'type');

                dispatch({
                    type: 'ELEMENT_UPDATE',
                    payload: {
                        workspaceId,
                        vertices: grouped.vertex,
                        edges: grouped.edge
                    }
                });
            }
        }
    }

    return api;
})


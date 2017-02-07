define(['../actions', 'data/web-worker/util/ajax'], function(actions, ajax) {
    actions.protectFromMain();


    const setFocusDebounce = _.debounce((dispatch, elementIds) => {
        dispatch({
            type: 'ELEMENT_SET_FOCUS',
            payload: elementIds
        })
    }, 250);
    const getWorkspaceIds = ({ element }) => {
        const { focusing, ...workspaces } = element;
        return Object.keys(workspaces);
    };


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
                                if (type === 'edge' && elements.length) {
                                    dispatch(api.updateEdgeLabels({ workspaceId, edges: elements }));
                                }
                            })
                    }
                });
            }
        },

        updateEdgeLabels: ({ workspaceId, edges }) => (dispatch, getState) => {
            const state = getState();
            const elementState = state.element[workspaceId];
            if (!elementState) return;
            const { vertices } = elementState;
            const updates = {};
            const addToUpdates = (vertexId, label) => {
                if (vertices[vertexId]) {
                    const vertex = vertices[vertexId];
                    if (vertex) {
                        const labels = vertex.edgeLabels || [];
                        if (!labels.includes(label)) {
                            updates[vertexId] = [...labels, label];
                        }
                    }
                }
            };

            edges.forEach(edge => {
                addToUpdates(edge.inVertexId, edge.label)
                addToUpdates(edge.outVertexId, edge.label)
            })

            if (!_.isEmpty(updates)) {
                dispatch({
                    type: 'ELEMENT_UPDATE_EDGELABELS',
                    payload: { workspaceId, vertexLabels: updates }
                })
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

        refreshElement: ({ workspaceId, vertexId, edgeId }) => (dispatch, getState) => {
            if (!workspaceId) {
                workspaceId = getState().workspace.currentId;
            }

            var params = {}, type;
            if (vertexId) {
                params.graphVertexId = vertexId;
                type = 'vertex';
            } else if (edgeId) {
                params.graphEdgeId = edgeId;
                type = 'edge';
            }

            ajax('GET', `/${type}/properties`, params).then(element => {
                dispatch(api.updateElement(workspaceId, element));
            });
        },

        propertyChange: (change) => (dispatch, getState) => {
            const { graphEdgeId, graphVertexId } = change;
            const isEdge = 'graphEdgeId' in change;
            const isVertex = 'graphVertexId' in change;
            const state = getState();
            const workspaceIds = getWorkspaceIds(state);
            const updateOnWorkspace = (workspaceId) => {
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
            };

            workspaceIds.forEach(updateOnWorkspace);
        },

        deleteElements: ({ vertexIds, edgeIds }) => (dispatch, getState) => {
            const state = getState();

            return Promise.map(getWorkspaceIds(state), (workspaceId) => {
                if (!state.element[workspaceId]) {
                    return Promise.resolve();
                }
                const elementStore = state.element[workspaceId];
                const { vertices, edges } = elementStore;
                const update = (key, list, type, storeKey, otherStoreKey) => {
                    const inStore = list.filter(id => elementStore[storeKey][id]);
                    if (inStore.length) {
                        return ajax('POST', `/${type}/exists`, { [key]: inStore })
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

                const updates = [];
                if (vertices && vertexIds && vertexIds.length) {
                    updates.push(update('vertexIds', vertexIds, 'vertex', 'vertices', 'edges'));
                }
                if (edges && edgeIds && edgeIds.length) {
                    updates.push(update('edgeIds', edgeIds, 'edge', 'edges', 'vertices'));
                }

                return Promise.all(updates);
            });
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


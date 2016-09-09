define(['updeep'], function(u) {
    'use strict';

    return function element(state, { type, payload }) {
        if (!state) return { focusing: { vertexIds: {}, edgeIds: {} } }

        if (payload) {
            switch (type) {
                case 'ELEMENT_SET_FOCUS': return setFocusing(state, payload)
                case 'ELEMENT_UPDATE':
                    const { workspaceId } = payload;
                    if (!workspaceId) throw new Error('WorkspaceId required');
                    return { ...state, [workspaceId]: update(state[workspaceId], payload) };
            }
        }

        return state;
    }

    function setFocusing(state, { vertexIds = [], edgeIds = [], elementIds = [] }) {
        const updates = {
            vertexIds: _.object(vertexIds.concat(elementIds).map(vertexId => [vertexId, true])),
            edgeIds: _.object(edgeIds.concat(elementIds).map(edgeId => [edgeId, true]))
        };
        return u({ focusing: u.constant(updates) }, state);
    }

    function update(previous, { vertices, edges }) {
        const updates = {};
        const updater = e => e._DELETED ? null : u.constant(e);

        if (vertices && vertices.length) {
            updates.vertices = _.mapObject(_.indexBy(vertices, 'id'), updater);
        } else if (!previous || !previous.vertices) updates.vertices = {};

        if (edges && edges.length) {
            updates.edges = _.mapObject(_.indexBy(edges, 'id'), updater);
        } else if (!previous || !previous.edges) updates.edges = {};

        return u(updates, previous)
    }

});


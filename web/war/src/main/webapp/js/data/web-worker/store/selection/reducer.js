define(['updeep'], function(u) {
    'use strict';

    return function selection(state, { type, payload }) {
        if (!state) return { idsByType: { vertices: [], edges: [] } };

        switch (type) {
            case 'SELECTION_ADD': return addSelection(state, payload);
            case 'SELECTION_REMOVE': return removeSelection(state, payload);
            case 'SELECTION_SET': return setSelection(state, payload);
            case 'SELECTION_CLEAR': return clearSelection(state, payload);
        }

        return state
    }

    function addSelection(state, { selection }) {
        const { vertices, edges } = selection;
        const addElements = (type, append) => (elements) => {
            if (append.length) {
                return _.uniq(elements.concat(append))
            }
            return elements;
        }
        return u({
            idsByType: {
                vertices: addElements('vertices', vertices),
                edges: addElements('edges', edges)
            }
        }, state);
    }

    function removeSelection(state, { selection }) {
        const { vertices = [], edges = [] } = selection;
        return u({
            idsByType: {
                vertices: u.reject(v => vertices.includes(v)),
                edges: u.reject(e => edges.includes(e))
            }
        }, state);
    }

    function setSelection(state, { selection }) {
        const { vertices, edges } = selection;
        return u({
            idsByType: u.constant({
                vertices: _.uniq(vertices || []),
                edges: _.uniq(edges || [])
            })
        }, state)
    }

    function clearSelection(state) {
        return u({
            idsByType: u.constant({
                vertices: [],
                edges: []
            })
        }, state)
    }
})



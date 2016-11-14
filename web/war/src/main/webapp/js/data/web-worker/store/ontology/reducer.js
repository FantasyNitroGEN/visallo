define(['updeep'], function(updeep) {
    'use strict';

    return function ontology(state, { type, payload }) {
        if (!state) return { concepts: {}, properties: {}, relationships: {} }

        switch (type) {
            case 'ONTOLOGY_UPDATE': return update(state, payload);
        }

        return state;
    }

    function update(state, payload) {
        return updeep(payload, state)
    }
});


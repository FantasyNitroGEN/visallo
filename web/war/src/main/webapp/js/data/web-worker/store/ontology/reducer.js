define(['updeep'], function(u) {
    'use strict';

    return function ontology(state = {}, { type, payload }) {

        switch (type) {
            case 'ONTOLOGY_UPDATE': return update(state, payload);
            case 'ONTOLOGY_PARTIAL_UPDATE': return updatePartial(state, payload);
            case 'ONTOLOGY_IRI_CREATED': return updateIri(state, payload);
            case 'ONTOLOGY_INVALIDATE': return invalidate(state, payload);
        }

        return state;
    }

    function update(state, payload) {
        const { workspaceId, ...ontology } = payload;
        return u({ [workspaceId]: u.constant(ontology) }, state);
    }

    function updatePartial(state, payload) {
        const { workspaceId, concepts = {}, relationships = {}, properties = {} } = payload;
        return u({
            [workspaceId]: {
                concepts: _.mapObject(concepts, o => u.constant(o)),
                relationships: _.mapObject(relationships, o => u.constant(o)),
                properties: _.mapObject(properties, o => u.constant(o)),
            }
        }, state)
    }

    function invalidate(state, { workspaceIds = [] }) {
        return u(u.omit(workspaceIds), state);
    }

    function updateIri(state, { type, key, iri }) {
        return u.updateIn(`iris.${type}.${key}`, iri, state);
    }
});


define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const anyNotEmpty = ({ conceptIds, relationshipIds, propertyIds }) => _.any([conceptIds, relationshipIds, propertyIds], l => !_.isEmpty(l))
    const add = (type, listName) => ({ workspaceId, key, ...rest }) => dispatch => {
        const obj = rest[type];
        return ajax('POST', `/ontology/${type}`, { workspaceId, ...obj })
            .then(payload => {
                dispatch(api.partial({ workspaceId, [listName]: { [payload.title]: payload }}))
                if (key) {
                    dispatch(api.iriCreated({ key, type, iri: payload.title }))
                }
            })
    };

    const api = {
        get: ({ workspaceId, invalidate = false }) => (dispatch, getState) => {
            const state = getState();
            if (!workspaceId) {
                workspaceId = state.workspace.currentId ||
                    (state.user.current && state.user.current.currentWorkspaceId)
            }

            if (!workspaceId) throw new Error('No workspace provided');

            if (!state.ontology[workspaceId] || invalidate) {
                return ajax('GET', '/ontology', { workspaceId })
                    .then(result => {
                        dispatch(api.update({ ...transform(result), workspaceId }))
                    })
            }
        },

        update: (payload) => ({
            type: 'ONTOLOGY_UPDATE',
            payload
        }),

        invalidate: ({ workspaceIds }) => ({
            type: 'ONTOLOGY_INVALIDATE',
            payload: {
                workspaceIds
            }
        }),

        partial: ({ workspaceId, ...ontology }) => (dispatch, getState) => {
            if (!workspaceId) {
                workspaceId = getState().workspace.currentId;
            }

            dispatch({
                type: 'ONTOLOGY_PARTIAL_UPDATE',
                payload: {
                    workspaceId,
                    ...transform(ontology)
                }
            })
        },

        addConcept: add('concept', 'concepts'),

        addProperty: add('property', 'properties'),

        addRelationship: add('relationship', 'relationships'),

        iriCreated: ({ type, key, iri }) => ({
            type: 'ONTOLOGY_IRI_CREATED',
            payload: { type, key, iri }
        }),

        ontologyChange: ({ workspaceId, conceptIds, relationshipIds, propertyIds }) => (dispatch, getState) => {
            const state = getState();
            const isPublishedChanged = !workspaceId;
            const ids = { conceptIds, relationshipIds, propertyIds };
            const hasIds = anyNotEmpty(ids)
            const currentWorkspaceId = state.workspace.currentId;
            const requestWithIds = (workspaceId, ontology) => {
                return ajax('GET', '/ontology/segment', { workspaceId, ...ontology })
                    .then(payload => {
                        dispatch(api.partial({ workspaceId, ...payload }))
                    })
            }

            if (isPublishedChanged) {
                let otherWorkspaces = Object.keys(state.ontology);
                if (currentWorkspaceId) {
                    otherWorkspaces = _.without(otherWorkspaces, currentWorkspaceId);
                }
                dispatch(api.invalidate({ workspaceIds: otherWorkspaces }));
                if (currentWorkspaceId) {
                    if (hasIds) {
                        return requestWithIds(currentWorkspaceId, ids);
                    } else {
                        dispatch(api.get({ currentWorkspaceId, invalidate: true }));
                    }
                }
            } else {
                const workspaceInStore = workspaceId in state.ontology;
                if (workspaceInStore) {
                    if (hasIds) {
                        return requestWithIds(workspaceId, ids);
                    } else {
                        dispatch(api.get({ workspaceId, invalidate: true }))
                    }
                }
            }
        }
    }

    return api;


    function transform(ontology) {
        const concepts = _.indexBy(ontology.concepts, 'title');
        const properties = _.indexBy(ontology.properties, 'title');
        const relationships = _.indexBy(ontology.relationships, 'title');

        return { concepts, properties, relationships };
    }
})


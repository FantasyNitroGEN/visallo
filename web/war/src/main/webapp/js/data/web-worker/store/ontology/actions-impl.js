define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const api = {
        get: () => {
            return ajax('GET', '/ontology')
                .then(result => api.update(result))
        },

        update: (ontology) => ({
            type: 'ONTOLOGY_UPDATE',
            payload: transform(ontology)
        })
    }

    return api;


    function transform(ontology) {
        const concepts = _.indexBy(ontology.concepts, 'title');
        const properties = _.indexBy(ontology.properties, 'title');
        const relationships = _.indexBy(ontology.relationships, 'title');

        return { concepts, properties, relationships };
    }
})


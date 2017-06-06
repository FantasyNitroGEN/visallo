define(['util/promise'], function(Promise) {
console.log('in store')
    var promise;
    var parsedOntology = JSON.parse(ONTOLOGY_JSON);
    var ontology = {
        concepts: _.indexBy(parsedOntology.concepts, "id"),
        relationships: _.indexBy(parsedOntology.relationships, "title"),
        properties: _.indexBy(parsedOntology.properties, "title")
    };
    var workspaceOntology = {};
    workspaceOntology['WORKSPACE_ID'] = ontology;

    return {
        getStore: function() {
            return {
                getState: function () { return { workspace: { currentId: 'WORKSPACE_ID' }, ontology: workspaceOntology }; },
                subscribe: function() {}
            };
        },
        getOrWaitForNestedState: function(callback){
            if (promise) return promise.then(function(o) {
                return JSON.parse(JSON.stringify(o));
            });

            var copied = callback({ ontology: workspaceOntology })
            return (promise = Promise.resolve(copied));
        }
    }
})

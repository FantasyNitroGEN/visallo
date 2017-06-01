define(['util/promise'], function(Promise) {
    var promise;
    return {
        getStore: function() {
            return {
                getState: function() {
                    return {
                        workspace: {},
                        ontology: {
                            //['w1']: {
                                //concepts: {},
                                //relationships: {},
                                //properties: {}
                            //}
                       }
                    };
                },
                subscribe: function() { }
            }
        },
        getOrWaitForNestedState: function(callback){
            if (promise) return promise.then(function(o) {
                return JSON.parse(JSON.stringify(o));
            });
            var parsedOntology = window.ONTOLOGY_JSON;
            var copied = callback({
                ontology: {
                    [publicData.currentWorkspaceId]: {
                        concepts: _.indexBy(parsedOntology.concepts, 'id'),
                        relationships: _.indexBy(parsedOntology.relationships, 'title'),
                        properties: _.indexBy(parsedOntology.properties, 'title')
                    }
                }
            })
            return (promise = Promise.resolve(copied));
        }
    }
})


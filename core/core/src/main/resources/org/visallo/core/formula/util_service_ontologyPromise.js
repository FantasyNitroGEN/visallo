define([
    'util/withDataRequest'
], function(withDataRequest) {

    // Override and check for error
    return withDataRequest.dataRequest('ontology', 'ontology').catch(function(e) {
        console.error(e);
        throw e;
    })
});

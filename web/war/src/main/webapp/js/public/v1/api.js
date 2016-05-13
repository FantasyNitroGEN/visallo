define([
    'flight/lib/component',
    'configuration/plugins/registry'
], function(
    defineComponent,
    registry) {
    'use strict';

    return {
        connect: connect,
        defineComponent: defineComponent,
        registry: registry
    };

    function connect() {
        return Promise.all([
            'util/element/list',
            'util/ontology/conceptSelect',
            'util/ontology/propertySelect',
            'util/ontology/relationshipSelect',
            'util/vertex/formatters',
            'util/vertex/justification/viewer',
            'util/visibility/edit',
            'util/visibility/view',
            'util/withDataRequest'
        ].map(function(module) {
            return Promise.require(module);
        })).spread(function(
            List,
            ConceptSelector,
            PropertySelector,
            RelationshipSelector,
            F,
            JustificationViewer,
            VisibilityEditor,
            VisibilityViewer,
            withDataRequest) {

            return {
                components: {
                    JustificationViewer: JustificationViewer,
                    List: List,
                    OntologyConceptSelector: ConceptSelector,
                    OntologyPropertySelector: PropertySelector,
                    OntologyRelationshipSelector: RelationshipSelector
                },
                formatters: F,
                dataRequest: withDataRequest.dataRequest
            };
        });
    }
});

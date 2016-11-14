define(['util/promise'], function() {
    return new Promise(function(resolve) {
        require([
            'text!../test/unit/mocks/ontology.json'
        ], function(json) {
            // Hack ontology for testing
            var ontologyJson = JSON.parse(json),
                person = _.findWhere(ontologyJson.concepts, { id: 'http://visallo.org/dev#person' });

            // Delete color for person
            if (person) {
                delete person.color;
            }

            // Add compound field that dependends on another compound
            ontologyJson.properties.push({
                title: 'http://visallo.org/testing#compound1',
                displayName: 'Testing Compound',
                userVisible: true,
                searchable: true,
                dataType: 'string',
                validationFormula:
                    'dependentProp("http://visallo.org/dev#title") && ' +
                    'dependentProp("http://visallo.org/dev#name")',
                displayFormula:
                    'dependentProp("http://visallo.org/dev#name") + ", "' +
                    'dependentProp("http://visallo.org/dev#title")',
                dependentPropertyIris: [
                    'http://visallo.org/dev#title',
                    'http://visallo.org/dev#name'
                ]
            })

            // Add heading
            ontologyJson.properties.push({
                title: 'http://visallo.org/testing#heading1',
                displayName: 'Testing Heading',
                userVisible: true,
                searchable: true,
                dataType: 'double',
                displayType: 'heading'
            })

            ontologyJson.properties.push({
                title: 'http://visallo.org/testing#integer1',
                displayName: 'Testing integer',
                userVisible: true,
                searchable: true,
                dataType: 'integer'
            })

            ontologyJson.properties.push({
                title: 'http://visallo.org/testing#number1',
                displayName: 'Testing number',
                userVisible: true,
                searchable: true,
                dataType: 'number'
            })

            // Add video sub concept to test displayType
            ontologyJson.concepts.push({
                id:'http://visallo.org/dev#videoSub',
                title:'http://visallo.org/dev#videoSub',
                displayName:'VideoSub',
                parentConcept:'http://visallo.org/dev#video',
                pluralDisplayName:'VideoSubs',
                searchable:true,
                properties:[]
            });

            // Add video sub concept to test compound properties
            ontologyJson.concepts.push({
                id:'http://visallo.org/dev#personSub',
                title:'http://visallo.org/dev#personSub',
                displayName:'PersonSub',
                parentConcept:'http://visallo.org/dev#person',
                pluralDisplayName:'PersonSubs',
                searchable:true,
                properties:[]
            });

            window.ONTOLOGY_JSON = ontologyJson;
            require(['data/web-worker/services/ontology'], function(ontology) {
                resolve(ontology.ontology());
            })
        });
    });
});

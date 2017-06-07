define(['reselect'], function(reselect) {
    const { createSelector } = reselect;

    const _visible = concept => concept.userVisible !== false &&
        concept.id !== 'http://www.w3.org/2002/07/owl#Thing' &&
        concept.id !== 'http://visallo.org#root';
    const _collectParents = concepts => concept => {
        const collecting = {
            color: null,
            path: [],
            properties: [],
            glyphIconHref: null,
            glyphIconSelectedHref: null
        };
        _collect(concept);
        const { path, properties, ...override } = collecting;
        const newConcept = {
            ...concept,
            path: '/' + path.reverse().join('/'),
            properties: _.uniq(properties),
            depth: path.length - 1,
            ...override
        };
        return newConcept;

        function _collect(concept) {
            collecting.color = collecting.color || concept.color;
            collecting.glyphIconHref = collecting.glyphIconHref || concept.glyphIconHref;
            collecting.glyphIconSelectedHref = collecting.glyphIconSelectedHref || concept.glyphIconSelectedHref;
            if (_visible(concept)) {
                collecting.path.push(concept.displayName)
            }
            collecting.properties = collecting.properties.concat(concept.properties);

            if (concept.parentConcept) {
                const parentConcept = concepts[concept.parentConcept];
                _collect(parentConcept);
            }
        }
    }

    const getWorkspace = (state) => state.workspace.currentId;

    const getOntologyRoot = (state) => state.ontology;

    const getConcepts = createSelector([getWorkspace, getOntologyRoot], (workspaceId, ontology) => {
        const concepts = ontology[workspaceId].concepts;
        return _.mapObject(concepts, _collectParents(concepts))
    })

    const getProperties = createSelector([getWorkspace, getOntologyRoot], (workspaceId, ontology) => {
        return ontology[workspaceId].properties;
    })

    const getRelationships = createSelector([getWorkspace, getOntologyRoot], (workspaceId, ontology) => {
        return ontology[workspaceId].relationships;
    })

    const getVisibleConcepts = createSelector([getConcepts], concepts => {
        return _.chain(concepts)
            .map()
            .filter(_visible)
            .sortBy('path')
            .value()
    })

    const getConceptKeyIris = createSelector([getOntologyRoot], ontology => ontology.iris && ontology.iris.concept)

    const getOntology = createSelector([getOntologyRoot, getWorkspace], (ontology, workspaceId) => ontology[workspaceId])

    const getVisibleProperties = createSelector([getProperties], properties => {
        const compareNameAndGroup = ({ displayName, propertyGroup }) => {
            const displayNameLC = displayName.toLowerCase();
            return propertyGroup ? `1${propertyGroup}${displayNameLC}` : `0${displayNameLC}`;
        };

        return _.chain(properties)
            .filter(_visible)
            .sortBy(compareNameAndGroup)
            .value()
    });

    const getVisiblePropertiesWithHeaders = createSelector([getVisibleProperties], properties => {
        let lastGroup;
        return properties.reduce(
            (properties, property) => {
                const { propertyGroup } = property;
                if (propertyGroup && lastGroup !== propertyGroup) {
                    lastGroup = propertyGroup;
                    return [
                        ...properties,
                        {
                            displayName: propertyGroup,
                            header: true
                        },
                        property
                    ];
                }
                return [...properties, property];
            },
            []
        );
    });

    return {
        getOntology,
        getConcepts,
        getConceptKeyIris,
        getVisibleConcepts,
        getProperties,
        getVisibleProperties,
        getVisiblePropertiesWithHeaders,
        getRelationships
    }
});

define(['reselect'], function(reselect) {
    const { createSelector } = reselect;

    const _visible = (item, options = {}) => {
        const { rootItemsHidden = true } = options;
        return item &&
            item.userVisible !== false &&
            (!rootItemsHidden || (
                item.id !== 'http://www.w3.org/2002/07/owl#Thing' &&
                item.id !== 'http://visallo.org#root'
            )) &&
            item.displayName;
    };
    const _collectParents = concepts => concept => {
        const collecting = {
            color: null,
            path: [],
            pathIris: [],
            properties: [],
            glyphIconHref: null,
            glyphIconSelectedHref: null
        };
        _collect(concept);
        const {
            path,
            pathIris,
            properties,
            glyphIconHref = 'img/glyphicons/glyphicons_194_circle_question_mark@2x.png',
            ...override } = collecting;
        const newConcept = {
            ...concept,
            path: '/' + path.reverse().join('/'),
            pathIris,
            properties: _.uniq(properties),
            depth: path.length - 1,
            glyphIconHref,
            ...override
        };
        return newConcept;

        function _collect(concept) {
            collecting.color = collecting.color || concept.color;
            collecting.glyphIconHref = collecting.glyphIconHref || concept.glyphIconHref;
            collecting.glyphIconSelectedHref = collecting.glyphIconSelectedHref || concept.glyphIconSelectedHref;
            if (_visible(concept)) {
                collecting.path.push(concept.displayName)
                collecting.pathIris.push(concept.title)
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

    const getVisibleRelationships = createSelector([getRelationships, getConcepts], (relationships, concepts) => {
        const anyIrisVisible = iris => _.isArray(iris) && _.any(iris, iri => _visible(concepts[iri], { rootItemsHidden: false }))
        const relationshipConceptsVisible = r => anyIrisVisible(r.rangeConceptIris) && anyIrisVisible(r.domainConceptIris);
        return _.chain(relationships)
            .map()
            .filter(r => _visible(r) && relationshipConceptsVisible(r))
            .sortBy('displayName')
            .value()
    })

    const getRelationshipKeyIris = state => state.ontology.iris && state.ontology.iris.relationship;

    const getConceptAncestors = createSelector([getConcepts], concepts => {
        const byParent = _.groupBy(concepts, 'parentConcept');
        const collectAncestors = (list, c, skipFirst) => {
            if (!skipFirst) list.push(c.title);
            if (c.parentConcept) {
                collectAncestors(list, concepts[c.parentConcept]);
            }
            return _.uniq(list);
        }
        return _.mapObject(concepts, c => collectAncestors([], c, true));
    })

    const getConceptDescendents = createSelector([getConcepts], concepts => {
        const byParent = _.groupBy(concepts, 'parentConcept');
        const collectDescendents = (list, c, skipFirst) => {
            if (!skipFirst) list.push(c.title);
            if (byParent[c.title]) {
                byParent[c.title].forEach(inner => collectDescendents(list, inner));
            }
            return _.uniq(list);
        }
        return _.mapObject(concepts, c => collectDescendents([], c, true));
    })

    const getVisibleConcepts = createSelector([getConcepts], concepts => {
        return _.chain(concepts)
            .map()
            .filter(_visible)
            .sortBy('path')
            .value()
    })

    const getConceptKeyIris = state => state.ontology.iris && state.ontology.iris.concept

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

    const getPropertyKeyIris = state => state.ontology.iris && state.ontology.iris.properties;

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
        getConceptDescendents,
        getConceptAncestors,
        getVisibleConcepts,

        getProperties,
        getPropertyKeyIris,
        getVisibleProperties,
        getVisiblePropertiesWithHeaders,

        getRelationships,
        getRelationshipKeyIris,
        getVisibleRelationships
    }
});

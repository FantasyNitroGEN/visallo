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
    const _collectParents = (concepts, { parentKey, extraKeys = [], defaults = {} } = {}) => concept => {
        const collecting = {
            path: [],
            pathIris: [],
            properties: [],
            ...(_.object(extraKeys.map(k => [k, null])))
        };
        _collect(concept);
        const {
            path,
            pathIris,
            properties,
            ...override } = collecting;
        _.each(override, (v, k) => {
            if (!v && defaults[k]) {
                override[k] = defaults[k];
            }
        })
        const newConcept = {
            ...concept,
            path: '/' + path.reverse().join('/'),
            pathIris,
            properties: _.uniq(properties),
            depth: path.length - 1,
            ...override
        };
        return newConcept;

        function _collect(concept) {
            extraKeys.forEach(k => {
                collecting[k] = collecting[k] || concept[k];
            })
            if (_visible(concept)) {
                collecting.path.push(concept.displayName)
                collecting.pathIris.push(concept.title)
            }
            collecting.properties = collecting.properties.concat(concept.properties);

            if (concept[parentKey]) {
                const parent = concepts[concept[parentKey]];
                _collect(parent);
            }
        }
    }

    const getWorkspace = (state) => state.workspace.currentId;

    const getOntologyRoot = (state) => state.ontology;

    const getConcepts = createSelector([getWorkspace, getOntologyRoot], (workspaceId, ontology) => {
        const concepts = ontology[workspaceId].concepts;
        const fn = _collectParents(concepts, {
            parentKey: 'parentConcept',
            extraKeys: ['color', 'glyphIconHref', 'glyphIconSelectedHref'],
            defaults: { glyphIconHref: 'img/glyphicons/glyphicons_194_circle_question_mark@2x.png' }
        });
        return _.mapObject(concepts, c => {
            const newC = fn(c);
            console.log(newC)
            return { ...newC, displayNameSub: '' };
        });
    })

    const getProperties = createSelector([getWorkspace, getOntologyRoot], (workspaceId, ontology) => {
        return ontology[workspaceId].properties;
    })

    const getRelationships = createSelector([getWorkspace, getOntologyRoot, getConcepts], (workspaceId, ontology, concepts) => {
        const relationships = ontology[workspaceId].relationships;
        const mostTopLevelGlyphIconForIris = iris => {
            const c = _.first(_.sortBy(iris.map(iri => concepts[iri]), 'depth'));
            return c && c.glyphIconHref || null;
        };
        const fn = _collectParents(relationships, { parentKey: 'parentIri' });
        return _.mapObject(relationships, r => {
            const newR = fn(r);
            const domainGlyphIconHref = mostTopLevelGlyphIconForIris(newR.domainConceptIris);
            const rangeGlyphIconHref = mostTopLevelGlyphIconForIris(newR.rangeConceptIris);
            const domains = newR.domainConceptIris.map(iri => concepts[iri].displayName)
            const ranges = newR.rangeConceptIris.map(iri => concepts[iri].displayName)
            const displayNameSub = domains.length === 1 ? ranges.map(r => domains[0] + '→' + r).join('\n') :
                ranges.length === 1 ? domains.map(d => d + '→' + ranges[0]).join('\n') :
                `(${domains}) → (${ranges})`
            return { ...newR, domainGlyphIconHref, rangeGlyphIconHref, displayNameSub };
        });
    })

    const getVisibleRelationships = createSelector([getRelationships, getConcepts], (relationships, concepts) => {
        const anyIrisVisible = iris => _.isArray(iris) && _.any(iris, iri => _visible(concepts[iri], { rootItemsHidden: false }))
        const relationshipConceptsVisible = r => anyIrisVisible(r.rangeConceptIris) && anyIrisVisible(r.domainConceptIris);
        return _.chain(relationships)
            .map()
            .filter(r => _visible(r) && relationshipConceptsVisible(r))
            .sortBy('path')
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

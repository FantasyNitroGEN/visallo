
/**
 * Get the current ontology. Includes:
 *
 * * Concepts: Vertex types
 * * Properties: Properties for elements
 * * Relationships: Edges
 *
 * @module services/ontology
 * @see module:dataRequest
 */
define([
    '../util/ajax',
    '../util/memoize',
    '../store',
    'configuration/plugins/registry'
], function(ajax, memoize, store, registry) {
    'use strict';

    /**
     * @undocumented
     */
    registry.documentExtensionPoint('org.visallo.ontology',
        'Ignore some ontology warnings',
        function(e) {
            return _.isArray(e.ignoreColorWarnings);
        }
    );

    var PARENT_CONCEPT = 'http://www.w3.org/2002/07/owl#Thing';
    var ROOT_CONCEPT = 'http://visallo.org#root';
    var ontologyReady = function(s) {
        return s &&
        s.ontology &&
        !_.isEmpty(s.ontology.concepts) &&
        !_.isEmpty(s.ontology.properties) &&
        !_.isEmpty(s.ontology.relationships);
    }
    var getOntology = function() {
        return store.getOrWaitForNestedState(function(s) {
            return JSON.parse(JSON.stringify(s.ontology));
        }, ontologyReady)
    }
    var extensions = registry.extensionsForPoint('org.visallo.ontology');

    /**
     * @alias module:services/ontology
     */
    var api = {

            /**
             * All ontology objects: concepts, properties, relationships
             *
             * The result is cached so only first call makes a request to server.
             *
             * @function
             */
            ontology: memoize(function() {
                return Promise.all([
                    api.concepts(),
                    api.properties(),
                    api.relationships()
                ]).then(function(results) {
                    var concepts = results.shift(),
                        properties = results.shift(),
                        relationships = results.shift();

                    return {
                        concepts: concepts,
                        properties: properties,
                        relationships: relationships
                    };
                })
            }),

            /**
             * Ontology properties
             *
             * @function
             */
            properties: memoize(function() {

                return getOntology()
                    .then(function(ontology) {
                        return {
                            list: _.sortBy(_.values(ontology.properties), 'displayName'),
                            byTitle: ontology.properties,
                            byDataType: _.groupBy(ontology.properties, 'dataType'),
                            byDependentToCompound: _.chain(ontology.properties)
                                .filter(function(p) {
                                    return 'dependentPropertyIris' in p;
                                })
                                .map(function(p) {
                                    return p.dependentPropertyIris.map(function(iri) {
                                        return [iri, p.title];
                                    })
                                })
                                .flatten(true)
                                .object()
                                .value()
                        };
                    })
            }),

            /**
             * Return properties by element type
             *
             * @function
             * @param {string} type Either 'vertex' or 'edge'
             * @returns {Array.<object>}
             */
            propertiesByDomainType: memoize(function(type) {
                return getOntology()
                    .then(function(ontology) {
                        var items = (type === 'concept' || type === 'vertex') ? ontology.concepts : ontology.relationships;

                        return _.chain(items)
                            .pluck('properties')
                            .compact()
                            .flatten()
                            .uniq()
                            .map(function(propertyName) {
                                return ontology.properties[propertyName]
                            })
                            .value();
                    });
            }),

            /**
             * Properties given edgeId
             *
             * @function
             * @param {string} id
             */
            propertiesByRelationship: memoize(function(relationshipId) {
                return api.ontology()
                    .then(function(ontology) {
                        var propertyIds = [],
                            collectPropertyIds = function(rId) {
                                var relation = ontology.relationships.byId[rId],
                                properties = relation && relation.properties,
                                parentId = relation && relation.parentIri;

                                if (properties && properties.length) {
                                    propertyIds.push.apply(propertyIds, properties);
                                }
                                if (parentId) {
                                    collectPropertyIds(parentId);
                                }
                            };

                        collectPropertyIds(relationshipId);

                        var properties = _.chain(propertyIds)
                            .uniq()
                            .map(function(pId) {
                                return ontology.properties.byTitle[pId];
                            })
                            .value();

                        return {
                            list: _.sortBy(properties, 'displayName'),
                            byTitle: _.pick(ontology.properties, propertyIds)
                        };
                    });
            }),

            /**
             * Properties given conceptId
             *
             * @function
             * @param {string} id
             */
            propertiesByConceptId: memoize(function(conceptId) {
                return getOntology()
                    .then(function(ontology) {
                        var propertyIds = [],
                            collectPropertyIds = function(conceptId) {
                                var concept = ontology.concepts[conceptId],
                                properties = concept && concept.properties,
                                parentConceptId = concept && concept.parentConcept;

                                if (properties && properties.length) {
                                    propertyIds.push.apply(propertyIds, properties);
                                }
                                if (parentConceptId) {
                                    collectPropertyIds(parentConceptId);
                                }
                            };

                        collectPropertyIds(conceptId);

                        var properties = _.chain(propertyIds)
                            .uniq()
                            .map(function(pId) {
                                return ontology.properties[pId];
                            })
                            .value();

                        return {
                            list: _.sortBy(properties, 'displayName'),
                            byTitle: _.pick(ontology.properties, propertyIds)
                        };
                    });
            }),

            /**
             * Ontology concepts
             *
             * @function
             */
            concepts: memoize(function() {
                var clsIndex = 0;

                return getOntology()
                    .then(function(ontology) {
                        return {
                            entityConcept: buildTree(
                                ontology.concepts,
                                ontology.concepts[PARENT_CONCEPT]
                            ),
                            forAdmin: _.chain(ontology.concepts)
                                .filter(onlyEntityConcepts.bind(null, ontology.concepts, true))
                                .map(addFlattenedTitles.bind(null, ontology.concepts, true))
                                .sortBy('flattenedDisplayName')
                                .value(),
                            byId: _.chain(ontology.concepts)
                                .map(addFlattenedTitles.bind(null, ontology.concepts, false))
                                .indexBy('id')
                                .value(),
                            byClassName: _.indexBy(ontology.concepts, 'className'),
                            byTitle: _.chain(ontology.concepts)
                                .filter(onlyEntityConcepts.bind(null, ontology.concepts, false))
                                .map(addFlattenedTitles.bind(null, ontology.concepts, false))
                                .sortBy('flattenedDisplayName')
                                .value()
                        };
                    });

                function buildTree(concepts, root) {
                    var groupedByParent = _.groupBy(concepts, 'parentConcept'),
                        ignoreColorWarnings = _.chain(extensions)
                            .pluck('ignoreColorWarnings')
                            .flatten()
                            .unique()
                            .value(),
                        findChildrenForNode = function(node) {
                            node.className = 'conceptId-' + (clsIndex++);
                            node.children = groupedByParent[node.id] || [];
                            node.children = node.children.map(function(child) {
                                if (!child.glyphIconHref) {
                                    child.glyphIconHref = node.glyphIconHref;
                                }
                                if (!child.glyphIconSelectedHref) {
                                    child.glyphIconSelectedHref = node.glyphIconSelectedHref;
                                }
                                if (!child.color) {
                                    if (node.color) {
                                        child.color = node.color;
                                    } else {
                                        if (!_.contains(ignoreColorWarnings, child.id) && child.userVisible !== false) {
                                            console.warn(
                                                'No color specified in concept hierarchy for conceptType:',
                                                child.id
                                            );
                                        }
                                        child.color = 'rgb(0, 0, 0)';
                                    }
                                }
                                return findChildrenForNode(child);
                            });
                            return node;
                        };

                    return findChildrenForNode(root);
                }

                function onlyEntityConcepts(conceptsById, includeThing, concept) {
                    var parentConceptId = concept.parentConcept,
                        currentParentConcept = null;

                    while (parentConceptId) {
                        currentParentConcept = conceptsById[parentConceptId];
                        if (!currentParentConcept) {
                            console.error('Could not trace concept\'s lineage to ' + PARENT_CONCEPT +
                                ' could not find ' + parentConceptId, concept);
                            return false;
                        }
                        if (currentParentConcept.id === PARENT_CONCEPT) {
                            return true;
                        }
                        parentConceptId = currentParentConcept.parentConcept;
                    }

                    return includeThing && concept.id === PARENT_CONCEPT;
                }

                function addFlattenedTitles(conceptsById, includeThing, concept) {
                    var parentConceptId = concept.parentConcept,
                        currentParentConcept = null,
                        parents = [];

                    while (parentConceptId) {
                        currentParentConcept = conceptsById[parentConceptId];
                        if (includeThing) {
                            if (currentParentConcept.id === ROOT_CONCEPT) break;
                        } else {
                            if (currentParentConcept.id === PARENT_CONCEPT) break;
                        }
                        parents.push(currentParentConcept);
                        parentConceptId = currentParentConcept.parentConcept;
                    }

                    parents.reverse();
                    var leadingSlashIfNeeded = parents.length ? '/' : '',
                        flattenedDisplayName = _.pluck(parents, 'displayName')
                            .join('/') + leadingSlashIfNeeded + concept.displayName,
                        indent = flattenedDisplayName
                            .replace(/[^\/]/g, '')
                            .replace(/\//g, '&nbsp;&nbsp;&nbsp;&nbsp;');

                    return _.extend({}, concept, {
                        flattenedDisplayName: flattenedDisplayName,
                        ancestors: _.pluck(parents, 'id'),
                        indent: indent
                    });
                }
            }),

            /**
             * Ontology relationships
             *
             * @function
             */
            relationships: memoize(function() {
                return Promise.all([api.concepts(), getOntology()])
                    .then(function(results) {
                        var concepts = results.shift(),
                            ontology = results.shift(),
                            conceptIriIsVisible = function(iri) {
                                var concept = concepts.byId[iri];
                                return concept && concept.userVisible !== false;
                            },
                            list = _.chain(ontology.relationships)
                                .filter(function(r) {
                                    return _.some(r.domainConceptIris, conceptIriIsVisible) &&
                                        _.some(r.rangeConceptIris, conceptIriIsVisible)
                                })
                                .sortBy('displayName')
                                .value(),
                            groupedByRelated = {};

                        return {
                            list: list,
                            byId: ontology.relationships,
                            byTitle: ontology.relationships,
                            groupedBySourceDestConcepts: conceptGrouping(concepts, list, groupedByRelated),
                            groupedByRelatedConcept: groupedByRelated
                        };
                    });

                // Calculates cache with all possible mappings from source->dest
                // including all possible combinations of source->children and
                // dest->children
                function conceptGrouping(concepts, relationships, groupedByRelated) {
                    var groups = {},
                        addToAllSourceDestChildrenGroups = function(r, source, dest) {
                            var key = genSourceDestKey(source, dest);

                            if (!groups[key]) {
                                groups[key] = [];
                            }
                            if (!groupedByRelated[source]) {
                                groupedByRelated[source] = [];
                            }
                            if (!groupedByRelated[dest]) {
                                groupedByRelated[dest] = [];
                            }

                            groups[key].push(r);
                            if (groupedByRelated[source].indexOf(dest) === -1) {
                                groupedByRelated[source].push(dest);
                            }
                            if (groupedByRelated[dest].indexOf(source) === -1) {
                                groupedByRelated[dest].push(source);
                            }

                            var destConcept = concepts.byId[dest]
                            if (destConcept && destConcept.children) {
                                destConcept.children.forEach(function(c) {
                                    addToAllSourceDestChildrenGroups(r, source, c.id);
                                })
                            }

                            var sourceConcept = concepts.byId[source]
                            if (sourceConcept && sourceConcept.children) {
                                sourceConcept.children.forEach(function(c) {
                                    addToAllSourceDestChildrenGroups(r, c.id, dest);
                                });
                            }
                        };

                    relationships.forEach(function(r) {
                        r.domainConceptIris.forEach(function(source) {
                            r.rangeConceptIris.forEach(function(dest) {
                                addToAllSourceDestChildrenGroups(r, source, dest);
                            });
                        });
                    });

                    return groups;
                }
            }),

            /**
             * Get the valid relationships between concepts
             *
             * @function
             * @param {string} source Source concept IRI
             * @param {string} target Target concept IRI
             */
            relationshipsBetween: memoize(function(source, dest) {
                return api.relationships()
                    .then(function(relationships) {
                        var key = genSourceDestKey(source, dest);

                        return _.chain(relationships.groupedBySourceDestConcepts[key] || [])
                            .uniq(function(r) {
                                return r.title
                            })
                            .sortBy('displayName')
                            .value()
                    });
            }, genSourceDestKey)
        };

    return api;

    function genSourceDestKey(source, dest) {
        return [source, dest].join('>');
    }
});

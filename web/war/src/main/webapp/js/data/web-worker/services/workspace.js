
define([
    '../util/ajax',
    '../util/store',
    '../util/abort'
], function(ajax, store, abortPrevious) {
    'use strict';

    var api = {
        diff: function(workspaceId) {
            return ajax('GET', '/workspace/diff', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            });
        },

        getOrCreate: function() {
            return (publicData.currentWorkspaceId ? api.get() : Promise.reject())
                .catch(function() {
                    return api.all()
                        .then(function(workspaces) {
                            var workspace = _.findWhere(workspaces, { sharedToUser: false });
                            if (workspace) {
                                return api.get(workspace.workspaceId);
                            }
                            return api.create();
                        });
                });
        },

        all: function() {
            return ajax('GET', '/workspace/all')
                .then(function(result) {
                    return _.sortBy(result.workspaces, function(w) {
                        return w.title.toLowerCase();
                    });
                })
        },

        get: function(workspaceId) {
            return ajax('GET', '/workspace', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            }).then(function(workspace) {
                return store.setWorkspace(workspace);
            });
        },

        'delete': function(workspaceId) {
            return ajax('DELETE', '/workspace', {
                workspaceId: workspaceId
            });
        },

        store: function(workspaceId) {
            var workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace');
            return Promise.resolve(workspace && workspace.vertices || []);
        },

        histogramValues: function(workspaceId, property) {
            if (arguments.length === 1) {
                property = arguments[0];
                workspaceId = null;
            }

            return (property.title === 'ALL_DATES' ?
                Promise.require('data/web-worker/services/ontology')
                    .then(function(o) {
                        return o.ontology();
                    }) :
                Promise.resolve())
                    .then(function(ontology) {
                        var ontologyConcepts = ontology && ontology.concepts,
                            ontologyProperties = ontology && ontology.properties,
                            workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace'),
                            filteredVertexIds = store.getObject(workspace.workspaceId, 'filteredVertexIds'),
                            vertexIds = _.chain(workspace.vertices)
                                .omit(filteredVertexIds || [])
                                .keys()
                                .value(),
                            vertices = store.getObjects(workspace.workspaceId, 'vertex', vertexIds),
                            foundOntologyProperties = [],
                            foundYPropertiesByConcept = {},
                            values = _.chain(vertices)
                                .compact()
                                .map(function(v) {
                                    var conceptProperty = _.findWhere(v.properties, { name: 'http://visallo.org#conceptType'}),
                                        conceptPropertyIri = conceptProperty && conceptProperty.value ||
                                            'http://www.w3.org/2002/07/owl#Thing',
                                        properties = ontologyProperties ?
                                        _.filter(v.properties, function(p) {
                                            var ontologyProperty = ontologyProperties.byTitle[p.name],
                                                matched = ontologyProperty && ontologyProperty.dataType === property.dataType && ontologyProperty.userVisible !== false;

                                            if (matched) {
                                                var found = _.find(foundOntologyProperties, function(f) {
                                                    return f.property.title === ontologyProperty.title;
                                                });
                                                if (found) {
                                                    if (conceptPropertyIri &&
                                                        found.concepts.indexOf(conceptPropertyIri) === -1) {
                                                        found.concepts.push(conceptPropertyIri);
                                                    }
                                                } else {
                                                    foundOntologyProperties.push({
                                                        property: ontologyProperty,
                                                        concepts: conceptPropertyIri ? [conceptPropertyIri] : []
                                                    });
                                                }
                                            }

                                            return matched;
                                        }) :
                                        _.where(v.properties, { name: property.title }),

                                        concept = ontologyConcepts && ontologyConcepts.byId[conceptPropertyIri],
                                        eligibleYTypes = 'double integer currency number'.split(' '),
                                        foundYProperties = conceptPropertyIri && foundYPropertiesByConcept[conceptPropertyIri];

                                    if (conceptPropertyIri && !foundYProperties) {
                                        foundYProperties = [];
                                        while (concept) {
                                            for (var i = 0; i < concept.properties.length; i++) {
                                                var p = ontologyProperties.byTitle[concept.properties[i]];
                                                if (p && p.userVisible !== false && ~eligibleYTypes.indexOf(p.dataType)) {
                                                    foundYProperties.push(p);
                                                }
                                            }
                                            concept = concept.parentConcept &&
                                                ontologyConcepts &&
                                                ontologyConcepts.byId[concept.parentConcept];
                                        }
                                        foundYPropertiesByConcept[conceptPropertyIri] = foundYProperties;
                                    }

                                    var yValues = {};
                                    if (foundYProperties && foundYProperties.length) {
                                        _.each(v.properties, function(p) {
                                            _.each(foundYProperties, function(prop) {
                                                if (p.name === prop.title) {
                                                    if (!yValues[p.name]) yValues[p.name] = [];
                                                    yValues[p.name].push(p.value);
                                                }
                                            })
                                        })
                                    }

                                    return _.map(properties, function(p) {
                                        var ontologyProperty = ontologyProperties && ontologyProperties.byTitle[p.name];
                                        return {
                                            vertexId: v.id,
                                            conceptIri: conceptPropertyIri,
                                            propertyIri: ontologyProperty && ontologyProperty.title,
                                            value: p.value,
                                            yValues: yValues
                                        };
                                    })
                                })
                                .flatten(true)
                                .compact()
                                .value();

                        return { values: values, foundOntologyProperties: foundOntologyProperties };
                    })
        },

        save: abortPrevious(function(workspaceId, changes) {
            if (arguments.length === 1) {
                changes = workspaceId;
                workspaceId = publicData.currentWorkspaceId;
            }

            var workspace = store.getObject(workspaceId, 'workspace');

            if (_.isEmpty(changes)) {
                console.warn('Workspace update called with no changes');
                return Promise.resolve({ saved: false, workspace: workspace });
            }

            var allChanges = _.extend({}, {
                entityUpdates: [],
                entityDeletes: [],
                userUpdates: [],
                userDeletes: []
            }, changes || {});

            allChanges.entityUpdates.forEach(function(entityUpdate) {
                var p = entityUpdate.graphPosition,
                    layout = entityUpdate.graphLayoutJson;

                if (p) {
                    p.x = Math.round(p.x);
                    p.y = Math.round(p.y);
                }
                if (layout) {
                    entityUpdate.graphLayoutJson = JSON.stringify(layout);
                } else if (!p) {
                    console.error('Entity updates require either graphPosition or graphLayoutJson', entityUpdate);
                }
            })

            allChanges.entityUpdates = _.filter(allChanges.entityUpdates, function(update) {
                var inWorkspace = update.vertexId in workspace.vertices,
                    hasGraphPosition = 'graphPosition' in update;

                return !inWorkspace || hasGraphPosition;
            });

            if (!store.workspaceShouldSave(workspace, allChanges)) {
                return Promise.resolve({ saved: false, workspace: workspace });
            }

            return ajax('POST', '/workspace/update', {
                workspaceId: workspaceId,
                data: JSON.stringify(allChanges)
            }).then(function() {
                return { saved: true, workspace: store.updateWorkspace(workspaceId, allChanges) };
            })
        }),

        vertices: function(workspaceId) {
            return ajax('GET', '/workspace/vertices', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            });
        },

        publish: function(changes) {
            return ajax('POST', '/workspace/publish', {
                publishData: JSON.stringify(changes)
            });
        },

        undo: function(changes) {
            return ajax('POST', '/workspace/undo', {
                undoData: JSON.stringify(changes)
            });
        },

        edges: function(workspaceId, additionalVertices) {
            return ajax('GET', '/workspace/edges', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            }).then(function(result) {
                return result.edges;
            })
        },

        create: function(options) {
            return ajax('POST', '/workspace/create', options);
        }
    };

    return api;
})

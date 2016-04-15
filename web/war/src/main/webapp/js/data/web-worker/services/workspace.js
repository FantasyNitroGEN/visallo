
define([
    '../util/ajax',
    '../util/store',
    '../util/queue'
], function(ajax, store, queue) {
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

        current: function(workspaceId) {
            var workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace');
            return Promise.resolve(workspace);
        },

        store: function(workspaceId) {
            var workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace');
            return Promise.resolve(workspace && workspace.vertices || []);
        },

        storeEdges: function(workspaceId) {
            var workspaceEdges = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspaceEdges');
            return Promise.resolve(workspaceEdges || []);
        },

        histogramValues: function(workspaceId, property) {
            if (arguments.length === 1) {
                property = arguments[0];
                workspaceId = null;
            }

            var workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace'),
                edgeIds;

            return (property.title === 'ALL_DATES' ?
                Promise.all([
                        Promise.require('data/web-worker/services/ontology').then(function(o) {
                            return o.ontology();
                        }),
                        (edgeIds = _.pluck(store.getObjects(workspace.workspaceId, 'workspaceEdges'), 'edgeId')).length ?
                            Promise.require('data/web-worker/services/edge').then(function(edge) {
                                return edge.multiple({ edgeIds: edgeIds })
                            }) : Promise.resolve([])
                    ]) :
                Promise.resolve())
                    .then(function(results) {
                        var ontology = results && results.shift(),
                            edges = results && results.shift().edges,
                            ontologyConcepts = ontology && ontology.concepts,
                            ontologyRelationships = ontology && ontology.relationships,
                            ontologyProperties = ontology && ontology.properties,
                            filteredVertexIds = store.getObject(workspace.workspaceId, 'filteredVertexIds'),
                            vertexIds = _.chain(workspace.vertices)
                                .omit(filteredVertexIds || [])
                                .keys()
                                .value(),
                            vertices = store.getObjects(workspace.workspaceId, 'vertex', vertexIds),
                            foundOntologyProperties = [],
                            foundYPropertiesByConcept = {},
                            values = _.chain(vertices.concat(edges || []))
                                .compact()
                                .map(function(v) {
                                    var isEdge = ('label' in v && 'inVertexId' in v && 'outVertexId' in v),
                                        conceptProperty = !isEdge && _.findWhere(v.properties, { name: 'http://visallo.org#conceptType'}),
                                        conceptPropertyIri = isEdge ? v.label : (
                                            conceptProperty && conceptProperty.value || 'http://www.w3.org/2002/07/owl#Thing'
                                        ),
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
                                        ontologyByType = isEdge ?
                                            ontologyRelationships && ontologyRelationships.byTitle :
                                            ontologyConcepts && ontologyConcepts.byId,
                                        parentTypeField = isEdge ? 'parentIri' : 'parentConcept',
                                        concept = ontologyByType[conceptPropertyIri],
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
                                            concept = concept.parentConcept && ontologyByType[concept[parentTypeField]];
                                        }

                                        v.properties.forEach(function(prop) {
                                            if (!_.findWhere(foundYProperties, { title: prop.name })) {
                                                var p = ontologyProperties.byTitle[prop.name];
                                                if (p && p.userVisible !== false && ~eligibleYTypes.indexOf(p.dataType)) {
                                                    foundYProperties.push(p);
                                                }
                                            }
                                        });

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
                                        var ontologyProperty = ontologyProperties && ontologyProperties.byTitle[p.name],
                                            base = {
                                                conceptIri: conceptPropertyIri,
                                                propertyIri: ontologyProperty && ontologyProperty.title,
                                                value: p.value,
                                                yValues: yValues
                                            };

                                        base[isEdge ? 'edgeId' : 'vertexId'] = v.id;
                                        return base;
                                    })
                                })
                                .flatten(true)
                                .compact()
                                .sortBy('value')
                                .value();

                        _.each(values, function(v) {
                            var isDateOnlyProperty = _.some(ontologyProperties.byDataType.date, {
                                    title: v.propertyIri,
                                    displayType: 'dateOnly'
                                });

                            if (isDateOnlyProperty) {
                               v.value += (new Date(v.value).getTimezoneOffset() * 60000);
                            }
                        });

                        return { values: values, foundOntologyProperties: foundOntologyProperties };
                    })
        },

        save: queue(function(workspaceId, changes) {
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
            });
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
            var params = {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            };
            if (additionalVertices && additionalVertices.length) {
                params.ids = additionalVertices
            }

            return ajax('GET', '/workspace/edges', params).then(function(result) {
                return result.edges;
            })
        },

        create: function(options) {
            return ajax('POST', '/workspace/create', options);
        }
    };

    return api;
})

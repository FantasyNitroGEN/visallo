
define([
    'flight/lib/component',
    'react-dom',
    'jsx!./DiffPanel',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    ReactDOM,
    DiffPanel,
    F,
    withDataRequest) {
    'use strict';

    var SHOW_CHANGES_TEXT_SECONDS = 3;

    return defineComponent(Diff, withDataRequest);

    function titleForEdgesVertices(vertex, vertexId, diffsGroupedByElement) {
        if (vertex) {
            return F.vertex.title(vertex);
        }

        var matchingDiffs = diffsGroupedByElement[vertexId],
            diff = matchingDiffs && matchingDiffs.length && matchingDiffs[0];

        if (diff && diff.title) {
            return diff.title;
        }

        return i18n('vertex.property.title.not_available');
    }

    function Diff() {

        this.render = function() {
            ReactDOM.render(DiffPanel({
                diffs: this.diffs,
                formatLabel: this.formatLabel,
                onPublishClick: this.onMarkPublish.bind(this),
                onUndoClick: this.onMarkUndo.bind(this),
                onSelectAllPublishClick: this.onSelectAllPublish.bind(this),
                onSelectAllUndoClick: this.onSelectAllUndo.bind(this),
                onDeselectAllClick: this.onDeselectAll.bind(this),
                publishing: this.publishing,
                undoing: this.undoing,
                onApplyPublishClick: this.onApplyPublishClick.bind(this),
                onApplyUndoClick: this.onApplyUndoClick.bind(this),
                onVertexRowClick: this.onVertexRowClick.bind(this),
                onEdgeRowClick: this.onEdgeRowClick.bind(this)
            }), this.$node[0]);
        };

        this.after('initialize', function() {
            var self = this;

            this.dataRequest('ontology', 'ontology').done(function(ontology) {
                self.ontologyConcepts = ontology.concepts;
                self.ontologyProperties = ontology.properties;
                self.ontologyRelationships = ontology.relationships;
                self.formatLabel = function(name) {
                    return self.ontologyProperties.byTitle[name].displayName;
                };
                self.setup();
            })
        });

        this.setup = function() {
            var self = this;
            this.diffs = [];

            self.processDiffs(self.attr.diffs).done(function(processDiffs) {
                self.diffs = processDiffs;
                self.render();
                self.updateVisibility();
                self.updateDraggables();
            });

            self.on('diffsChanged', function(event, data) {
                self.processDiffs(data.diffs).done(function(processDiffs) {
                    self.diffs = processDiffs;
                    self.render();
                    self.updateVisibility();
                    self.updateDraggables();
                });
            })
            self.on(document, 'objectsSelected', self.onObjectsSelected);
        };

        this.processDiffs = function(diffs) {
            var self = this,
                referencedVertices = [],
                referencedEdges = [],
                groupedByElement = _.groupBy(diffs, function(diff) {
                    if (diff.elementType === 'vertex' || diff.type === 'VertexDiffItem') {
                        referencedVertices.push(diff.vertexId || diff.elementId || diff.outVertexId);
                    } else if (diff.elementType === 'edge' || diff.type === 'EdgeDiffItem') {
                        referencedEdges.push(diff.edgeId || diff.elementId);
                    }
                    if (diff.inVertexId) {
                        referencedVertices.push(diff.inVertexId);
                    }
                    if (diff.outVertexId) {
                        referencedVertices.push(diff.outVertexId);
                    }
                    if (diff.vertexId) return diff.vertexId;
                    if (diff.edgeId) return diff.edgeId;
                    if (diff.elementId) return diff.elementId;
                    return diff.outVertexId;
                }),
                output = [];

            return Promise.all([
                this.dataRequest('vertex', 'store', { vertexIds: _.unique(referencedVertices) }),
                this.dataRequest('edge', 'store', { edgeIds: _.unique(referencedEdges) }),
                visalloData.selectedObjectsPromise()
            ]).spread(function(vertexResults, edgeResults, selectedObjects) {
                    var vertices = _.compact(vertexResults),
                        edges = _.compact(edgeResults),
                        verticesById = _.indexBy(vertices, 'id'),
                        edgesById = _.indexBy(edges, 'id'),
                        selectedById = selectedObjects.vertices.concat(selectedObjects.edges)
                            .map(function(object) { return object.id; })
                            .reduce(function(selected, id) {
                                selected[id] = true;
                                return selected;
                            }, {}),
                        previousDiffsById = self.diffsById || {};
                    self.diffsForElementId = {};
                    self.diffsById = {};
                    self.diffDependencies = {};
                    self.undoDiffDependencies = {};

                    _.each(groupedByElement, function(diffs, elementId) {
                        var actionTypes = {
                                CREATE: { type: 'create', display: i18n('workspaces.diff.action.types.create') },
                                UPDATE: { type: 'update', display: i18n('workspaces.diff.action.types.update') },
                                DELETE: { type: 'delete', display: i18n('workspaces.diff.action.types.delete') }
                            },
                            outputItem = {
                                properties: [],
                                action: {},
                                active: selectedById[elementId],
                                publish: previousDiffsById[elementId] && previousDiffsById[elementId].publish,
                                undo: previousDiffsById[elementId] && previousDiffsById[elementId].undo,
                                className: F.className.to(elementId)
                            },
                            isElementVertex = (
                                diffs[0].elementType === 'vertex' ||
                                diffs[0].type === 'VertexDiffItem'
                            );

                        if (isElementVertex) {
                            outputItem.vertexId = elementId;
                            outputItem.vertex = verticesById[elementId];
                            if (outputItem.vertex) {
                                outputItem.title = F.vertex.title(outputItem.vertex);
                            } else {
                                outputItem.vertex = {
                                    id: elementId,
                                    properties: [],
                                    'http://visallo.org#visibilityJson': diffs[0]['http://visallo.org#visibilityJson']
                                };
                                outputItem.title = diffs[0].title || i18n('vertex.property.title.not_available');
                            }
                            if (diffs[0].conceptType) {
                                var concept = self.ontologyConcepts.byId[diffs[0].conceptType];
                                if (concept) {
                                    outputItem.conceptImage = concept.glyphIconHref;
                                    outputItem.selectedConceptImage = concept.glyphIconSelectedHref || concept.glyphIconHref;
                                }
                            }
                        } else {
                            outputItem.edgeId = elementId;
                            outputItem.edge = edgesById[elementId];
                            if (outputItem.edge) {
                                outputItem.edgeLabel = self.ontologyRelationships.byTitle[outputItem.edge.label]
                                .displayName;
                            } else {
                                outputItem.edge = {
                                    id: elementId,
                                    properties: [],
                                    'http://visallo.org#visibilityJson': diffs[0].visibilityJson
                                };
                                outputItem.edgeLabel = self.ontologyRelationships.byTitle[diffs[0].label].displayName;
                            }

                            var sourceId = diffs[0].outVertexId,
                                targetId = diffs[0].inVertexId,
                                source = verticesById[sourceId],
                                target = verticesById[targetId];
                            outputItem.sourceTitle = titleForEdgesVertices(source, sourceId, groupedByElement);
                            outputItem.targetTitle = titleForEdgesVertices(target, targetId, groupedByElement);
                        }

                        diffs.forEach(function(diff) {

                            switch (diff.type) {
                                case 'VertexDiffItem':
                                    diff.id = elementId;
                                    diff.publish = outputItem.publish;
                                    diff.undo = outputItem.undo;
                                    outputItem.action = diff.deleted ? actionTypes.DELETE : actionTypes.CREATE;
                                    self.diffsForElementId[elementId] = diff;
                                    self.diffsById[elementId] = diff;
                                    addDiffDependency(diff.id);
                                    break;

                                case 'PropertyDiffItem':
                                    var ontologyProperty = self.ontologyProperties.byTitle[diff.name];
                                    var compoundProperty = self.ontologyProperties.byDependentToCompound[diff.name];
                                    var isDependent = !!diff.dependentName;

                                    if (ontologyProperty && ontologyProperty.userVisible) {
                                        if (!isDependent) {
                                            diff.id = elementId + diff.name + diff.key;
                                            diff.publish = previousDiffsById[diff.id] && previousDiffsById[diff.id].publish;
                                            diff.undo = previousDiffsById[diff.id] && previousDiffsById[diff.id].undo;
                                            addDiffDependency(diff.elementId, diff);
                                            diff.className = F.className.to(diff.id);
                                        }

                                        if (compoundProperty &&
                                            F.vertex.hasProperty(outputItem.vertex, compoundProperty)) {

                                            diff.dependentName = diff.name;
                                            diff.name = compoundProperty;
                                            var previousPropertyWithKey = _.findWhere(outputItem.properties, {
                                                key: diff.key,
                                                name: diff.name
                                            })
                                            if (previousPropertyWithKey) {
                                                if (previousPropertyWithKey.old) {
                                                    previousPropertyWithKey.old.push(diff.old);
                                                }
                                                if (previousPropertyWithKey.new) {
                                                    previousPropertyWithKey.new.push(diff.new);
                                                }
                                                previousPropertyWithKey.diffs.push(diff)
                                            } else {
                                                if (diff.old) {
                                                    diff.old = [diff.old];
                                                }
                                                if (diff.new) {
                                                    diff.new = [diff.new];
                                                }
                                                diff.diffs = [diff];
                                                outputItem.properties.push(diff)
                                            }
                                        } else {
                                            outputItem.properties.push(diff)
                                        }
                                        self.diffsById[diff.id] = diff;
                                    }
                                    break;

                                case 'EdgeDiffItem':
                                    diff.id = diff.edgeId;
                                    diff.publish = outputItem.publish;
                                    diff.undo = outputItem.undo;
                                    diff.inVertex = verticesById[diff.inVertexId];
                                    diff.outVertex = verticesById[diff.outVertexId];
                                    diff.className = F.className.to(diff.edgeId);
                                    diff.displayLabel = self.ontologyRelationships.byTitle[diff.label].displayName;
                                    self.diffsForElementId[diff.edgeId] = diff;
                                    outputItem.action = diff.deleted ? actionTypes.DELETE : actionTypes.CREATE;
                                    addDiffDependency(diff.inVertexId, diff);
                                    addDiffDependency(diff.outVertexId, diff);
                                    self.diffsById[diff.id] = diff;
                                    break;

                                default:
                                    console.warn('Unknown diff item type', diff.type)
                            }

                            addDiffDependency(diff.id);
                        });

                        if (_.isEmpty(outputItem.action)) {
                            outputItem.action = actionTypes.UPDATE;
                        }

                        output.push(outputItem);
                    });

                    return output;
            });

            function addDiffDependency(id, diff) {
                if (!self.diffDependencies[id]) {
                    self.diffDependencies[id] = [];
                }
                if (diff) {
                    self.diffDependencies[id].push(diff.id);

                    // Undo dependencies are inverse
                    if (!self.undoDiffDependencies[diff.id]) {
                        self.undoDiffDependencies[diff.id] = [];
                    }
                    self.undoDiffDependencies[diff.id].push(id);
                }
            }
        };

        this.onObjectsSelected = function(event, data) {
            var vertices = data.vertices,
                edges = data.edges;

            this.diffs.forEach(function(diff) {
                diff.active = _.findWhere(vertices, { id: diff.vertexId }) || _.findWhere(edges, { id: diff.edgeId });
            });
            this.render();
        };

        this.onVertexRowClick = function(vertexId) {
            this.trigger('selectObjects', {
                vertexIds: vertexId ? [vertexId] : []
            });
        };

        this.onEdgeRowClick = function(edgeId) {
            this.trigger('selectObjects', {
                edgeIds: edgeId ? [edgeId] : []
            });
        };

        this.onDeselectAll = function() {
            var self = this;
            this.diffs.forEach(function(diff) {
                deselectAction(diff);
                diff.properties.forEach(function(property) {
                    deselectAction(property);
                });
            });
            Object.keys(this.diffsById).forEach(function(id) {
                deselectAction(self.diffsById[id]);
            });
            this.render();

            function deselectAction(diff, action) {
                diff.publish = false;
                diff.undo = false;
            }
        };

        this.onSelectAll = function(action) {
            var self = this;
            this.diffs
                .filter(function(diff) { return diff.action.type !== 'update' })
                .forEach(function(diff) {
                    selectAction(diff, action);
                    diff.properties.forEach(function(property) {
                        selectAction(property, action);
                    });
                });
            Object.keys(this.diffsById).forEach(function(id) {
                selectAction(self.diffsById[id], action);
            });
            this.render();

            function selectAction(diff, action) {
                diff.publish = false;
                diff.undo = false;
                diff[action] = true;
            }
        };
        this.onSelectAllPublish = _.partial(this.onSelectAll, 'publish');
        this.onSelectAllUndo = _.partial(this.onSelectAll, 'undo');

        this.onApplyAll = function(type) {
            var self = this,
                diffsToSend = this.buildDiffsToSend(type);
            this.publishing = type === 'publish';
            this.undoing = type === 'undo';
            this.render();

            this.dataRequest('workspace', type, diffsToSend)
                .finally(function() {
                    self.publishing = self.undoing = false;
                    self.trigger(document, 'updateDiff');
                })
                .then(function(response) {
                    var failures = response.failures,
                        success = response.success,
                        nextDiffs = self.buildNextDiffs(type, failures);

                    return self
                        .processDiffs(nextDiffs)
                        .then(function(processDiffs) {
                            if (processDiffs.length) {
                                self.diffs = processDiffs;
                                self.render();
                                self.updateVisibility();
                                self.updateDraggables();

                                if (failures && failures.length) {
                                    var error = $('<div>')
                                        .addClass('alert alert-error')
                                        .html(
                                            '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                                            '<ul><li>' + _.pluck(failures, 'errorMessage').join('</li><li>') + '</li></ul>'
                                        )
                                        .prependTo(self.$node.find('.diff-content'))
                                        .alert();
                                }

                                if (type === 'undo') {
                                    self.trigger('loadCurrentWorkspace');
                                }
                            } else {
                                self.trigger('toggleDiffPanel');
                            }
                        });
                })
                .catch(function(errorText) {
                    //TODO move to react
                    var error = $('<div>')
                        .addClass('alert alert-error')
                        .html(
                            '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                            i18n('workspaces.diff.error', type, errorText)
                        )
                        .prependTo(self.$node.find('.diff-content'))
                        .alert();

                    _.delay(error.remove.bind(error), 5000)
                });
        };
        this.onApplyPublishClick = _.partial(this.onApplyAll, 'publish');
        this.onApplyUndoClick = _.partial(this.onApplyAll, 'undo');

        this.buildDiffsToSend = function(applyType) {
            var self = this,
                diffsToSend = [];
            this.diffs.forEach(function(diff) {
                var vertexId = diff.vertexId,
                    edgeId = diff.edgeId,
                    properties = diff.properties;
                if (diff[applyType]) {
                    if (diff.vertex) {
                        diffsToSend.push(vertexDiffToSend(diff));
                    } else if (diff.edge) {
                        diffsToSend.push(edgeDiffToSend(diff));
                    }
                    diff.applying = self.diffsById[vertexId || edgeId].applying = true;
                }
                properties
                    .filter(function(property) { return property[applyType]; })
                    .forEach(function(property) {
                        property.applying = self.diffsById[property.id].applying = true;
                        if (property.diffs) {
                            diffsToSend = diffsToSend.concat(property.diffs.map(propertyDiffToSend))
                        } else {
                            diffsToSend.push(propertyDiffToSend(property));
                        }
                    });
            });
            return diffsToSend;

            function vertexDiffToSend(diff) {
                var vertex = self.diffsById[diff.vertexId];

                return {
                    type: 'vertex',
                    vertexId: diff.vertexId,
                    action: vertex.deleted ? 'delete' : 'create',
                    status: vertex.sandboxStatus
                };
            }

            function edgeDiffToSend(diff) {
                var edge = self.diffsById[diff.edgeId];

                return {
                    type: 'relationship',
                    edgeId: diff.edgeId,
                    sourceId: edge.outVertexId,
                    destId: edge.inVertexId,
                    action: edge.deleted ? 'delete' : 'create',
                    status: edge.sandboxStatus
                };
            }

            function propertyDiffToSend(diff) {
                var diffToSend = {
                    type: 'property',
                    key: diff.key,
                    name: diff.dependentName || diff.name,
                    action: diff.deleted ? 'delete' : 'update',
                    status: diff.sandboxStatus
                };
                diffToSend[diff.elementType + 'Id'] = diff.elementId;
                return diffToSend;
            }
        };

        this.buildNextDiffs = function(applyType, failures) {
            var self = this,
                failuresById = failures.reduce(function(failures, failure) {
                    var type = failure.type,
                        vertexId = failure.vertexId,
                        edgeId = failure.edgeId,
                        name = failure.name,
                        key = failure.key,
                        id;

                    switch (type) {
                        case 'vertex':
                            id = failure.vertexId;
                            break;
                        case 'relationship':
                            id = failure.edgeId;
                            break;
                        case 'property':
                            id = (vertexId || edgeId) + name + key;
                            break;
                    }
                    failures[id] = true;
                    return failures;
                }, {});
            return Object.keys(self.diffsById)
                .map(function(id) {
                    return self.diffsById[id];
                })
                .reduce(function(diffsToProcess, diff) {
                    if (!diff.applying || failuresById[diff.id]) {
                        diffsToProcess.push(diff);
                    }
                    diff[applyType] = diff.applying ? failuresById[diff.id] : diff[applyType];
                    diff.applying = false;
                    return diffsToProcess;
                }, []);
        };

        //TODO handle in react
        this.updateDraggables = function() {
            this.$node.find('.vertex-label h1')
                .draggable({
                    appendTo: 'body',
                    helper: 'clone',
                    revert: 'invalid',
                    revertDuration: 250,
                    scroll: false,
                    zIndex: 100,
                    distance: 10,
                    start: function(event, ui) {
                        ui.helper.css({
                            paddingLeft: $(this).css('padding-left'),
                            paddingTop: 0,
                            paddingBottom: 0,
                            margin: 0,
                            fontSize: $(this).css('font-size')
                        })
                    }
                });

            this.$node.droppable({ tolerance: 'pointer', accept: '*' });
        };

        this.updateVisibility = function() {
            var self = this;

            require(['util/visibility/view'], function(Visibility) {
                Visibility.teardownAll();
                self.$node.find('.visibility').each(function() {
                    var node = $(this),
                        visibility = JSON.parse(node.attr('data-visibility'));

                    Visibility.attachTo(node, {
                        value: visibility && visibility.source
                    });
                });
            });
        };

        this.onMarkUndo = function(diffId, state) {
            var self = this,
                diff = this.diffsById[diffId],
                deps = this.diffDependencies[diffId] || [],
                vertexDiff;
            state = state === undefined ? !diff.undo : state;

            switch (diff.type) {
                case 'VertexDiffItem':
                    vertexDiff = _.findWhere(this.diffs, { vertexId: diffId});
                    vertexDiff.undo = diff.undo = state;
                    vertexDiff.publish = diff.publish = false;

                    if (state) {
                        if (!diff.deleted) {
                            deps.forEach(function(diffId) {
                                self.onMarkUndo(diffId, true);
                                self.trigger('markUndoDiffItem', { diffId: diffId, state: true });
                            })
                        }
                    }

                    break;

                case 'PropertyDiffItem':
                    var byId = {};
                    byId[diff.elementType + 'Id'] = diff.elementId;
                    var propertyDiff = _.chain(this.diffs)
                        .findWhere(byId)
                        .reduce(function(result, val, key) { return key === 'properties' ? val : result})
                        .findWhere({ id: diffId })
                        .value();

                    if (propertyDiff) {
                        propertyDiff.undo = diff.undo = state;
                        propertyDiff.publish = diff.publish = false;
                    }

                    if (!state) {
                        vertexDiff = self.diffsForElementId[diff.elementId];
                        if (vertexDiff && vertexDiff.undo) {
                            self.onMarkUndo(vertexDiff.id, false);
                            self.trigger('markUndoDiffItem', { diffId: vertexDiff.id, state: false });
                        }
                    }

                    break;

                case 'EdgeDiffItem':
                    var edgeDiff = _.findWhere(this.diffs, { edgeId: diffId });
                    edgeDiff.undo = diff.undo = state;
                    edgeDiff.publish = diff.publish = false;

                    var inVertex = self.diffsForElementId[diff.inVertexId],
                        outVertex = self.diffsForElementId[diff.outVertexId];

                    if (state) {
                        if (diff.deleted) {
                            if (inVertex && inVertex.deleted) {
                                self.onMarkUndo(inVertex.id, true);
                                self.trigger('markUndoDiffItem', { diffId: inVertex.id, state: true });
                            }
                            if (outVertex && outVertex.deleted) {
                                self.onMarkUndo(outVertex.id, true);
                                self.trigger('markUndoDiffItem', { diffId: outVertex.id, state: true });
                            }
                        } else {
                            deps.forEach(function(diffId) {
                                self.onMarkUndo(diffId, true);
                                self.trigger('markUndoDiffItem', { diffId: diffId, state: true });
                            })
                        }
                    } else {
                        if (inVertex) {
                            self.onMarkUndo(inVertex.id, false);
                            self.trigger('markUndoDiffItem', { diffId: inVertex.id, state: false });
                        }

                        if (outVertex) {
                            self.onMarkUndo(outVertex.id, false);
                            self.trigger('markUndoDiffItem', { diffId: outVertex.id, state: false });
                        }
                    }

                    break;

                default: console.warn('Unknown diff item type', diff.type)
            }
            this.render();
        };

        this.onMarkPublish = function(diffId, state) {
            var self = this,
                diff = this.diffsById[diffId],
                vertexDiff;
            state = state === undefined ? !diff.publish : state;

            switch (diff.type) {

                case 'VertexDiffItem':
                    vertexDiff = _.findWhere(this.diffs, { vertexId: diffId });
                    vertexDiff.publish = diff.publish = state;
                    vertexDiff.undo = diff.undo = false;
                    if (state && diff.deleted) {
                        this.diffDependencies[diff.id].forEach(function(diffId) {
                            var diff = self.diffsById[diffId];
                            if (diff && diff.type === 'EdgeDiffItem' && diff.deleted) {
                                self.onMarkPublish(diffId, true);
                                self.trigger('markPublishDiffItem', { diffId: diffId, state: true });
                            } else {
                                self.onMarkPublish(diffId, false);
                                self.trigger('markPublishDiffItem', { diffId: diffId, state: false });
                            }
                        });
                    } else if (!state) {
                        this.diffDependencies[diff.id].forEach(function(diffId) {
                            self.onMarkPublish(diffId, false);
                            self.trigger('markPublishDiffItem', { diffId: diffId, state: false });
                        });
                    }

                    break;

                case 'PropertyDiffItem':
                    var byId = {};
                    byId[diff.elementType + 'Id'] = diff.elementId;
                    var propertyDiff = _.chain(this.diffs)
                        .findWhere(byId)
                        .reduce(function(result, val, key) { return key === 'properties' ? val : result})
                        .findWhere({ id: diffId })
                        .value();

                    if (propertyDiff) {
                        propertyDiff.publish = diff.publish = state;
                        propertyDiff.undo = diff.undo = false;
                    }

                    if (state) {
                        vertexDiff = this.diffsForElementId[diff.elementId];
                        if (vertexDiff && !vertexDiff.deleted) {
                            self.onMarkPublish(diff.elementId, true);
                            this.trigger('markPublishDiffItem', { diffId: diff.elementId, state: true })
                        }
                    }

                    break;

                case 'EdgeDiffItem':
                    var edgeDiff = _.findWhere(this.diffs, { edgeId: diffId });
                    edgeDiff.publish = diff.publish = state;
                    edgeDiff.undo = diff.undo = false;

                    if (!state) {
                        // Unpublish all dependents
                        this.diffDependencies[diff.id].forEach(function(diffId) {
                            self.onMarkPublish(diffId, false);
                            self.trigger('markPublishDiffItem', { diffId: diffId, state: false });
                        });
                    } else {
                        var inVertexDiff = this.diffsForElementId[diff.inVertexId],
                            outVertexDiff = this.diffsForElementId[diff.outVertexId];

                        if (inVertexDiff && !diff.deleted) {
                            self.onMarkPublish(diff.inVertexId, true);
                            this.trigger('markPublishDiffItem', { diffId: diff.inVertexId, state: true });
                        }
                        if (outVertexDiff && !diff.deleted) {
                            self.onMarkPublish(diff.outVertexId, true);
                            this.trigger('markPublishDiffItem', { diffId: diff.outVertexId, state: true });
                        }
                    }

                    break;

                default: console.warn('Unknown diff item type', diff.type)
            }
            this.render();
        };
    }
});

define(['util/undoManager'], function(UndoManager) {
    'use strict';

    return withWorkspaces;

    function withWorkspaces() {
        var lastReloadedState,
            undoManagersPerWorkspace = {};

        this.after('initialize', function() {
            var self = this;

            this.fireApplicationReadyOnce = _.once(this.trigger.bind(this, 'applicationReady'));

            this.on('loadCurrentWorkspace', this.onLoadCurrentWorkspace);
            this.on('switchWorkspace', this.onSwitchWorkspace);
            this.on('reloadWorkspace', this.onReloadWorkspace);
            this.on('updateWorkspace', this.onUpdateWorkspace);
            this.on('loadEdges', this.onLoadEdges);

            this.on(window, 'keydown', function(event) {
                var character = String.fromCharCode(event.which).toUpperCase();

                if ($(event.target).hasClass('clipboardManager')) {
                    if (_isUndo(character, event)) {
                        perform('performUndo');
                        event.preventDefault();
                    }

                    if (_isRedo(character, event)) {
                        perform('performRedo');
                        event.preventDefault();
                    }
                }

                function perform(type) {
                    var undoManager = undoManagersPerWorkspace[self.visalloData.currentWorkspaceId];
                    if (undoManager) {
                        undoManager[type]();
                    }
                }

                function _isUndo(character, event) {
                    return (
                        // Windows
                        (character === 'Z' && event.ctrlKey) ||
                        // Mac
                        (character === 'Z' && event.metaKey && !event.shiftKey)
                    );
                }
                function _isRedo(character, event) {
                    return (
                        // Windows
                        (character === 'Y' && event.ctrlKey) ||
                        // Mac
                        (character === 'Z' && event.metaKey && event.shiftKey)
                    );
                }
            })
        });

        this.onLoadCurrentWorkspace = function(event) {
            var currentWorkspaceId = this.visalloData.currentWorkspaceId;
            this.trigger('switchWorkspace', { workspaceId: currentWorkspaceId });
        };

        this.onReloadWorkspace = function() {
            if (lastReloadedState) {
                this.workspaceLoaded(lastReloadedState);
                this.edgesLoaded(lastReloadedState.edges);
            }
        };

        this.onLoadEdges = function(event, data) {
            var self = this;
            this.dataRequestPromise.done(function(dataRequest) {
                dataRequest('workspace', 'edges', data && data.workspaceId, data && data.vertexIds)
                    .done(function(edges) {
                        self.edgesLoaded({ edges: edges });
                    })
            });
        };

        this.onSwitchWorkspace = function(event, data) {
            lastReloadedState = {};
            this.setPublicApi('currentWorkspaceId', data.workspaceId);
            this.setPublicApi('currentWorkspaceEditable', data.editable);
            this.worker.postMessage({
                type: 'workspaceSwitch',
                workspaceId: data.workspaceId
            });
        };

        this.onUpdateWorkspace = function(event, data) {
            var self = this,
                triggered = false,
                buffer = _.delay(function() {
                    triggered = true;
                    self.trigger('workspaceSaving', lastReloadedState.workspace);
                }, 250);

            if (!data.ignoreUndoManager && (!_.isEmpty(data.entityUpdates) || !_.isEmpty(data.entityDeletes))) {
                var invertData = inverse(data, lastReloadedState.workspace.vertices),
                    undoManager = undoManagerForWorkspace(lastReloadedState.workspace.workspaceId);
                undoManager.performedAction('Workspace Update', {
                    undo: function() {
                        self.trigger('updateWorkspace', $.extend({}, invertData, { ignoreUndoManager: true }));
                    },
                    redo: function() {
                        self.trigger('updateWorkspace', $.extend({}, data, { ignoreUndoManager: true }));
                    }
                })
            }

            this.dataRequestPromise.done(function(dataRequest) {
                dataRequest('workspace', 'save', data)
                    .then(function(data) {
                        clearTimeout(buffer);
                        if (data.saved) {
                            triggered = true;
                        }
                    })
                    .catch(function(e) {
                        console.error(e);
                    })
                    .then(function() {
                        if (triggered) {
                            self.trigger('workspaceSaved', lastReloadedState.workspace);
                        }
                    })
            });
        };

        // Worker Handlers

        this.edgesLoaded = function(message) {
            var self = this,
                edgeIds = _.pluck(message.edges, 'edgeId');

            lastReloadedState.edges = message;
            this.trigger('edgesLoaded', message);
            this.loadFullEdges(edgeIds);
        };

        this.workspaceUpdated = function(message) {
            if (lastReloadedState &&
                lastReloadedState.workspace &&
                lastReloadedState.workspace.workspaceId === message.workspace.workspaceId) {
                lastReloadedState.workspace = message.workspace;
            }
            this.trigger('workspaceUpdated', message);
            if (message.newVertices.length) {
                this.trigger('loadEdges', {
                    workspaceId: message.workspace.workspaceId,
                    vertexIds: _.pluck(message.newVertices, 'id')
                });
            }
        };

        this.workspaceLoaded = function(message) {
            lastReloadedState = message;
            var workspace = message.workspace;
            workspace.data = {
                vertices: message.vertices
            };
            this.setPublicApi('currentWorkspaceId', workspace.workspaceId);
            this.setPublicApi('currentWorkspaceEditable', workspace.editable);
            this.setPublicApi('currentWorkspaceCommentable', workspace.commentable);
            this.trigger('workspaceLoaded', workspace);
            this.trigger('selectObjects');
            this.fireApplicationReadyOnce();
        };

        this.loadFullEdges = function(edgeIds) {
            var self = this;
            return this.dataRequestPromise.then(function(dataRequest) {
                if (edgeIds.length) {
                    return dataRequest('edge', 'multiple', { edgeIds: edgeIds })
                }
                return null;
            }).then(function(data) {
                if (data) {
                    self.trigger('edgesLoaded', data);
                }
            });
        };

        function undoManagerForWorkspace(workspaceId) {
            var undoManager = undoManagersPerWorkspace[workspaceId];
            if (!undoManager) {
                undoManager = undoManagersPerWorkspace[workspaceId] = new UndoManager();
            }
            return undoManager;
        }

        function padWorkspaceUpdate(workspaceUpdates) {
            'entityUpdates entityDeletes userUpdates userDeletes'.split(' ').forEach(function(k) {
                if (!(k in workspaceUpdates)) {
                    workspaceUpdates[k] = [];
                }
            });
            return workspaceUpdates;
        }

        function inverse(workspaceUpdates, workspaceVertices) {
            var inverted = {
                entityUpdates: [],
                entityDeletes: []
            };

            if (workspaceUpdates.entityUpdates) {
                workspaceUpdates.entityUpdates.forEach(function(update) {
                    inverted.entityUpdates.push($.extend(true, {}, workspaceVertices[update.vertexId]));
                })
            }
            if (workspaceUpdates.entityDeletes) {
                workspaceUpdates.entityDeletes.forEach(function(update) {
                    inverted.entityUpdates.push($.extend(true, {}, workspaceVertices[update]));
                })
            }

            return inverted;
        }
    }
});

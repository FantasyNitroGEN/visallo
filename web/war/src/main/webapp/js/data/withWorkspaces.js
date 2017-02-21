define([], function() {
    'use strict';

    return withWorkspaces;

    function withWorkspaces() {
        var workspace,
            undoManagersPerWorkspace = {};

        this.after('initialize', function() {
            var self = this;

            this.fireApplicationReadyOnce = _.once(this.trigger.bind(this, 'applicationReady'));

            this.on('loadCurrentWorkspace', this.onLoadCurrentWorkspace);
            this.on('switchWorkspace', this.onSwitchWorkspace);
            this.on('updateWorkspace', this.onUpdateWorkspace);
            this.on('undo', this.onUndo);
            this.on('redo', this.onRedo);

            visalloData.storePromise.then(store => {
                const selectId = (s) => s.workspace.currentId || null;
                const select = (s) => selectId(s) && s.workspace.byId[selectId(s)] || null;

                let previous = store.getState();

                store.subscribe(() => {
                    const state = store.getState()
                    const oldWorkspace = select(previous);
                    const newWorkspace = select(state);
                    const changed = newWorkspace && (!oldWorkspace || oldWorkspace.workspaceId !== newWorkspace.workspaceId);

                    if (changed) {
                        workspace = {...newWorkspace};
                        this.setPublicApi('currentWorkspaceId', workspace.workspaceId);
                        this.setPublicApi('currentWorkspaceName', workspace.title);
                        this.setPublicApi('currentWorkspaceEditable', workspace.editable);
                        this.setPublicApi('currentWorkspaceCommentable', workspace.commentable);
                        this.trigger('workspaceLoaded', workspace);
                        this.trigger('selectObjects');
                        this.fireApplicationReadyOnce();
                    } else {
                        _.each(state.workspace.byId, (workspace, id) => {
                            const workspaceChanged = previous.workspace.byId[id] && previous.workspace.byId[id] !== workspace;
                            if (workspaceChanged) {
                                this.setPublicApi('currentWorkspaceName', workspace.title);
                                this.setPublicApi('currentWorkspaceEditable', workspace.editable);
                                this.setPublicApi('currentWorkspaceCommentable', workspace.commentable);
                                this.trigger('workspaceUpdated', { workspace })
                            }
                        });
                        const deletedKeys = Object.keys(_.omit(previous.workspace.byId, Object.keys(state.workspace.byId)));
                        if (deletedKeys.length) {
                            deletedKeys.forEach(workspaceId => {
                                this.trigger('workspaceDeleted', { workspaceId });
                            })
                        }
                    }
                    previous = state;
                })
            })
        });

        this.onLoadCurrentWorkspace = function(event) {
            var currentWorkspaceId = this.visalloData.currentWorkspaceId;
            this.trigger('switchWorkspace', { workspaceId: currentWorkspaceId });
        };

        this.onSwitchWorkspace = function(event, data) {
            this.setPublicApi('currentWorkspaceId', data.workspaceId);
            Promise.all([
                visalloData.storePromise,
                Promise.require('data/web-worker/store/workspace/actions')
            ]).spread(function(store, workspaceActions) {
                store.dispatch(workspaceActions.setCurrent(data.workspaceId))
            });
        };

        this.onUpdateWorkspace = function(event, data) {
            var self = this,
                triggered = false,
                buffer = _.delay(function() {
                    triggered = true;
                    self.trigger('workspaceSaving', workspace);
                }, 250),
                result,
                legacyKeys = ['entityUpdates', 'entityDeletes'],
                legacy = _.pick(data, legacyKeys);

            if (legacy.length) {
                data = _.omit(data, legacyKeys);
                console.warn('updateWorkspace no longer accepts entity changes');
            }

            if (!_.isEmpty(data)) {
                this.dataRequestPromise.then(function(dataRequest) {
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
                                self.trigger('workspaceSaved', result);
                            }
                        })
                });
            }
        };

        this.onUndo = function() {
            Promise.all([
                visalloData.storePromise,
                Promise.require('data/web-worker/store/undo/actions')
            ]).spread((store, actions) => {
                const scope = this.visalloData.currentWorkspaceId;
                store.dispatch(actions.undoForProduct());
            });
        };

        this.onRedo = function() {
            Promise.all([
                visalloData.storePromise,
                Promise.require('data/web-worker/store/undo/actions')
            ]).spread((store, actions) => {
                const scope = this.visalloData.currentWorkspaceId;
                store.dispatch(actions.redoForProduct());
            });
        };
    }
});

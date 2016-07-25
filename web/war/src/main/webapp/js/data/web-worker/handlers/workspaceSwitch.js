define(['../services/workspace'], function(Workspace) {
    'use strict';

    return function(message) {
        Workspace.get(message.workspaceId)
            .catch(function(xhr) {
                if (xhr && xhr.status === 500) {
                    throw new Error(xhr);
                } else {
                    return Workspace.all().then(function(workspaces) {
                        var workspace = _.find(workspaces, function(w) {
                            return !w.sharedToUser && ('createdBy' in w);
                        });
                        if (workspace && workspace.workspaceId !== message.workspaceId) {
                            return Workspace.get(workspace.workspaceId);
                        }
                        return Workspace.create();
                    });
                }
            })
            .then(function(workspace) {
                return Promise.all([
                    Workspace.edges(workspace.workspaceId),
                    Workspace.vertices(workspace.workspaceId),
                    Promise.require('data/web-worker/util/store')
                ]).then(function(results) {
                    var edges = results.shift(),
                        vertices = results.shift().vertices,
                        store = results.shift();

                    store.setWorkspace(workspace);

                    pushSocketMessage({
                        type: 'setActiveWorkspace',
                        data: {
                            workspaceId: workspace.workspaceId,
                            userId: publicData.currentUser.id
                        }
                    });
                    dispatchMain('workspaceLoaded', {
                        workspace: workspace,
                        vertices: vertices
                    });
                    dispatchMain('edgesLoaded', {
                        edges: edges
                    });
                })
            });
    };

});

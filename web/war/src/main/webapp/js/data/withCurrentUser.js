define([], function() {
    'use strict';

    return withCurrentUser;

    function withCurrentUser() {

        this.after('initialize', function() {
            this.on('willLogout', function() {
                this.worker.postMessage({
                    type: 'atmosphereConfiguration',
                    close: true
                })
            })
            this.on('didLogout', function() {
                this.setPublicApi('currentUser', undefined);
                this.setPublicApi('socketSourceGuid', undefined);
            })
            this.on('userStatusChange', function(event, user) {
                if (user &&
                    user.status &&
                    user.status === 'OFFLINE' &&
                    user.id &&
                    visalloData.currentUser &&
                    user.id === visalloData.currentUser.id) {
                    $(document).trigger('logout', { message: i18n('visallo.session.expired') });
                }
            });
            this.on('workspaceLoaded', function(event, data) {
                var user = visalloData.currentUser;
                user.currentWorkspaceId = data.workspaceId;
                this.setPublicApi('currentUser', user);
                this.store.update('currentUser', user);
            })
        });

        this.around('dataRequestCompleted', function(dataRequestCompleted, request) {
            if (isUserMeRequest(request)) {
                var user = request.result;

                this.setPublicApi('currentUser', user, { onlyIfNull: true });
                this.store.update('currentUser', user);

                if (user.currentWorkspaceId) {
                    this.setPublicApi('currentWorkspaceId', user.currentWorkspaceId, { onlyIfNull: true });
                } else {
                    return this.findOrCreateWorkspace(user.id, dataRequestCompleted, request);
                }
            } else if (isUserPreferenceUpdate(request)) {
                visalloData.currentUser.uiPreferences = request.result.uiPreferences;
                this.setPublicApi('currentUser', visalloData.currentUser);
            }

            return dataRequestCompleted.call(this, request);
        });

        this.findOrCreateWorkspace = function(userId, dataRequestCompleted, request) {
            var self = this;

            this.dataRequestPromise
                .done(function(dataRequest) {
                    dataRequest('workspace', 'all')
                        .then(function(workspaces) {
                            if (workspaces.length) {
                                return Promise.resolve(workspaces[0]);
                            }

                            return dataRequest('workspace', 'create')
                        })
                        .done(function(workspace) {
                            self.pushSocket({
                                type: 'setActiveWorkspace',
                                data: {
                                    workspaceId: workspace.workspaceId,
                                    userId: userId
                                }
                            });
                            self.setPublicApi('currentWorkspaceId', workspace.workspaceId);
                            dataRequestCompleted.call(this, request);
                        });
                })
        };

    }

    function isUserPreferenceUpdate(request) {
        return request &&
            request.result &&
            request.result.uiPreferences;
    }

    function isUserMeRequest(request) {
        return request &&
               request.success &&
               request.originalRequest.service === 'user' &&
               request.originalRequest.method === 'me' &&
               request.result;
    }
});

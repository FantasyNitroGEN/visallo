define(['./withDataRequest'], function(withDataRequest) {
    'use strict';

    return withWorkspaceHelper;

    function withWorkspaceHelper() {

        this.loadWorkspaceAndListenOn = function(handler) {
            if (!_.isFunction(handler)) {
                throw new Error('handler must be a function. Will be called once when loaded and on changes');
            }

            var self = this;
            Promise.race([
                new Promise(function(fulfill, reject) {
                    self.on(document, 'workspaceLoaded', function workspaceLoaded(event, workspace) {
                        self.off(document, 'workspaceLoaded', workspaceLoaded);
                        self.on(document, 'workspaceLoaded', handler);
                        fulfill([event, workspace]);
                    });
                }),
                withDataRequest.dataRequest('workspace', 'current')
                .then(function(workspace) {
                    if (!workspace) {
                        return new Promise(function() { });
                    }

                    return [undefined, workspace];
                })
            ]).done(function(args) {
                handler.apply(self, args);
            })
        };
    }
});

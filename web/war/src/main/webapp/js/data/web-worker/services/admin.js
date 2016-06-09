
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {

        vertexDelete: function(vertexId, workspaceId) {
            return ajax('POST->HTML', '/admin/deleteVertex', {
                graphVertexId: vertexId,
                workspaceId: workspaceId
            });
        },

        edgeDelete: function(edgeId, workspaceId) {
            return ajax('POST->HTML', '/admin/deleteEdge', {
                edgeId: edgeId,
                workspaceId: workspaceId
            });
        },

        dictionary: function() {
            return ajax('GET', '/admin/dictionary');
        },

        dictionaryDelete: function(rowKey) {
            return ajax('POST', '/admin/dictionary/delete', {
                entryRowKey: rowKey
            });
        },

        dictionaryAdd: function(concept, tokens, resolvedName) {
            var data = {
                concept: concept,
                tokens: tokens
            };

            if (resolvedName) {
                data.resolvedName = resolvedName;
            }

            return ajax('POST', '/admin/dictionary', data);
        },

        plugins: function() {
            return ajax('GET', '/admin/plugins');
        },

        systemNotificationCreate: function(options) {
            if ('endDate' in options && !options.endDate) {
                delete options.endDate;
            }
            if ('externalUrl' in options && !options.externalUrl) {
                delete options.externalUrl;
            }
            return ajax('POST', '/notification/system', options);
        },

        systemNotificationDelete: function(id) {
            return ajax('DELETE', '/notification/system', {
                notificationId: id
            });
        },

        userDelete: function(userName) {
            return ajax('POST', '/user/delete', {
                'user-name': userName
            });
        },

        workspaceShare: function(workspaceId, userName) {
            return ajax('POST', '/workspace/shareWithMe', {
                'user-name': userName,
                workspaceId: workspaceId
            });
        },

        workspaceImport: function(workspaceFile) {
            var formData = new FormData();
            formData.append('workspace', workspaceFile);
            return ajax('POST->HTML', '/admin/workspace/import', formData);
        },

        ontologyUpload: function(iri, file) {
            var formData = new FormData();
            formData.append('file', file);
            formData.append('documentIRI', iri);
            return ajax('POST->HTML', '/admin/upload-ontology', formData);
        }
    };

    return api;
});

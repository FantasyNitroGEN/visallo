define([
    'require',
    'configuration/plugins/registry',
    '../store'
], function(require, registry, store) {
    'use strict';

    var NOOP = function() {},
        socketHandlers = {
            workspaceChange: function(data, json) {
                require(['../store/workspace/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.update({ workspace: data }))
                });
            },
            workspaceDelete: function(data) {
                require(['../store/workspace/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.deleteWorkspace({ workspaceId: data.workspaceId }));
                });
            },
            ontologyChange: function(data) {
                require(['../store/ontology/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.get({ workspaceId: data.workspaceId }));
                });
            },
            ontologyConceptsChange: function(data) {
                require(['../store/ontology/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.conceptsChange(data));
                });
            },
            workProductPreviewChange: function(data) {
                const { id, workspaceId, md5 } = data;
                require(['../store/product/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.previewChanged({ productId: id, workspaceId, md5 }));
                })
            },
            workProductChange: function(data) {
                const guid = publicData.socketSourceGuid;
                if ('skipSourceGuid' in data) {
                    if (guid === data.skipSourceGuid) {
                        return;
                    }
                }

                require(['../store/product/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.changedOnServer(data.id));
                })
            },
            workProductDelete: function(data) {
                require(['../store/product/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.remove(data.id))
                })
            },
            sessionExpiration: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'sessionExpiration'
                });
            },
            userStatusChange: (function() {
                var previousById = {};
                return function(data) {
                    var previous = data && previousById[data.id];
                    if (!previous || !_.isEqual(data, previous)) {
                        previousById[data.id] = data;
                        dispatchMain('rebroadcastEvent', {
                            eventName: 'userStatusChange',
                            data: data
                        });
                    }
                }
            })(),
            userWorkspaceChange: NOOP,
            publish: function(data) {
                // Property undo already publishes propertyChange
                if (data.objectType !== 'property' || data.publishType !== 'undo') {
                    socketHandlers.propertyChange(data);
                }
            },
            propertyChange: function(data) {
                require(['../store/element/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.propertyChange(data));
                });
            },
            verticesDeleted: function(data) {
                require(['../store/element/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.deleteElements({ vertexIds: data.vertexIds }));
                });
            },
            edgeDeletion: function(data) {
                require(['../store/element/actions-impl'], function(actions) {
                    store.getStore().dispatch(actions.deleteElements({ edgeIds: [data.edgeId] }));
                });
            },
            textUpdated: function(data) {
                if (data.graphVertexId &&
                    (!data.workspaceId ||
                     data.workspaceId === publicData.currentWorkspaceId)) {

                    dispatchMain('rebroadcastEvent', {
                        eventName: 'textUpdated',
                        data: {
                            vertexId: data.graphVertexId
                        }
                    })
                }
            },
            longRunningProcessDeleted: function(processId) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'longRunningProcessDeleted',
                    data: {
                        processId: processId
                    }
                });
            },
            longRunningProcessChange: function(process) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'longRunningProcessChanged',
                    data: {
                        process: process
                    }
                });
            },
            entityImageUpdated: function(data) {
                if (data && data.graphVertexId) {
                    socketHandlers.propertyChange(data);
                }
            },
            notification: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'notificationActive',
                    data: data
                });
            },
            systemNotificationUpdated: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'notificationUpdated',
                    data: data
                });
            },
            systemNotificationEnded: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'notificationDeleted',
                    data: data
                });
            }
        },
        callHandlersForName = function(name, data) {
            var extensions = _.where(
                registry.extensionsForPoint('org.visallo.websocket.message'),
                { name: name }
            );
            if (extensions.length) {
                extensions.forEach(function(e) {
                    e.handler(data);
                });

                return true;
            }
        };

    return function(data) {
        var body = data.responseBody,
            json = JSON.parse(body);

        if (isBatchMessage(json)) {
            var filtered = _.reject(json.data, messageFromUs);
            if (filtered.length) {
                console.groupCollapsed('Socket Batch (' + filtered.length + ')');
                filtered.forEach(process);
                console.groupEnd();
            }
        } else if (!messageFromUs(json)) {
            process(json);
        }
    }

    function process(json) {
        console.debug('%cSocket: %s %O', 'color:#999;font-style:italics', json.type, json.data || json)
        if (json.type in socketHandlers) {
            socketHandlers[json.type]('data' in json ? json.data : json, json);
            callHandlersForName(json.type, json.data);
        } else if (!callHandlersForName(json.type, json.data)) {
            console.warn('Unhandled socket message type:' + json.type, 'message:', json);
        }
    }

    function messageFromUs(json) {
        return json.sourceGuid && json.sourceGuid === publicData.socketSourceGuid;
    }

    function isBatchMessage(json) {
        return json.type === 'batch' && _.isArray(json.data);
    }
});

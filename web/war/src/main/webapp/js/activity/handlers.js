
define([], function() {
    'use strict';

    return [
        {
            type: 'saveWorkspace',
            kind: 'eventWatcher',
            eventNames: ['workspaceSaving', 'workspaceSaved'],
            titleRenderer: function(el, datum) {
                el.textContent = datum.eventData.title;
            },
            autoDismiss: true
        },
        {
            type: 'org-visallo-ingest-cloud',
            kind: 'longRunningProcesses',
            titleRenderer: function(el, task) {
                const type = i18n(`${task.destination}.title`);
                const config = _.isString(task.configuration) ? JSON.parse(task.configuration) : task.configuration;
                const number = config.itemsCount;
                const plural = number === 1 ? '' : 's';
                const titleKey = 'activity.tasks.type.org-visallo-ingest-cloud.title';

                el.textContent = i18n(titleKey, number, plural, type);
            },
            autoDismiss: false,
            allowCancel: true,
            finishedComponentPath: 'activity/builtin/CloudImport'
        },
        {
            type: 'findPath',
            kind: 'longRunningProcess',
            titleRenderer: function(el, process) {
                require([
                    'util/withDataRequest',
                    'util/vertex/formatters'
                ], function(withDataRequest, F) {
                    withDataRequest.dataRequest('vertex', 'store', {
                        workspaceId: process.workspaceId,
                        vertexIds: [
                            process.outVertexId,
                            process.inVertexId
                        ]
                    }).done(function(vertices) {
                        if (vertices.length === 2) {
                            var source = F.vertex.title(vertices[0]),
                                dest = F.vertex.title(vertices[1]);

                            el.textContent = source + ' → ' + dest;
                            $('<div>')
                                .css({ fontSize: '90%' })
                                .text(i18n('popovers.find_path.hops.option', process.hops))
                                .appendTo(el);
                        }
                    });
                });
            },
            onRemove: function() {
                this.trigger('defocusPaths');
            },
            finishedComponentPath: 'activity/builtin/findPath'
        }
    ];
})

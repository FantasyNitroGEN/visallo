
require(['configuration/plugins/registry'], function(registry) {
    registry.registerExtension('org.visallo.websocket.message', {
        name: 'structuredImportDryrun',
        handler: function(data) {
            dispatchMain('rebroadcastEvent', {
                eventName: 'structuredImportDryrunProgress',
                data: data
            });
        }
    });
});

define('data/web-worker/services/org-visallo-structuredingest', ['data/web-worker/util/ajax'], function(ajax) {
    'use strict';

    return {
        mimeTypes: _.memoize(function() {
            return ajax('GET', '/structured-ingest/mimeTypes')
        }),
        analyze: function(vertexId) {
            return ajax('GET', '/structured-ingest/analyze', {
                graphVertexId: vertexId
            })
        },
        ingest: function(mapping, vertexId, options, isPreview, shouldPublish) {
            return ajax('POST', '/structured-ingest/ingest', _.tap({
                mapping: JSON.stringify(mapping),
                graphVertexId: vertexId,
                preview: Boolean(isPreview),
                publish: Boolean(shouldPublish)
            }, function(params) {
                if (options) {
                    params.parseOptions = options;
                }
            }));
        }
    }
})

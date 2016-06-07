define([
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/requirejs/promise!util/service/ontologyPromise'
    ], function(F, dataRequest, ontology) {
    'use strict';

    var conceptDisplay = _.compose(_.property('displayName'), F.vertex.concept),
        vertexStore = function(vertexId) {
            return dataRequest.dataRequest('vertex', 'store', { vertexIds: vertexId })
        },
        vertexDisplayId = function(vertexId) {
            return vertexStore(vertexId).then(F.vertex.title)
        },
        edgeLabelDisplay = function(edge) {
            return ontology.relationships.byTitle[edge.label].displayName;
        },
        outVertexDisplay = _.compose(vertexDisplayId, _.property('outVertexId')),
        inVertexDisplay = _.compose(vertexDisplayId, _.property('inVertexId')),
        outVertexConceptDisplay = function(edge) {
            return vertexStore(edge.outVertexId).then(conceptDisplay)
        },
        inVertexConceptDisplay = function(edge) {
            return vertexStore(edge.inVertexId).then(conceptDisplay)
        };

    return [
        {
            applyTo: { type: 'edge' },
            identifier: 'org.visallo.layout.root',
            layout: { type: 'flex', options: { direction: 'column' }},
            componentPath: 'detail/item/edge',
            children: [
                { ref: 'org.visallo.layout.header', style: { flex: '0 0 auto' } },
                { ref: 'org.visallo.layout.body', style: { flex: '1 1 auto', overflow: 'auto' } }
            ]
        },
        {
            applyTo: { type: 'edge' },
            identifier: 'org.visallo.layout.header.text',
            className: 'edge-heading',
            children: [
                { ref: 'org.visallo.layout.text', style: 'title', className: 'vertex-out', model: outVertexDisplay },
                { ref: 'org.visallo.layout.text', style: 'subtitle', model: outVertexConceptDisplay },
                { ref: 'org.visallo.layout.text', className: 'edge-label', model: edgeLabelDisplay },
                { ref: 'org.visallo.layout.text', style: 'title', className: 'vertex-in', model: inVertexDisplay },
                { ref: 'org.visallo.layout.text', style: 'subtitle', model: inVertexConceptDisplay }
            ]
        }
    ];
});

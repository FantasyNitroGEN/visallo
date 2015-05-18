
define([
    './3djs/graph/graph',
    './3djs/graph/renderer'
], function(Graph, GraphRenderer) {
    'use strict';

    return {

        VERSION: '1.0',

        Graph: Graph,
        GraphRenderer: GraphRenderer
    };
});

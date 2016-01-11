define([
    'flight/lib/component',
    './withItem'
], function(
    defineComponent,
    withItem) {
    'use strict';

    return defineComponent(Vertex, withItem);

    function Vertex() {

        this.attributes({
            model: null
        })

        this.after('initialize', function() {
            var self = this;

            this.$node.find('.vertex-header .title')
                .addClass('vertex-draggable')
                .attr('data-vertex-id', this.attr.model.id)

            this.on(document, 'verticesUpdated', function(event, data) {
                data.vertices.forEach(function(v) {
                    if (self.attr.model.id === v.id) {
                        self.trigger('updateModel', { model: v })
                    }
                });
            })

        })
    }

});

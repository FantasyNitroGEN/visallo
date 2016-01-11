define([
    'flight/lib/component',
    './withItem'
], function(
    defineComponent,
    withItem) {
    'use strict';

    return defineComponent(Edge, withItem);

    function Edge() {

        this.attributes({
            model: null
        })

        this.after('initialize', function() {
            var self = this;

            this.on(document, 'edgesUpdated', this.onEdgesUpdated);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node.find('.vertex-in,.vertex-out').each(function() {
                var vertexId = $(this).hasClass('vertex-in') ? self.attr.model.inVertexId : self.attr.model.outVertexId;
                $(this).attr('data-vertex-id', vertexId).addClass('vertex-draggable');
            });
        })

        this.onEdgesUpdated = function(event, data) {
            var self = this;
            data.edges.forEach(function(e) {
                if (self.attr.model.id === e.id) {
                    self.trigger('updateModel', { model: e })
                }
            });
        };

        this.onVerticesUpdated = function(event, data) {
            var source = _.findWhere(data.vertices, { id: this.attr.model.source.id }),
                target = _.findWhere(data.vertices, { id: this.attr.model.target.id });

            if (source || target) {
                this.trigger('updateModel', {
                    model: _.extend({}, this.attr.model, {
                        source: source || this.attr.model.source,
                        target: target || this.attr.model.target
                    })
                })
            }
        };
    }

});

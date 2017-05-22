define([
    'flight/lib/component',
    './vertex-item.hbs',
    'util/element/withPreview',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    withPreview,
    F) {
    'use strict';

    return defineComponent(DetailRelationshipItem, withPreview);

    function DetailRelationshipItem() {

        this.after('initialize', function() {
            const { vertex, relationship } = this.attr.item;
            const vertexId = vertex && vertex.id;
            const title = F.vertex.title(vertex);
            const timeSubtitle = F.vertex.time(vertex);
            const subtitle = F.vertex.subtitle(vertex);

            this.vertex = vertex;

            this.$node
                .addClass('default')
                .addClass(timeSubtitle ? 'has-timeSubtitle' : '')
                .addClass(subtitle ? 'has-subtitle' : '')
                .addClass(relationship.inVertexId === vertexId ? 'relation-to' : 'relation-from')
                .html(template({ title, timeSubtitle, subtitle }));

            if (vertexId) {
                this.$node.data('vertexId', vertexId);
            }
        });

        this.before('teardown', function() {
            this.$node.removeData('vertexId');
            this.$node.removeClass('has-timeSubtitle has-subtitle relation-to relation-from');
            this.$node.empty();
        });
    }
});

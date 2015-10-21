define([
    'flight/lib/component',
    'tpl!./vertex-item',
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
            this.vertex = this.attr.item.vertex;

            var timeSubtitle = F.vertex.time(this.vertex),
                subtitle = F.vertex.subtitle(this.vertex);

            this.$node
                .addClass(timeSubtitle ? 'has-timeSubtitle' : '')
                .addClass(subtitle ? 'has-subtitle' : '')
                .addClass(this.attr.item.relationship.inVertexId === this.vertex.id ? 'relation-to' : 'relation-from')
                .html(template({
                    title: F.vertex.title(this.vertex),
                    timeSubtitle: timeSubtitle,
                    subtitle: subtitle
                }));

            this.$node.data('vertexId', this.vertex.id);
        });

        this.before('teardown', function() {
            this.$node.removeData('vertexId');
            this.$node.removeClass('has-timeSubtitle has-subtitle relation-to relation-from');
            this.$node.empty();
        });
    }
});

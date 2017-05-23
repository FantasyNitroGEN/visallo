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

    return defineComponent(VertexItem, withPreview);

    function VertexItem() {

        this.after('initialize', function() {
            this.vertex = this.attr.item;

            var timeSubtitle = F.vertex.time(this.vertex),
                subtitle = F.vertex.subtitle(this.vertex);

            this.$node
                .addClass('default')
                .addClass(timeSubtitle ? 'has-timeSubtitle' : '')
                .addClass(subtitle ? 'has-subtitle' : '')
                .html(template({
                    title: F.vertex.title(this.vertex),
                    timeSubtitle: timeSubtitle,
                    subtitle: subtitle
                }));

            this.$node.data('vertexId', this.vertex.id);
        });

        this.before('teardown', function() {
            this.$node.removeData('vertexId');
            this.$node.removeClass('has-timeSubtitle has-subtitle');
            this.$node.empty();
        });
    }
});

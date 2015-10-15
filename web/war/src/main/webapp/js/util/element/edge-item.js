define([
    'flight/lib/component',
    'tpl!./edge-item',
    'util/requirejs/promise!util/service/ontologyPromise',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    ontology,
    F) {
    'use strict';

    return defineComponent(EdgeItem);

    function EdgeItem() {

        this.after('initialize', function() {
            var edge = this.attr.item,
                ontologyRelation = ontology.relationships.byTitle[edge.label],
                title = ontologyRelation.titleFormula ? F.edge.title(edge) : ontologyRelation.displayName,
                subtitle = ontologyRelation.subtitleFormula ? F.edge.subtitle(edge) : null,
                timeSubtitle = ontologyRelation.timeFormula ? F.edge.time(edge) : null;

            this.$node
                .addClass('edge-item')
                .addClass(timeSubtitle ? 'has-timeSubtitle' : '')
                .addClass(subtitle ? 'has-subtitle' : '')
                .html(template({
                    title: title,
                    timeSubtitle: timeSubtitle,
                    subtitle: subtitle
                }));

            this.$node.data('edgeId', edge.id);
        });

        this.before('teardown', function() {
            this.$node.removeData('edgeId');
            this.$node.removeClass('edge-item has-timeSubtitle has-subtitle');
            this.$node.empty();
        });
    }
});

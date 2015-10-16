define([
    'flight/lib/component',
    'tpl!./edge-item',
    'util/requirejs/promise!util/service/ontologyPromise',
    'util/vertex/justification/viewer',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    ontology,
    JustificationViewer,
    withDataRequest,
    F) {
    'use strict';

    return defineComponent(EdgeItem, withDataRequest);

    function EdgeItem() {

        this.after('initialize', function() {
            var self = this,
                edge = this.attr.item,
                ontologyRelation = ontology.relationships.byTitle[edge.label],
                title = ontologyRelation.titleFormula ? F.edge.title(edge) : ontologyRelation.displayName,
                subtitle = ontologyRelation.subtitleFormula ? F.edge.subtitle(edge) : null,
                timeSubtitle = ontologyRelation.timeFormula ? F.edge.time(edge) : null;

            this.$node.data('edgeId', edge.id);

            this.dataRequest('config', 'properties')
                .done(function(properties) {
                    self.$node
                        .addClass('edge-item')
                        .addClass(timeSubtitle ? 'has-timeSubtitle' : '')
                        .addClass(subtitle ? 'has-subtitle' : '')
                        .html(template({
                            title: title,
                            timeSubtitle: timeSubtitle,
                            subtitle: subtitle
                        }));

                    if (properties['field.justification.validation'] !== 'NONE' &&
                                        self.attr.usageContext === 'detail/multiple') {
                        self.renderJustification();
                    }
                });
        });

        this.renderJustification = function() {
            var edge = this.attr.item,
                titleSpan = this.$node.children('span.title'),
                justification = _.findWhere(edge.properties, { name: 'http://visallo.org#justification' }),
                sourceInfo = _.findWhere(edge.properties, { name: '_sourceMetadata' });

            if (justification || sourceInfo) {
                titleSpan.empty();
                JustificationViewer.attachTo(titleSpan, {
                    justificationMetadata: justification && justification.value,
                    sourceMetadata: sourceInfo && sourceInfo.value
                });
            }
        };

        this.before('teardown', function() {
            this.$node.removeData('edgeId');
            this.$node.removeClass('edge-item has-timeSubtitle has-subtitle');
            this.$node.empty();
        });
    }
});

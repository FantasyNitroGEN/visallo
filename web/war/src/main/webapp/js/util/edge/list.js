define([
    'flight/lib/component',
    'd3',
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/vertex/justification/viewer'
], function(
    defineComponent,
    d3,
    F,
    withDataRequest,
    JustificationViewer) {
    'use strict';

    return defineComponent(EdgeList, withDataRequest);

    function EdgeList() {

        this.defaultAttrs({
            edgeItemSelector: '.edge'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                edgeItemSelector: this.onSelectEdge
            });
            this.on(document, 'edgesDeleted', this.onEdgesDeleted);
            this.on(document, 'objectsSelected', this.onObjectsSelected);

            this.$list = $('<ul>')
                .addClass('nav nav-list edge-list')
                .appendTo(this.$node.empty())
                .get(0);

            this.dataRequest('ontology', 'relationships')
                .done(function(relationships) {
                    self.ontologyRelationships = relationships;
                    self.render();
                });
        });

        this.onObjectsSelected = function(event, data) {
            if (data && data.edges.length) {
                var toSelect = this.$node.find(data.edges.map(function(e) {
                        return '.' + F.className.to(e.id);
                    }).join(','))
                this.$node.find('.active').not(toSelect).removeClass('active');
                toSelect.addClass('active');
            } else {
                this.$node.find('.active').removeClass('active');
            }
        };

        this.onEdgesDeleted = function(event, data) {
            if (data.edgeId) {
                this.attr.edges = _.reject(this.attr.edges, function(e) {
                    return e.id === data.edgeId;
                });
                this.render();
            }
        };

        this.onSelectEdge = function(event) {
            this.trigger('selectObjects', {
                edgeIds: [$(event.target).closest('li')
                    .addClass('active')
                    .siblings('.active')
                        .removeClass('active')
                        .end()
                    .data('edgeId')
                ]
            });
        };

        this.render = function() {
            var self = this;

            d3.select(this.$list)
                .selectAll('li.edge')
                .data(this.attr.edges)
                .call(function() {
                    this.enter()
                        .append('li')
                        .attr('class', function(d) {
                            return 'edge vertex-item ' + F.className.to(d.id);
                        })
                        .append('a')
                        .call(function() {
                            this.append('span');
                            this.append('div').attr('class', 'subtitle')
                            this.append('div').attr('class', 'subtitleTime')
                        });

                    this.attr('data-edge-id', function(d) {
                        return d.id;
                    });

                    this.select('span').each(function() {
                        var $this = $(this),
                            d = d3.select(this).datum(),
                            justification = _.findWhere(d.properties, { name: 'http://visallo.org#justification' }),
                            sourceInfo = _.findWhere(d.properties, { name: '_sourceMetadata' }),
                            ontologyRelation = self.ontologyRelationships.byTitle[d.label];

                        $this.teardownAllComponents();

                        if (self.attr.showTypeLabel && ontologyRelation && ontologyRelation.titleFormula) {
                            $this.text(F.edge.title(d));
                        } else if (self.attr.showTypeLabel && ontologyRelation) {
                            $this.text(ontologyRelation.displayName);
                        } else if (justification || sourceInfo) {
                            JustificationViewer.attachTo($this, {
                                justificationMetadata: justification && justification.value,
                                sourceMetadata: sourceInfo && sourceInfo.value
                            });
                        } else {
                            $this.text(i18n('detail.multiple.edge.justification.novalue'));
                        }
                    });

                    this.select('.subtitle').text(F.edge.subtitle);
                    this.select('.subtitleTime').text(F.edge.time);

                    this.exit().remove();
                });
        }
    }
});

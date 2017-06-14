
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/withFormFieldErrors',
    'util/withTeardown',
    'util/withDataRequest',
    'util/visibility/edit',
    'util/ontology/relationshipSelect',
    'detail/dropdowns/propertyForm/justification'
], function(
    defineComponent,
    withVertexPopover,
    withFormFieldErrors,
    withTeardown,
    withDataRequest,
    Visibility,
    RelationshipSelect,
    Justification) {
    'use strict';

    return defineComponent(
        CreateConnectionPopover,
        withVertexPopover,
        withFormFieldErrors,
        withTeardown,
        withDataRequest
    );

    function CreateConnectionPopover() {

        this.defaultAttrs({
            connectButtonSelector: '.connect-dialog button.connect',
            invertButtonSelector: '.connect-dialog button.invert-connection'
        });

        this.before('teardown', function() {
            this.trigger('finishedVertexConnection');
        })

        this.before('initialize', function(node, config) {
            config.template = 'createConnectionPopover';
        });

        this.after('initialize', function() {
            this.on('click', {
                connectButtonSelector: this.onCreateConnection,
                invertButtonSelector: this.onInvert
            });
        });

        this.popoverInitialize = function() {
            this.visibilitySource = null;
            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);
            this.on('relationshipSelected', this.onRelationshipSelected);
            this.on('rendered', this.positionDialog);

            RelationshipSelect.attachTo(this.popover.find('.relationships'), {
                focus: true,
                sourceConcept: this.attr.otherCyNode.data('conceptType'),
                targetConcept: this.attr.cyNode.data('conceptType')
            });
            Visibility.attachTo(this.popover.find('.visibility'), {
                value: ''
            });
            Justification.attachTo(this.popover.find('.justification'));

            this.updateRelationshipLabels();
        };

        this.updateRelationshipLabels = function() {
            var self = this,
                button = this.select('connectButtonSelector');

            button.text(i18n('org.visallo.web.product.graph.connection.button.connect')).attr('disabled', true);

            this.popover.find('.relationships').trigger('limitParentConceptId', {
                    sourceConceptId: this.attr.otherCyNode.data('conceptType'),
                    targetConceptId: this.attr.cyNode.data('conceptType')
                })
                .trigger('selectRelationshipId');
            this.relationship = null;
            this.positionDialog();
            this.checkValid();

            // TODO:
            //select.html('<option>' + i18n('relationship.form.no_valid_relationships') + '</option>');
        };

        this.getTemplate = function() {
            return new Promise(f => require(['./createConnectionPopoverTpl'], f));
        };

        this.onRelationshipSelected = function(event, data) {
            this.relationship = data.relationship ? data.relationship.title : null;
            this.checkValid();
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onJustificationChange = function(event, data) {
            this.justification = data;
            this.checkValid();
        };

        this.checkValid = function() {
            var button = this.select('connectButtonSelector');

            if (this.relationship &&
                this.visibilitySource && this.visibilitySource.valid &&
                this.justification && this.justification.valid) {
                button.removeAttr('disabled');
            } else {
                button.attr('disabled', true);
            }
        }

        this.onInvert = function(e) {
            var self = this;

            if (this.ignoreViewportChanges) {
                return;
            }

            if (!this.currentNodeIndex) {
                this.currentNodeIndex = 1;
                this.nodes = [this.attr.cyNode, this.attr.otherCyNode];
            }

            var node = this.nodes[this.currentNodeIndex % 2],
                other = this.nodes[(this.currentNodeIndex + 1) % 2],
                otherTitle = other.data('truncatedTitle'),
                currentNodePosition = node.renderedPosition(),
                otherNodePosition = other.renderedPosition();

            this.currentNodeIndex++;

            this.ignoreViewportChanges = true;
            this.popover.find('.title').text(otherTitle);
            this.attr.cyNode = node;
            this.attr.otherCyNode = other;
            this.updateRelationshipLabels();

            this.attr.cy.animate({
                panBy: {
                    x: otherNodePosition.x - currentNodePosition.x,
                    y: otherNodePosition.y - currentNodePosition.y
                }
            }, {
                duration: 400,
                easing: 'spring(250, 20)',
                complete: function() {
                    self.ignoreViewportChanges = false;
                    self.onViewportChanges();
                }
            });

            this.trigger(document, 'invertVertexConnection');
        };

        this.onCreateConnection = function(e) {
            var self = this,
                $target = $(e.target)
                    .text('Connecting...')
                    .attr('disabled', true),
                parameters = {
                    outVertexId: this.attr.outVertexId,
                    inVertexId: this.attr.inVertexId,
                    predicateLabel: this.relationship,
                    visibilitySource: this.visibilitySource.value
                };
            if (this.attr.otherCyNode.id() !== this.attr.edge.data('source')) {
                // Invert
                parameters.outVertexId = this.attr.inVertexId;
                parameters.inVertexId = this.attr.outVertexId;
            }

            this.attr.teardownOnTap = false;

            if (this.justification.sourceInfo) {
                parameters.sourceInfo = JSON.stringify(this.justification.sourceInfo);
            } else if (this.justification.justificationText) {
                parameters.justificationText = this.justification.justificationText;
            }

            this.dataRequest('edge', 'create', parameters)
                .then(function(data) {
                    self.teardown();
                })
                .catch(function(error) {
                    $target.text(i18n('org.visallo.web.product.graph.connection.button.connect'))
                        .removeAttr('disabled');
                    self.markFieldErrors(error);
                    self.positionDialog();
                })
                .finally(function() {
                    self.attr.teardownOnTap = true;
                })
        };
    }
});

define([
    'flight/lib/component',
    'util/withDropdown',
    './statementForm.hbs',
    'util/withDataRequest',
    'util/ontology/relationshipSelect'
], function(
    defineComponent,
    withDropdown,
    statementFormTemplate,
    withDataRequest,
    RelationshipSelect) {
    'use strict';

    return defineComponent(StatementForm, withDropdown, withDataRequest);

    function StatementForm() {

        this.defaultAttrs({
            formSelector: '.form',
            sourceTermSelector: '.src-term',
            destTermSelector: '.dest-term',
            termLabelsSelector: '.src-term span, .dest-term span',
            createStatementButtonSelector: '.create-statement',
            statementLabelSelector: '.statement-label',
            invertAnchorSelector: 'a.invert',
            relationshipSelector: '.selector',
            buttonDivSelector: '.buttons',
            manualOpen: true
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(statementFormTemplate({
                source: this.attr.sourceTerm.text(),
                dest: this.attr.destTerm.text()
            }));

            this.on('relationshipSelected', this.onRelationshipSelected);
            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);

            this.applyTermClasses(this.attr.sourceTerm, this.select('sourceTermSelector'));
            this.applyTermClasses(this.attr.destTerm, this.select('destTermSelector'));

            this.attr.sourceTerm.addClass('focused');
            this.attr.destTerm.addClass('focused');

            this.select('createStatementButtonSelector').attr('disabled', true);
            this.getRelationshipLabels();

            this.on('click', {
                createStatementButtonSelector: this.onCreateStatement,
                invertAnchorSelector: this.onInvert
            });
            this.on('opened', this.onOpened);
        });

        this.after('teardown', function() {
            this.attr.sourceTerm.removeClass('focused');
            this.attr.destTerm.removeClass('focused');
        });

        this.onRelationshipSelected = function(event, data) {
            this.relationship = data && data.relationship ? data.relationship.title : null;
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
            var button = this.select('createStatementButtonSelector');

            if (this.visibilitySource && this.visibilitySource.valid &&
                this.justification && this.justification.valid &&
                this.relationship) {
                button.removeAttr('disabled');
            } else {
                button.attr('disabled', true);
            }
        };

        this.applyTermClasses = function(el, applyToElement) {
            var classes = el.attr('class').split(/\s+/),
                ignored = [/^ui-*/, /^term$/, /^(entity|vertex|property)$/, /^label-info$/, /^detected-object$/];

            classes.forEach(function(cls) {
                var ignore = _.any(ignored, function(regex) {
                    return regex.test(cls);
                });
                if (!ignore) {
                    applyToElement.addClass(cls);
                }
            });

            this.dataRequest('ontology', 'concepts')
                .done(function(concepts) {
                    var concept = concepts.byId[el.data('info')['http://visallo.org#conceptType']];
                    if (concept) {
                        applyToElement.addClass('concepticon-' + concept.className);
                    }
                });
        };

        this.onOpened = function() {
            this.select('relationshipSelector').find('input').focus();
        };

        this.onInvert = function(e) {
            e.preventDefault();

            var sourceTerm = this.attr.sourceTerm;
            this.attr.sourceTerm = this.attr.destTerm;
            this.attr.destTerm = sourceTerm;
            this.relationship = null;

            this.select('formSelector').toggleClass('invert');
            this.getRelationshipLabels();
            this.checkValid();
        };

        this.onCreateStatement = function(event) {
            var self = this,
                parameters = {
                    outVertexId: this.attr.sourceTerm.data('info').resolvedToVertexId ||
                        this.attr.sourceTerm.data('vertex-id'),
                    inVertexId: this.attr.destTerm.data('info').resolvedToVertexId ||
                        this.attr.destTerm.data('vertex-id'),
                    predicateLabel: this.relationship,
                    visibilitySource: this.visibilitySource.value
                };

            if (this.select('formSelector').hasClass('invert')) {
                var swap = parameters.outVertexId;
                parameters.outVertexId = parameters.inVertexId;
                parameters.inVertexId = swap;
            }

            if (this.justification.sourceInfo) {
                parameters.sourceInfo = JSON.stringify(this.justification.sourceInfo);
            } else if (this.justification.justificationText) {
                parameters.justificationText = this.justification.justificationText;
            }

            _.defer(this.buttonLoading.bind(this));

            this.dataRequest('edge', 'create', parameters)
                .then(function(data) {
                    _.defer(self.teardown.bind(self));
                    self.trigger(document, 'loadEdges');
                })
                .catch(function(error) {
                    self.clearLoading();
                    // TODO: fix how errors are returned
                    self.markFieldErrors(error || 'Server Error');
                })
        };

        this.getRelationshipLabels = function() {
            var self = this,
                sourceConcept = this.attr.sourceTerm.data('info')['http://visallo.org#conceptType'],
                targetConcept = this.attr.destTerm.data('info')['http://visallo.org#conceptType'];

            this.visibilitySource = { source: '', valid: true };

            require([
                'util/visibility/edit',
                'detail/dropdowns/propertyForm/justification'
            ], function(Visibility, Justification) {

                Visibility.attachTo(self.$node.find('.visibility'), {
                    value: ''
                });

                Justification.attachTo(self.$node.find('.justification'));

                const relationshipNode = self.select('relationshipSelector');
                relationshipNode.trigger('limitParentConceptId', { sourceConcept, targetConcept });
                relationshipNode.one('rendered', () => {
                    requestIdleCallback(() => self.manualOpen())
                })
                RelationshipSelect.attachTo(relationshipNode, { sourceConcept, targetConcept });
            });
        };
    }

});

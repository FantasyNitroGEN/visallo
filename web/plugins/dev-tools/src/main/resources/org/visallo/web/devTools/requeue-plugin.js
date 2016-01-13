define([
    'flight/lib/component',
    'configuration/admin/utils/withFormHelpers',
    'hbs!org/visallo/web/devTools/templates/requeue',
    'util/formatters',
    'util/ontology/conceptSelect',
    'util/ontology/propertySelect',
    'util/ontology/relationshipSelect',
    'util/withDataRequest'
], function(
    defineComponent,
    withFormHelpers,
    template,
    F,
    ConceptSelect,
    FieldSelection,
    RelationshipSelector,
    withDataRequest) {
    'use strict';

    return defineComponent(Requeue, withDataRequest, withFormHelpers);

    function Requeue() {

        this.defaultAttrs({
            vertexPrioritySelector: 'select.vertex-priority',
            vertexConceptTypeSelector: 'div.vertex-concept-type',
            vertexPropertyNameSelector: 'div.vertex-property-name',
            vertexButtonSelector: 'button.vertex',
            edgePrioritySelector: 'select.edge-priority',
            edgeLabelSelector: 'div.edge-label',
            edgeButtonSelector: 'button.edge-label'
        });

        this.after('initialize', function() {
            var self = this;
            this.on('click', {
                vertexButtonSelector: this.onVertexRequeue,
                edgeButtonSelector: this.onEdgeRequeue
            });

            this.on('conceptSelected', this.onConceptSelected);
            this.on('propertyselected', this.onPropertySelected);
            this.on('relationshipSelected', this.onRelationshipSelected);

            this.$node.html(template({}));

            ConceptSelect.attachTo(this.select('vertexConceptTypeSelector'), {
                showAdminConcepts: true
            });

            Promise.all([
                this.dataRequest('ontology', 'properties')
            ]).done(function(results) {
                var properties = results.shift();
                FieldSelection.attachTo(self.select('vertexPropertyNameSelector'), {
                    properties: properties.list,
                    showAdminProperties: true,
                    placeholder: 'Choose a Property...'
                });
            });

            Promise.all([
                this.dataRequest('ontology', 'relationships')
            ]).done(function(results) {
                var relationships = results.shift();

                RelationshipSelector.attachTo(self.select('edgeLabelSelector'), {
                    relationships: relationships.list,
                    showAdminProperties: true
                });
            })
        });

        this.onVertexRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.dataRequest(
                      'admin',
                      'queueVertices',
                      this.select('vertexPrioritySelector').val(),
                      this.vertexConceptIri,
                      this.vertexPropertyIri
                    )
                )
            );
        };

        this.onEdgeRequeue = function(event) {
            this.handleSubmitButton(event.target,
              this.showResult(
                this.dataRequest(
                  'admin',
                  'queueEdges',
                  this.select('edgePrioritySelector').val(),
                  this.edgeLabelIri
                )
              )
            );
        };

        this.showResult = function(promise) {
            var self = this;

            return promise
                .then(this.showSuccess.bind(this))
                .catch(this.showError.bind(this))
                .finally(function() {
                    _.delay(function() {
                        self.$node.find('.alert').remove();
                    }, 3000)
                });
        };

        this.onConceptSelected = function(event, data) {
            var concept = data && data.concept;
            if (concept) {
                this.vertexConceptIri = concept.id;
            } else {
                this.vertexConceptIri = null;
            }
        };

        this.onPropertySelected = function(event, data) {
            var property = data && data.property;
            if (property) {
                this.vertexPropertyIri = property.title;
            } else {
                this.vertexPropertyIri = null;
            }
        };

        this.onRelationshipSelected = function(event, data) {
            var relationship = data && data.relationship;
            if (relationship) {
                this.edgeLabelIri = relationship.title;
            } else {
                this.edgeLabelIri = null;
            }
        };
    }
});

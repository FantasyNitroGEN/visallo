define([
    'flight/lib/component',
    'configuration/admin/utils/withFormHelpers',
    'hbs!org/visallo/web/devTools/templates/requeue',
    'util/withDataRequest'
], function(
    defineComponent,
    withFormHelpers,
    template,
    withDataRequest) {
    'use strict';

    return defineComponent(Requeue, withDataRequest, withFormHelpers);

    function Requeue() {

        this.defaultAttrs({
            vertexPrioritySelector: 'select.vertex-priority',
            vertexConceptTypeSelector: 'input.vertex-concept-type',
            vertexPropertyNameSelector: 'input.vertex-property-name',
            vertexButtonSelector: 'button.vertex',
            edgePrioritySelector: 'select.edge-priority',
            edgeLabelSelector: 'input.edge-label',
            edgeButtonSelector: 'button.edge-label'
        });

        this.after('initialize', function() {
            this.on('click', {
                vertexButtonSelector: this.onVertexRequeue,
                edgeButtonSelector: this.onEdgeRequeue
            });

            this.$node.html(template({}));
        });

        this.onVertexRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.dataRequest(
                      'admin',
                      'queueVertices',
                      this.select('vertexPrioritySelector').val(),
                      this.select('vertexConceptTypeSelector').val(),
                      this.select('vertexPropertyNameSelector').val()
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
                  this.select('edgeLabelSelector').val()
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
        }
    }
});

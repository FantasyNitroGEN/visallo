require([
    'configuration/admin/plugin',
    'hbs!org/visallo/web/devTools/templates/requeue',
    'util/withDataRequest'
], function(
    defineVisalloAdminPlugin,
    template,
    withDataRequest) {
    'use strict';

    return defineVisalloAdminPlugin(Requeue, {
        mixins: [withDataRequest],
        section: 'Vertex',
        name: 'Requeue',
        subtitle: 'Requeue vertices and edges'
    });

    function Requeue() {

        this.defaultAttrs({
            vertexPropertyNameSelector: 'input.vertex-property-name',
            vertexPropertyNameButton: 'button.vertex-property-name',
            edgeLabelSelector: 'input.edge-label',
            edgeLabelButton: 'button.edge-label',
            vertexButton: 'button.vertex',
            edgeButton: 'button.edge'
        });

        this.after('initialize', function() {
            this.on('click', {
                vertexPropertyNameButton: this.onVertexPropertyNameRequeue,
                edgeLabelButton: this.onEdgeLabelRequeue,
                vertexButton: this.onVertexRequeue,
                edgeButton: this.onEdgeRequeue
            });

            this.$node.html(template({}));
        });

        this.onVertexPropertyNameRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.dataRequest('admin', 'queueVertices', this.select('vertexPropertyNameSelector').val())
                )
            );
        };

        this.onEdgeLabelRequeue = function(event) {
            this.handleSubmitButton(event.target,
              this.showResult(
                this.dataRequest('admin', 'queueEdges', this.select('edgeLabelSelector').val())
              )
            );
        };

        this.onVertexRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.dataRequest('admin', 'queueVertices')
                )
            );
        };

        this.onEdgeRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.dataRequest('admin', 'queueEdges')
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

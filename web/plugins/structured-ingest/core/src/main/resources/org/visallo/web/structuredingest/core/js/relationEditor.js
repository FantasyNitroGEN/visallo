define([
    'flight/lib/component',
    '../templates/relationEditor.hbs',
    'util/withDataRequest',
    'util/vertex/formatters',
    'util/ontology/relationshipSelect',
    'util/visibility/edit',
    'require'
], function(
    defineComponent,
    template,
    withDataRequest,
    F,
    RelationshipSelector,
    Visibility,
    require) {
    'use strict';

    var idIncrementor = 0;

    return defineComponent(RelationEditor, withDataRequest);

    function RelationEditor() {

        this.defaultAttrs({
            closeSelector: '.cancel-mapping',
            selectSelector: '.selector',
            visibilitySelector: '.visibility',
            addSelector: '.add-edge'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                closeSelector: this.onClose,
                addSelector: this.onAddEdge
            });

            Promise.resolve(this.render())
                .then(function() {
                    self.trigger('fieldRendered');
                })
        });

        this.after('teardown', function() {
            this.$node.empty();
        })

        this.render = function() {
            var self = this;

            this.on('relationshipSelected', this.onRelationshipSelected);
            this.on('visibilitychange', this.onVisibilityChanged);

            this.$node.html(template(this.attr));

            return new Promise(function(fulfill) {
                self.off('rendered');
                self.on('rendered', fulfill);
                RelationshipSelector.attachTo(self.select('selectSelector'), {
                    sourceConcept: self.attr.sourceConcept,
                    targetConcept: self.attr.targetConcept
                });
                Visibility.attachTo(self.select('visibilitySelector'), {
                    placeholder: 'Relationship Visibility'
                });
            })
        };

        this.onVisibilityChanged = function(event, data) {
            this.visibility = data.value;
        };

        this.onRelationshipSelected = function(event, data) {
            this.selectedRelationship = data.relationship;
            this.select('addSelector').prop('disabled', !data.relationship);
        };

        this.onAddEdge = function() {
            if (this.selectedRelationship) {
                this.trigger('updateMappedObject', {
                    type: 'edge',
                    finished: true,
                    object: {
                        id: 'edge-' + (idIncrementor++),
                        displayName: [this.attr.sourceDisplayName, this.attr.targetDisplayName].join(' → '),
                        label: this.selectedRelationship.title,
                        outVertex: this.attr.sourceIndex,
                        inVertex: this.attr.targetIndex,
                        visibilitySource: this.visibility
                    }
                })
                this.teardown();
            }
        };

        this.onClose = function() {
            this.trigger('updateMappedObject');
        };
    }
});

define([
    'flight/lib/component',
    'util/ontology/relationshipSelect',
    'configuration/admin/utils/withFormHelpers',
    'hbs!org/visallo/web/devTools/templates/ontology-edit-relationship',
    'util/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    RelationshipSelector,
    withFormHelpers,
    template,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(OntologyEditRelationship, withDataRequest, withFormHelpers);

    function OntologyEditRelationship() {

        this.defaultAttrs({
            relationshipSelector: '.relationship-container',
            buttonSelector: '.btn-primary'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                buttonSelector: this.onSave
            });

            this.on('relationshipSelected', this.onRelationshipSelected);

            this.$node.html(template({}));

            Promise.all([
                Promise.require('fields/selection/selection'),
                Promise.require('util/messages'),
                this.dataRequest('ontology', 'relationships')
            ]).done(function(results) {
                var FieldSelection = results.shift(),
                    i18n = results.shift(),
                    relationships = results.shift();

                RelationshipSelector.attachTo(self.select('relationshipSelector'), {
                    relationships: relationships.list,
                    showAdminProperties: true,
                    placeholder: i18n('relationship.form.field.selection.placeholder')
                });
            })
        });

        this.onSave = function() {
            var self = this;

            this.handleSubmitButton(
                this.select('buttonSelector'),
                this.dataRequest('io-visallo-web-devTools', 'ontologyEditRelationship', {
                    relationship: this.currentRelationship,
                    displayName: this.$node.find('.displayName').val(),
                    intents: this.$node.find('.intents').val().split(/[\n\s,]+/)
                })
                    .then(function() {
                        self.showSuccess('Saved, refresh to see changes');
                    })
                    .catch(function() {
                        self.showError();
                    })

            )
        };

        this.onRelationshipSelected = function(event, data) {
            var self = this;

            if (data.relationship) {
                this.currentRelationship = data.relationship.title;
                this.$node.find('.btn-primary').removeAttr('disabled');

                this.$node.find('*').not('.relationship-container *').val('').removeAttr('checked');
                _.each(data.relationship, function(value, key) {
                    if (key === 'intents') {
                        value = value.join('\n');
                    }
                    self.updateFieldValue(key, value)
                });
            } else {
                this.$node.find('.btn-primary').attr('disabled', true);
            }
        };

        this.updateFieldValue = function(field, value) {
            var $field = this.$node.find('.' + field),
                type = $field.prop('type');

            if (!$field.length) {
                return;
            }

            switch (type) {
                case 'text':
                case 'textarea':
                    $field.val(value);
                    break;
                case 'checkbox':
                    $field.prop('checked', value);
                    break;
                default:
                    console.error('Unhandled type', type);
            }
        }
    }
});

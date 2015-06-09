require([
    'configuration/admin/plugin',
    'hbs!org/visallo/web/devTools/templates/ontology-edit-property',
    'util/formatters',
    'util/withDataRequest',
], function(
    defineVisalloAdminPlugin,
    template,
    F,
    withDataRequest) {
    'use strict';

    return defineVisalloAdminPlugin(OntologyEditProperty, {
        mixins: [withDataRequest],
        section: 'Ontology',
        name: 'Properties',
        subtitle: 'Modify ontology properties'
    });

    function OntologyEditProperty() {

        this.defaultAttrs({
            propertySelector: '.property-container',
            buttonSelector: '.btn-primary'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                buttonSelector: this.onSave
            });

            this.on('propertyselected', this.onPropertySelected);

            this.$node.html(template({}));

            Promise.all([
                Promise.require('fields/selection/selection'),
                Promise.require('util/messages'),
                this.dataRequest('ontology', 'properties')
            ]).done(function(results) {
                var FieldSelection = results.shift(),
                    i18n = results.shift(),
                    properties = results.shift();

                FieldSelection.attachTo(self.select('propertySelector'), {
                    properties: properties.list,
                    showAdminProperties: true,
                    placeholder: i18n('property.form.field.selection.placeholder')
                });
            })
        });

        this.onSave = function() {
            var self = this;

            this.handleSubmitButton(
                this.select('buttonSelector'),
                this.dataRequest('io-visallo-web-devTools', 'ontologyEditProperty', {
                    property: this.currentProperty,
                    displayName: this.$node.find('.displayName').val(),
                    dataType: this.$node.find('.dataType').val(),
                    displayType: this.$node.find('.displayType').val(),
                    addable: this.$node.find('.addable').is(':checked'),
                    searchable: this.$node.find('.searchable').is(':checked'),
                    userVisible: this.$node.find('.userVisible').is(':checked'),
                    displayFormula: this.$node.find('.displayFormula').val(),
                    possibleValues: this.$node.find('.possibleValues').val(),
                    dependentPropertyIris: this.$node.find('.dependentPropertyIris')
                        .val().split(/[\n\s,]+/)
                })
                    .then(function() {
                        self.showSuccess('Saved, refresh to see changes');
                    })
                    .catch(function() {
                        self.showError();
                    })

            )
        };

        this.onPropertySelected = function(event, data) {
            var self = this;

            if (data.property) {
                this.currentProperty = data.property.title;
                this.$node.find('.btn-primary').removeAttr('disabled');

                data.property.userVisible = data.property.userVisible !== false;
                data.property.searchable = data.property.searchable !== false;
                data.property.addable = data.property.addable !== false;

                this.$node.find('*').not('.property-container *').val('').removeAttr('checked');
                _.each(data.property, function(value, key) {
                    if (key === 'dependentPropertyIris') {
                        value = value.join('\n');
                    }
                    if (key === 'possibleValues' && value) {
                        value = JSON.stringify(value, null, 2);
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
                case 'color':
                    $field.val(rgbToHex(value));
                    break;
                default:
                    console.error('Unhandled type', type);
            }
        }
    }
});

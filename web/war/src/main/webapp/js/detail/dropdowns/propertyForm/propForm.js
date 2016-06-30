define([
    'require',
    'flight/lib/component',
    'util/withDropdown',
    'tpl!./propForm',
    'util/ontology/propertySelect',
    'tpl!util/alert',
    'util/withTeardown',
    'util/vertex/vertexSelect',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    require,
    defineComponent,
    withDropdown,
    template,
    FieldSelection,
    alertTemplate,
    withTeardown,
    VertexSelector,
    F,
    withDataRequest
) {
    'use strict';

    return defineComponent(PropertyForm, withDropdown, withTeardown, withDataRequest);

    function PropertyForm() {

        this.defaultAttrs({
            propertyListSelector: '.property-list',
            saveButtonSelector: '.btn-primary',
            deleteButtonSelector: '.btn-danger',
            configurationSelector: '.configuration',
            configurationFieldSelector: '.configuration input',
            previousValuesSelector: '.previous-values',
            previousValuesDropdownSelector: '.previous-values-container .dropdown-menu',
            vertexContainerSelector: '.vertex-select-container',
            visibilitySelector: '.visibility',
            justificationSelector: '.justification',
            visibilityInputSelector: '.visibility input'
        });

        this.before('initialize', function(n, c) {
            c.manualOpen = true;
        });

        this.after('initialize', function() {
            var self = this,
                property = this.attr.property,
                vertex = this.attr.data;

            this.modified = {};

            this.on('click', {
                saveButtonSelector: this.onSave,
                deleteButtonSelector: this.onDelete,
                previousValuesSelector: this.onPreviousValuesButtons
            });
            this.on('keyup', {
                configurationFieldSelector: this.onKeyup,
                justificationSelector: this.onKeyup,
                visibilityInputSelector: this.onKeyup
            });

            this.on('propertyerror', this.onPropertyError);
            this.on('propertychange', this.onPropertyChange);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('propertyselected', this.onPropertySelected);
            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);
            this.on('paste', {
                configurationFieldSelector: _.debounce(this.onPaste.bind(this), 10)
            });
            this.on('click', {
                previousValuesDropdownSelector: this.onPreviousValuesDropdown
            });
            this.$node.html(template({
                property: property,
                vertex: vertex
            }));

            this.select('saveButtonSelector').attr('disabled', true);
            this.select('deleteButtonSelector').hide();
            this.select('saveButtonSelector').hide();

            if (this.attr.property) {
                this.trigger('propertyselected', {
                    disablePreviousValuePrompt: true,
                    property: _.chain(property)
                        .pick('displayName key name value visibility metadata'.split(' '))
                        .extend({
                            title: property.name
                        })
                        .value()
                });
            } else if (!vertex) {
                this.on('vertexSelected', this.onVertexSelected);
                VertexSelector.attachTo(this.select('vertexContainerSelector'), {
                    value: ''
                });
                this.manualOpen();
            } else {
                this.setupPropertySelectionField();
            }
        });

        this.setupPropertySelectionField = function() {
            var self = this,
                ontologyRequest,
                aclRequest;

            if (F.vertex.isEdge(this.attr.data)) {
                ontologyRequest = this.dataRequest('ontology', 'propertiesByRelationship', this.attr.data.label);
                aclRequest = this.dataRequest('edge', 'acl', this.attr.data.id);
            } else {
                ontologyRequest = this.dataRequest('ontology', 'propertiesByConceptId',
                    F.vertex.prop(this.attr.data, 'conceptType'));
                aclRequest = this.dataRequest('vertex', 'acl', this.attr.data.id);
            }

            Promise.all([ontologyRequest, aclRequest]).done(function(results) {
                var ontologyProperties = results[0],
                    acl = results[1];

                FieldSelection.attachTo(self.select('propertyListSelector'), {
                    properties: ontologyProperties.list,
                    focus: true,
                    placeholder: i18n('property.form.field.selection.placeholder'),
                    unsupportedProperties: _.pluck(_.where(acl.propertyAcls, { addable: false }), 'name')
                });
                self.manualOpen();
            });
        };

        this.onVertexSelected = function(event, data) {
            event.stopPropagation();

            if (data.item && data.item.properties) {
                this.attr.data = data.item;
                this.setupPropertySelectionField();
            }
        };

        this.after('teardown', function() {
            this.select('visibilitySelector').teardownAllComponents();

            if (this.$node.closest('.buttons').length === 0) {
                this.$node.closest('tr').remove();
            }
        });

        this.onPaste = function(event) {
            var self = this,
                value = $(event.target).val();

            _.defer(function() {
                self.trigger(
                    self.select('justificationSelector'),
                    'valuepasted',
                    { value: value }
                );
            });
        };

        this.onPreviousValuesButtons = function(event) {
            var self = this,
                dropdown = this.select('previousValuesDropdownSelector'),
                buttons = this.select('previousValuesSelector').find('.active').removeClass('active'),
                action = $(event.target).closest('button').addClass('active').data('action');

            event.stopPropagation();
            event.preventDefault();

            if (action === 'add') {
                dropdown.hide();
                this.trigger('propertyselected', {
                    fromPreviousValuePrompt: true,
                    property: _.omit(this.currentProperty, 'value', 'key')
                });
            } else if (this.previousValues.length > 1) {
                this.trigger('propertyselected', {
                    property: _.omit(this.currentProperty, 'value', 'key')
                });

                dropdown.html(
                        this.previousValues.map(function(p, i) {
                            var visibility = p.metadata && p.metadata['http://visallo.org#visibilityJson'];
                            return _.template(
                                '<li data-index="{i}">' +
                                    '<a href="#">{value}' +
                                        '<div data-visibility="{visibilityJson}" class="visibility"/>' +
                                    '</a>' +
                                '</li>')({
                                value: F.vertex.prop(self.attr.data, self.previousValuesPropertyName, p.key),
                                visibilityJson: JSON.stringify(visibility || {}),
                                i: i
                            });
                        }).join('')
                    ).show();

                require(['util/visibility/view'], function(Visibility) {
                    dropdown.find('.visibility').each(function() {
                        var value = $(this).data('visibility');
                        Visibility.attachTo(this, {
                            value: value && value.source
                        });
                    });
                });

            } else {
                dropdown.hide();
                this.trigger('propertyselected', {
                    fromPreviousValuePrompt: true,
                    property: $.extend({}, this.currentProperty, this.previousValues[0])
                });
            }
        };

        this.onPreviousValuesDropdown = function(event) {
            var li = $(event.target).closest('li'),
                index = li.data('index');

            this.$node.find('.previous-values .edit-previous').addClass('active');
            this.trigger('propertyselected', {
                fromPreviousValuePrompt: true,
                property: $.extend({}, this.currentProperty, this.previousValues[index])
            });
        };

        this.onPropertySelected = function(event, data) {
            var self = this,
                property = data.property,
                disablePreviousValuePrompt = data.disablePreviousValuePrompt,
                propertyName = property.title,
                config = self.select('configurationSelector'),
                visibility = self.select('visibilitySelector'),
                justification = self.select('justificationSelector');

            this.currentProperty = property;
            this.$node.find('.errors').hide();

            config.teardownAllComponents();
            visibility.teardownAllComponents();
            justification.teardownAllComponents();

            var vertexProperty = property.title === 'http://visallo.org#visibilityJson' ?
                    _.first(F.vertex.props(this.attr.data, property.title)) :
                    !_.isUndefined(property.key) ?
                    _.first(F.vertex.props(this.attr.data, property.title, property.key)) :
                    undefined,
                previousValue = vertexProperty && vertexProperty.value,
                visibilityValue = vertexProperty &&
                    vertexProperty.metadata &&
                    vertexProperty.metadata['http://visallo.org#visibilityJson'],
                sandboxStatus = vertexProperty && vertexProperty.sandboxStatus,
                isExistingProperty = typeof vertexProperty !== 'undefined',
                previousValues = disablePreviousValuePrompt !== true && F.vertex.props(this.attr.data, propertyName),
                previousValuesUniquedByKey = previousValues && _.unique(previousValues, _.property('key')),
                previousValuesUniquedByKeyUpdateable = _.where(previousValuesUniquedByKey, {updateable: true});


            this.currentValue = this.attr.attemptToCoerceValue || previousValue;
            if (this.currentValue && _.isObject(this.currentValue) && ('latitude' in this.currentValue)) {
                this.currentValue = 'point(' + this.currentValue.latitude + ',' + this.currentValue.longitude + ')';
            }

            if (visibilityValue) {
                visibilityValue = visibilityValue.source;
                this.visibilitySource = { value: visibilityValue, valid: true };
            }

            if (property.name === 'http://visallo.org#visibilityJson') {
                vertexProperty = property;
                isExistingProperty = true;
                previousValues = null;
                previousValuesUniquedByKey = null;
            }

            if (data.fromPreviousValuePrompt !== true) {
                if (previousValuesUniquedByKeyUpdateable && previousValuesUniquedByKeyUpdateable.length) {
                    this.previousValues = previousValuesUniquedByKeyUpdateable;
                    this.previousValuesPropertyName = propertyName;
                    this.select('previousValuesSelector')
                        .show()
                        .find('.active').removeClass('active')
                        .addBack()
                        .find('.edit-previous span').text(previousValuesUniquedByKeyUpdateable.length)
                        .addBack()
                        .find('.edit-previous small').toggle(previousValuesUniquedByKeyUpdateable.length > 1);

                    this.select('justificationSelector').hide();
                    this.select('visibilitySelector').hide();
                    this.select('saveButtonSelector').hide();
                    this.select('previousValuesDropdownSelector').hide();

                    return;
                } else {
                    this.select('previousValuesSelector').hide();
                }
            }

            this.select('previousValuesDropdownSelector').hide();
            this.select('justificationSelector').show();
            this.select('visibilitySelector').show();
            this.select('saveButtonSelector').show();

            this.select('deleteButtonSelector')
                .toggle(
                    !!isExistingProperty &&
                    propertyName !== 'http://visallo.org#visibilityJson'
                );

            var button = this.select('saveButtonSelector')
                .text(isExistingProperty ? i18n('property.form.button.update') : i18n('property.form.button.add'));

            button.attr('disabled', true);

            this.dataRequest('ontology', 'properties').done(function(properties) {
                var propertyDetails = properties.byTitle[propertyName];
                self.currentPropertyDetails = propertyDetails;
                if (propertyName === 'http://visallo.org#visibilityJson') {
                    require(['util/visibility/edit'], function(Visibility) {
                        var val = vertexProperty && vertexProperty.value,
                            source = (val && val.source) || (val && val.value && val.value.source);

                        Visibility.attachTo(visibility, {
                            value: source || ''
                        });
                        visibility.find('input').focus();
                        self.settingVisibility = true;
                        self.visibilitySource = { value: source, valid: true };

                        self.checkValid();
                        self.manualOpen();
                    });
                } else if (propertyDetails) {
                    var isCompoundField = propertyDetails.dependentPropertyIris &&
                        propertyDetails.dependentPropertyIris.length,
                        fieldComponent;

                    if (isCompoundField) {
                        self.currentValue = _.pluck(F.vertex.props(self.attr.data, propertyName), 'value');
                        fieldComponent = 'fields/compound/compound';
                    } else if (propertyDetails.displayType === 'duration') {
                        fieldComponent = 'fields/duration';
                    } else {
                        fieldComponent = propertyDetails.possibleValues ?
                            'fields/restrictValues' : 'fields/' + propertyDetails.dataType;
                    }

                    require([
                        fieldComponent,
                        'detail/dropdowns/propertyForm/justification',
                        'util/visibility/edit'
                    ], function(PropertyField, Justification, Visibility) {
                        if (self.attr.manualOpen) {
                            var $toHide = $()
                                .add(config)
                                .add(justification)
                                .add(visibility)
                                .hide();
                        }

                        Justification.attachTo(justification, {
                            justificationText: self.attr.justificationText,
                            sourceInfo: self.attr.sourceInfo
                        });

                        Visibility.attachTo(visibility, {
                            value: visibilityValue || ''
                        });

                        self.settingVisibility = false;
                        self.checkValid();
                        self.$node.find('configuration').hide();

                        self.on('fieldRendered', function() {
                            if ($toHide) {
                                $toHide.show();
                            }
                            self.manualOpen();
                        });
                        if (isCompoundField) {
                            PropertyField.attachTo(config, {
                                property: propertyDetails,
                                vertex: self.attr.data,
                                values: property.key !== undefined ?
                                    F.vertex.props(self.attr.data, propertyDetails.title, property.key) :
                                    null
                            });
                        } else {
                            PropertyField.attachTo(config, {
                                property: propertyDetails,
                                vertexProperty: vertexProperty,
                                value: self.attr.attemptToCoerceValue || previousValue,
                                tooltip: (!self.attr.sourceInfo && !self.attr.justificationText) ? {
                                    html: true,
                                    title:
                                        '<strong>' +
                                        i18n('justification.field.tooltip.title') +
                                        '</strong><br>' +
                                        i18n('justification.field.tooltip.subtitle'),
                                    placement: 'left',
                                    trigger: 'focus'
                                } : null
                            });
                        }
                        self.previousPropertyValue = self.getConfigurationValues();
                    });
                } else console.warn('Property ' + propertyName + ' not found in ontology');
            });
        };

        this.onVisibilityChange = function(event, data) {
            var self = this;

            this.visibilitySource = data;
            this.modified.visibility = this.currentProperty.metadata ? visibilityModified() : !!this.visibilitySource.value;
            this.checkValid();

            function visibilityModified() {
                var currentVisibility = self.visibilitySource.value,
                    previousVisibility;
                if (self.currentProperty.title === 'http://visallo.org#visibilityJson') {
                    previousVisibility = self.currentProperty.value.source;
                } else {
                    previousVisibility = self.currentProperty.metadata['http://visallo.org#visibilityJson'].source;
                }

                if (!currentVisibility) {
                    return !!previousVisibility;
                } else {
                    return currentVisibility !== previousVisibility;
                }
            }
        };

        this.onJustificationChange = function(event, data) {
            var self = this;

            this.justification = data;
            this.modified.justification = this.currentProperty.metadata ? justificationModified() : !!this.justification.justificationText;
            this.checkValid();

            function justificationModified() {
                var previousJustification = self.currentProperty.metadata['http://visallo.org#justification'],
                    currentJustificationText = self.justification && self.justification.hasOwnProperty('justificationText') ?
                        self.justification.justificationText : undefined;

                if (previousJustification !== undefined) {
                    return currentJustificationText !== previousJustification.justificationText;
                } else {
                    return !!currentJustificationText;
                }
            }
        };

        this.onPropertyInvalid = function(event, data) {
            event.stopPropagation();

            this.propertyInvalid = true;
            this.checkValid();
        };

        this.checkValid = function() {
            if (this.settingVisibility) {
                this.valid = this.visibilitySource && this.visibilitySource.valid;
            } else {
                var valid = !this.propertyInvalid &&
                    (this.visibilitySource && this.visibilitySource.valid) &&
                    (this.justification ? this.justification.valid : true);
                var empty = _.reject(this.$node.find('.configuration input'), function(input) {
                    return !input.required || !!input.value;
                }).length > 0;

                this.valid = valid && !empty && _.some(this.modified);
            }

            if (this.valid) {
                this.select('saveButtonSelector').removeAttr('disabled');
            } else {
                this.select('saveButtonSelector').attr('disabled', true);
            }
        };

        this.onPropertyChange = function(event, data) {
            var self = this;

            this.propertyInvalid = false;
            event.stopPropagation();

            var isCompoundField = this.currentPropertyDetails.dependentPropertyIris,
                transformValue = function(valueArray) {
                    if (valueArray.length === 1) {
                        if (_.isObject(valueArray[0]) && ('latitude' in valueArray[0])) {
                            return JSON.stringify(valueArray[0])
                        }
                        return valueArray[0];
                    } else if (valueArray.length === 2) {
                        // Must be geoLocation
                        return 'point(' + valueArray.join(',') + ')';
                    } else if (valueArray.length === 3) {
                        return JSON.stringify({
                            description: valueArray[0],
                            latitude: valueArray[1],
                            longitude: valueArray[2]
                        });
                    }
                };

            if (isCompoundField) {
                this.currentValue = _.map(data.values, transformValue);
            } else {
                this.currentValue = data.value;
            }

            this.currentMetadata = data.metadata;
            this.modified.value = this.currentProperty.value ? valueModified() : !!this.currentValue;
            this.checkValid();


            function valueModified() {
                var previousValue = self.previousPropertyValue,
                    propertyValue = self.getConfigurationValues();

                if (previousValue !== undefined) {
                    return propertyValue !== previousValue;
                } else {
                    return !!propertyValue;
                }
            }
        };

        this.onPropertyError = function(event, data) {
            var messages = this.markFieldErrors(data.error);

            this.$node.find('.errors').html(
                alertTemplate({
                    error: messages
                })
            ).show();
            _.defer(this.clearLoading.bind(this));
        };

        this.getConfigurationValues = function() {
            var config = this.select('configurationSelector').lookupAllComponents().shift();

            return _.isFunction(config.getValue) ? config.getValue() : config.getValues();
        };

        this.onKeyup = function(evt) {
            if (evt.which === $.ui.keyCode.ENTER) {
                this.onSave();
            }
        };

        this.onDelete = function() {
            _.defer(this.buttonLoading.bind(this, this.attr.deleteButtonSelector));
            this.trigger('deleteProperty', {
                vertexId: this.attr.data.id,
                property: _.pick(this.currentProperty, 'key', 'name'),
                node: this.node
            });
        };

        this.onSave = function(evt) {
            var self = this;

            if (!this.valid) return;

            var vertexId = this.attr.data.id,
                propertyKey = this.currentProperty.key,
                propertyName = this.currentProperty.title,
                value = this.currentValue,
                justification = _.pick(this.justification || {}, 'sourceInfo', 'justificationText'),
                oldMetadata = this.currentProperty.metadata,
                oldVisibilitySource = oldMetadata && oldMetadata['http://visallo.org#visibilityJson']
                    ? oldMetadata['http://visallo.org#visibilityJson'].source
                    : undefined;

            _.defer(this.buttonLoading.bind(this, this.attr.saveButtonSelector));

            this.$node.find('input').tooltip('hide');

            this.$node.find('.errors').hide();
            if (propertyName.length &&
                (
                    this.settingVisibility ||
                    (
                        (_.isString(value) && value.length) ||
                        _.isNumber(value) ||
                        value
                    )
                )) {

                this.trigger('addProperty', {
                    isEdge: F.vertex.isEdge(this.attr.data),
                    vertexId: this.attr.data.id,
                    property: $.extend({
                            key: propertyKey,
                            name: propertyName,
                            value: value,
                            visibilitySource: this.visibilitySource.value,
                            oldVisibilitySource: oldVisibilitySource,
                            metadata: this.currentMetadata
                        }, justification),
                    node: this.node
                });
            }
        };
    }
});

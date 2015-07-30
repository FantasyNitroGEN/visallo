
define([
    'flight/lib/component',
    '../withPopover',
    'util/visibility/edit',
    'util/ontology/conceptSelect',
    'util/formatters',
    'util/withFormFieldErrors',
    'util/withDataRequest'
], function(
    defineComponent,
    withPopover,
    VisibilityEditor,
    ConceptSelector,
    F,
    withFormFieldErrors,
    withDataRequest) {
    'use strict';

    return defineComponent(FileImport, withPopover, withFormFieldErrors, withDataRequest);

    function FileImport() {

        this.defaultAttrs({
            importSelector: '.btn-primary',
            cancelSelector: '.btn-default',
            checkboxSelector: '.checkbox',
            visibilityInputSelector: '.visibility',
            conceptSelector: '.concept-container'
        });

        this.after('teardown', function() {
            if (this.request && this.request.cancel) {
                this.request.cancel();
            }
        });

        this.before('initialize', function(node, config) {
            config.template = 'fileImport/template';

            if (!config.files) config.files = [];

            config.rolledUp = [{
                name: config.stringType || (
                    config.files.length === 1 ?
                    config.files[0].name :
                    i18n('popovers.file_import.files.some', config.files.length)
                ),
                size: config.stringType ? '' :
                    F.bytes.pretty(
                        _.chain(config.files.length ? config.files : config.items)
                            .map(_.property('size'))
                            .reduce(function(memo, num) {
                                return memo + num;
                            }, 0)
                            .value()
                    ),
                index: 'collapsed'
            }];

            config.multipleFiles = config.files.length > 1;
            config.formattedFiles = _.map(config.files, function(f, i) {
                return {
                    name: f.name,
                    size: F.bytes.pretty(f.size, 0),
                    index: i
                };
            })
            config.pluralString = i18n('popovers.file_import.files.' + (
                config.files.length === 1 || config.stringType ? 'one' : 'some'
            ), config.files.length);

            this.after('setupWithTemplate', function() {
                var self = this;

                this.mousePosition = {
                    x: window.lastMousePositionX,
                    y: window.lastMousePositionY
                };
                this.visibilitySource = null;
                this.visibilitySources = new Array(config.files.length);
                this.concepts = new Array(config.files.length);

                this.on(this.popover, 'visibilitychange', this.onVisibilityChange);
                this.on(this.popover, 'conceptSelected', this.onConceptChange);

                this.enterShouldSubmit = 'importSelector';

                VisibilityEditor.attachTo(this.popover.find(this.attr.visibilityInputSelector));
                ConceptSelector.attachTo(this.popover.find(this.attr.conceptSelector).eq(0), {
                    focus: true,
                    defaultText: i18n('popovers.file_import.concept.placeholder')
                });
                ConceptSelector.attachTo(this.popover.find(this.attr.conceptSelector), {
                    defaultText: i18n('popovers.file_import.concept.placeholder')
                });

                this.on(this.popover, 'click', {
                    importSelector: this.onImport,
                    cancelSelector: this.onCancel
                });

                this.on(this.popover, 'change', {
                    checkboxSelector: this.onCheckboxCopy
                })

                this.on(this.popover, 'keyup', {
                    visibilityInputSelector: function(e) {
                        if ($(e.target).closest('.single').length &&
                            e.which === $.ui.keyCode.ENTER) {
                            this.onImport();
                        }
                    }
                })
            })
        });

        this.onCheckboxCopy = function(e) {
            var $checkbox = $(e.target),
                checked = $checkbox.is(':checked');

            this.popover.toggleClass('collapseVisibility', checked);
            this.popover.find('.errors').empty();
            _.delay(this.positionDialog.bind(this), 50);
        };

        this.onConceptChange = function(event, data) {
            var concept = data.concept,
                index = $(event.target)
                    .closest(this.attr.conceptSelector)
                    .data('concept', concept)
                    .data('fileIndex');

            if (index === 'collapsed') {
                this.concept = concept;
            } else {
                this.concepts[index] = concept;
            }

            this.checkValid();
        };

        this.onVisibilityChange = function(event, data) {
            var index = $(event.target)
                .data('visibility', data)
                .data('fileIndex');

            if (index === 'collapsed') {
                this.visibilitySource = data;
            } else {
                this.visibilitySources[index] = data;
            }

            this.checkValid();
        };

        this.checkValid = function() {
            var collapsed = this.isVisibilityCollapsed(),
                isValid = collapsed ?
                    (this.visibilitySource && this.visibilitySource.valid) :
                    _.every(this.visibilitySources, _.property('valid'));

            if (isValid) {
                this.popover.find('button').removeAttr('disabled');
            } else {
                this.popover.find('button').attr('disabled', true);
            }

            return isValid;
        };

        this.isVisibilityCollapsed = function() {
            var checkbox = this.popover.find('.checkbox input');

            return checkbox.length === 0 || checkbox.is(':checked');
        };

        this.onCancel = function() {
            this.teardown();
        };

        this.onImport = function() {
            if (!this.checkValid()) {
                return false;
            }

            var self = this,
                button = this.popover.find('.btn-primary')
                    .text(i18n('popovers.file_import.importing'))
                    .attr('disabled', true),
                cancelButton = this.popover.find('.btn-default').show(),
                collapsed = this.isVisibilityCollapsed(),
                files = this.attr.files,
                conceptValue = collapsed ?
                    this.concept && this.concept.id :
                    _.map(this.concepts, function(c) {
                        return c && c.id || '';
                    }),
                visibilityValue = collapsed ?
                    this.visibilitySource.value :
                    _.map(this.visibilitySources, _.property('value'));

            this.attr.teardownOnTap = false;

            var req = _.partial(this.dataRequest.bind(this), 'vertex', _, _, conceptValue, visibilityValue);
            if (this.attr.files.length) {
                this.request = req('importFiles', files);
            } else {
                this.request = req('importFileString', {
                    string: this.attr.string,
                    type: this.attr.stringMimeType
                });
            }

            this.request
                .progress(function(complete) {
                    var percent = Math.round(complete * 100);
                    button.text(percent + '% ' + i18n('popovers.file_import.importing'));
                })
                .then(function(result) {
                    self.trigger('updateWorkspace', {
                        options: {
                            selectAll: true
                        },
                        entityUpdates: result.vertexIds.map(function(vId, i) {
                            var page = self.attr.anchorTo.page;
                            return {
                                vertexId: vId,
                                graphLayoutJson: i === 0 ? {
                                    pagePosition: page && page.x && page.y ?
                                        page : self.mousePosition
                                } : {}
                            }
                        })
                    });
                    self.teardown();
                })
                .catch(function(error) {
                    self.attr.teardownOnTap = true;
                    // TODO: fix error
                    self.markFieldErrors(error || 'Unknown Error', self.popover);
                    cancelButton.hide();
                    button.text(i18n('popovers.file_import.button.import'))
                        .removeClass('loading')
                        .removeAttr('disabled')

                    _.defer(self.positionDialog.bind(self));
                })
        }
    }
});

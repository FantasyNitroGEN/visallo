
define([
    'flight/lib/component',
    '../withPopover',
    'hbs!./formattedFile',
    'detail/dropdowns/propertyForm/justification',
    'util/visibility/edit',
    'util/ontology/conceptSelect',
    'util/formatters',
    'util/withFormFieldErrors',
    'util/withDataRequest'
], function(
    defineComponent,
    withPopover,
    fileTemplate,
    Justification,
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
            toggleCheckboxSelector: '.toggle-collapsed',
            importFileButtonSelector: 'button.file-select',
            importFileSelector: 'input.file',
            visibilityInputSelector: '.visibility',
            justificationSelector: '.justification',
            conceptSelector: '.concept-container',
            singleSelector: '.single',
            individualVisibilitySelector: '.individual-visibility'
        });

        this.after('teardown', function() {
            if (this.request && this.request.cancel) {
                this.request.cancel();
            }
        });

        this.getTitle = function(files, stringType) {
            if (files.length) {
                var pluralString = i18n('popovers.file_import.files.' + (
                    files.length === 1 || stringType ? 'one' : 'some'
                ), files.length);
                return i18n('popovers.file_import.title', pluralString);
            }
            return i18n('popovers.file_import.nofile.title');
        };

        this.before('initialize', function(node, config) {
            config.template = 'fileImport/template';

            if (!config.files) config.files = [];

            config.hasFile = config.files.length > 0;
            config.multipleFiles = config.files.length > 1;
            config.title = this.getTitle(config.files, config.stringType);

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
                this.on(this.popover, 'justificationchange', this.onJustificationChange);
                this.on(this.popover, 'conceptSelected', this.onConceptChange);

                this.enterShouldSubmit = 'importSelector';

                this.on(this.popover, 'click', {
                    importSelector: this.onImport,
                    cancelSelector: this.onCancel
                });

                this.on(this.popover, 'change', {
                    toggleCheckboxSelector: this.onCheckboxCopy,
                    importFileSelector: this.onFileChange
                })

                this.on(this.popover, 'keyup', {
                    visibilityInputSelector: function(e) {
                        if ($(e.target).closest('.single').length &&
                            e.which === $.ui.keyCode.ENTER) {
                            this.onImport();
                        }
                    }
                })

                this.setFiles(this.attr.files);
                this.checkValid();
            })
        });

        this.onFileChange = function(event) {
            var files = event.target.files;
            if (files.length) {
                this.setFiles(files);
            }
        };

        this.setFiles = function(files) {
            this.attr.files = files;
            this.popover.find('.popover-title').text(this.getTitle(files, this.attr.stringType));
            this.popover.find(this.attr.importSelector).text(files.length ?
                i18n('popovers.file_import.button.import') :
                i18n('popovers.file_import.button.nofile.import')
            );
            var $single = this.popover.find(this.attr.singleSelector).hide();

            if (files.length === 0) {
                this.popover.find(this.attr.importFileButtonSelector).toggle(!this.attr.stringType);
                this.popover.find(this.attr.toggleCheckboxSelector).hide();
                this.popover.find(this.attr.individualVisibilitySelector).hide();
                $single.html(fileTemplate({
                    name: this.attr.stringType || undefined,
                    index: 'collapsed',
                    justification: true
                }))
            } else if (files.length === 1) {
                this.popover.find(this.attr.importFileButtonSelector).hide();
                this.popover.find(this.attr.toggleCheckboxSelector).hide();
                this.popover.find(this.attr.individualVisibilitySelector).hide();
                $single.html(fileTemplate({
                    name: files[0].name,
                    size: F.bytes.pretty(files[0].size, 0),
                    index: 'collapsed'
                }));
            } else {
                this.popover.find(this.attr.importFileButtonSelector).hide();
                this.popover.find(this.attr.toggleCheckboxSelector).show();
                $single.html(fileTemplate({
                    name: i18n('popovers.file_import.files.some', files.length),
                    size: F.bytes.pretty(_.chain(files)
                            .map(_.property('size'))
                            .reduce(function(memo, num) {
                                return memo + num;
                            }, 0)
                            .value()),
                    index: 'collapsed'
                }));
                this.popover.find(this.attr.individualVisibilitySelector)
                    .hide()
                    .html(
                        $.map(files, function(file, i) {
                            return fileTemplate({
                                name: file.name,
                                size: F.bytes.pretty(file.size, 0),
                                index: i
                            })
                        })
                    )
            }

            Justification.attachTo(this.popover.find(this.attr.justificationSelector).eq(0));
            VisibilityEditor.attachTo(this.popover.find(this.attr.visibilityInputSelector));
            ConceptSelector.attachTo(this.popover.find(this.attr.conceptSelector).eq(0), {
                focus: true,
                defaultText: files.length ?
                    i18n('popovers.file_import.concept.placeholder') :
                    i18n('popovers.file_import.concept.nofile.placeholder')
            });
            ConceptSelector.attachTo(this.popover.find(this.attr.conceptSelector), {
                defaultText: i18n('popovers.file_import.concept.placeholder')
            });
            $single.show();

            this.positionDialog();
        }

        this.onCheckboxCopy = function(e) {
            var $checkbox = $(e.target),
                checked = $checkbox.is(':checked');

            this.popover.toggleClass('collapseVisibility', checked);
            this.popover.find(this.attr.individualVisibilitySelector).toggle(!checked);
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

        this.onJustificationChange = function(event, data) {
            this.justification = data;
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
                    (this.visibilitySource && this.visibilitySource.valid &&
                     (this.attr.files.length || (
                         this.justification && this.justification.valid))
                    ) :
                    _.every(this.visibilitySources, _.property('valid'));

            if (isValid && this.attr.files.length === 0 && !this.concept) {
                isValid = false;
            }

            this.popover.find(this.attr.importSelector).prop('disabled', !isValid);

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
                files = this.attr.files,
                button = this.popover.find('.btn-primary')
                    .text(files.length ?
                          i18n('popovers.file_import.importing') :
                          i18n('popovers.file_import.creating')
                    )
                    .attr('disabled', true),
                cancelButton = this.popover.find('.btn-default').show(),
                collapsed = this.isVisibilityCollapsed(),
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
            if (files.length) {
                this.request = req('importFiles', files);
            } else if (this.attr.string) {
                this.request = req('importFileString', {
                    string: this.attr.string,
                    type: this.attr.stringMimeType
                });
            } else {
                this.request = req('create', this.justification);
            }

            if (_.isFunction(this.request.progress)) {
                this.request
                    .progress(function(complete) {
                        var percent = Math.round(complete * 100);
                        button.text(percent + '% ' + (
                            files.length ?
                                  i18n('popovers.file_import.importing') :
                                  i18n('popovers.file_import.creating')
                        ));
                    })
            }

            this.request.then(function(result) {
                var vertexIds = _.isArray(result.vertexIds) ? result.vertexIds : [result.id];
                self.trigger('updateWorkspace', {
                    options: {
                        selectAll: true
                    },
                    entityUpdates: vertexIds.map(function(vId, i) {
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
                button.text(files.length ?
                        i18n('popovers.file_import.button.import') :
                        i18n('popovers.file_import.button.nofile.import')
                    )
                    .removeClass('loading')
                    .removeAttr('disabled')

                _.defer(self.positionDialog.bind(self));
            })
        }
    }
});

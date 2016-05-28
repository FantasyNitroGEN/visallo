define([
    'util/privileges'
], function(Privileges) {
    'use strict';

    FoldersNotSupported.prototype = Object.create(Error.prototype);

    return withFileDrop;

    function FoldersNotSupported() {}

    function withFileDrop() {

        this.after('initialize', function() {
            var self = this;

            if (!this.handleFilesDropped) {
                return console.warn('Implement handleFilesDropped');
            }

            if (this.attr.canEdit === false) {
                return;
            }

            this.node.ondragover = function(e) {
                if (Privileges.canEDIT) {
                    e.dataTransfer.dropEffect = 'copy';
                    $(this).addClass('file-hover');
                } else {
                    e.dataTransfer.dropEffect = 'none';
                    self.trigger('displayInformation', {
                        message: i18n('graph.workspace.readonly'),
                        position: [e.pageX, e.pageY]
                    });
                }
                return false;
            };
            this.node.ondragenter = function(e) {
                $(this).addClass('file-hover'); return false;
            };
            this.node.ondragleave = function(e) {
                if (!Privileges.canEDIT) {
                    self.trigger('hideInformation');
                }
                $(this).removeClass('file-hover');
                return false;
            };
            this.node.ondrop = function(e) {
                if (e.dataTransfer &&
                    e.dataTransfer.files) {

                    e.preventDefault();
                    e.stopPropagation();

                    if (self.$node.hasClass('uploading')) return;
                    if (e.dataTransfer.files.length === 0 &&
                        e.dataTransfer.items.length === 0) return;

                    if (Privileges.canEDIT) {
                        var dt = e.dataTransfer,
                            files = dt.files,
                            items = dt.items,
                            folderCheck = (files.length) ?
                                Promise.all(_.toArray(e.dataTransfer.files).map(function(file) {
                                    return new Promise(function(fulfill, reject) {
                                        var reader = new FileReader(),
                                            slice = file.slice(0, 1000);

                                        reader.onload = fulfill;
                                        reader.onerror = function() {
                                            reject(new FoldersNotSupported());
                                        };
                                        reader.readAsText(slice);
                                    })
                                })) :
                                Promise.resolve();

                        folderCheck
                            .then(function() {
                                if (files.length) {
                                    self.handleFilesDropped(files, e);
                                } else if (items.length && _.isFunction(self.handleItemsDropped)) {
                                    self.handleItemsDropped(items, e);
                                }
                            })
                            .catch(FoldersNotSupported, function(error) {
                                self.trigger('displayInformation', {
                                    message: i18n('popovers.file_import.folder.error'),
                                    position: [e.pageX, e.pageY]
                                });
                            })
                            .catch(function(error) {
                                console.error(error);

                                self.trigger('displayInformation', {
                                    message: i18n('popovers.file_import.general.error'),
                                    position: [e.pageX, e.pageY]
                                });
                            })
                    } else {
                        self.trigger('hideInformation');
                    }
                }
            };
        });
    }
});

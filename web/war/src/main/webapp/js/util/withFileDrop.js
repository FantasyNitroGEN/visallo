define([
    'util/privileges'
], function(Privileges) {
    'use strict';

    return withFileDrop;

    function withFileDrop() {

        this.after('initialize', function() {
            var self = this;

            if (!this.handleFilesDropped) {
                return console.warn('Implement handleFilesDropped');
            }

            this.node.ondragover = function(e) {
                e.dataTransfer.dropEffect = 'copy';
                $(this).addClass('file-hover'); return false;
            };
            this.node.ondragenter = function(e) {
                $(this).addClass('file-hover'); return false;
            };
            this.node.ondragleave = function(e) {
                $(this).removeClass('file-hover'); return false;
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
                                        var reader = new FileReader();
                                        reader.onload = fulfill;
                                        reader.onerror = reject;
                                        reader.readAsText(file);
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
                            .catch(function() {
                                self.trigger('displayInformation', {
                                    message: i18n('popovers.file_import.folder.error'),
                                    position: [e.pageX, e.pageY]
                                });
                            })
                    }
                }
            };
        });
    }
});

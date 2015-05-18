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
                        if (e.dataTransfer.files.length) {
                            self.handleFilesDropped(e.dataTransfer.files, e);
                        } else if (e.dataTransfer.items.length && _.isFunction(self.handleItemsDropped)) {
                            self.handleItemsDropped(e.dataTransfer.items, e);
                        }
                    }
                }
            };
        });
    }
});

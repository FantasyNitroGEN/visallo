define([
    'flight/lib/component',
    'configuration/plugins/registry'
], function(defineComponent, registry) {
    'use strict';

    return defineComponent(AddItem);

    function AddItem() {

        this.after('initialize', function() {
            this.add();
        });

        this.add = function() {
            var self = this;

            this.$node
                .addClass('add')
                .on('click', function() {
                    self.list();
                });
        };

        this.list = function() {
            var self = this,
                extensions = registry.extensionsForPoint('org.visallo.dashboard.item');

            this.$node
                .removeClass('add')
                .addClass('list')
                .html(
                    $('<ul>').append(
                        $.map(_.sortBy(extensions, function(e) {
                            return e.title.toLowerCase();
                        }), function(extension) {
                            return $('<li>')
                                .on('click', function() {
                                    self.trigger('addItem', {
                                        extension: extension
                                    });
                                })
                                .text(extension.title)
                                .append(
                                    $('<span>').text(extension.description)
                                )
                        })
                    )
                );
        };
    }
});

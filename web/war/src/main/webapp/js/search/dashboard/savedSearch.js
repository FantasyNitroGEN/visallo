define([
    'flight/lib/component',
    'util/withDataRequest'
], function(defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(SavedSearch, withDataRequest);

    function SavedSearch() {

        this.after('initialize', function() {
            var self = this;

            if (this.attr.item.configuration.searchId) {
                this.$node.html('Loading...');
                this.dataRequest('search', 'run',
                    this.attr.item.configuration.searchId,
                    this.attr.item.configuration.searchParameters
                )
                    .then(function(results) {
                        if (results.elements.length) {
                            require(['util/' + results.elements[0].type + '/list'], function(List) {
                                List.attachTo($('<div>').appendTo(self.$node.empty().css('overflow', 'auto')), {
                                    edges: results.elements,
                                    vertices: results.elements
                                })
                            })
                        } else {
                            self.$node.html('<i>No results</i>');
                        }
                    })
                    .catch(function(error) {
                        console.error(error);
                        self.trigger('showError');
                    })
            } else {
                this.$node
                    .css('overflow', 'inherit')
                    .html(
                        $('<a>')
                            .text('Configure Saved Search...')
                            .on('click', function() {
                                self.trigger('configureItem');
                            })
                    );
            }
        });

    }
});

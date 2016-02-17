define([
    'flight/lib/component',
    'util/withDataRequest'
], function(defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(SavedSearch, withDataRequest);

    function SavedSearch() {

        this.attributes({
            resultsContainerSelector: '.element-list',
            item: null
        });

        this.after('initialize', function() {
            var self = this;

            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);

            if (this.attr.item.configuration.searchId) {
                this.on('refreshData', this.loadItems);
                this.loadItems();
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

        this.loadItems = function() {
            var self = this;
            this.$node.text(i18n('dashboard.savedsearches.loading'));
            this.dataRequest('search', 'run',
                this.attr.item.configuration.searchId,
                this.attr.item.configuration.searchParameters
                )
                .then(function(results) {
                    if (results.elements.length) {
                        require(['util/' + results.elements[0].type + '/list'], function(List) {
                            List.attachTo($('<div>').appendTo(self.$node.empty().css('overflow', 'auto')), {
                                edges: results.elements,
                                vertices: results.elements,
                                infiniteScrolling: (results.elements.length < results.totalHits),
                                nextOffset: results.nextOffset
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
        }

       this.onInfiniteScrollRequest = function(event, data) {
            var trigger = this.trigger.bind(this,
               this.select('resultsContainerSelector'),
               'addInfiniteItems'
            );
            var options = _.extend({}, this.attr.item.configuration.searchParameters, data.paging)

            this.dataRequest('search', 'run',
                this.attr.item.configuration.searchId,
                options
            )
            .then(function(results) {
                 if (results) {
                     trigger({
                         success: true,
                         items: results.elements,
                         total: results.totalHits,
                         nextOffset: results.nextOffset
                     })
                 }
            })
            .catch(function() {
                trigger({success: false});
            })
        };
    }
});

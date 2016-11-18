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
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);

            this.loadSearch();
        });

        this.loadSearch = function() {
            const item = this.attr.item;
            if (item.configuration.searchId) {
                this.dataRequest('search', 'get', this.attr.item.configuration.searchId)
                    .then((search) => {
                        this.off('refreshData', this.loadSearch);
                        this.on('refreshData', this.loadSearch);
                        this.loadItems();
                    })
                    .catch((e) => {
                        item.configuration.searchId = '';
                        this.trigger('configurationChanged', {
                            item: item,
                            extension: this.attr.extension
                        });
                    });
            } else {
                this.setConfiguring();
            }
        };

        this.loadItems = function() {
            var self = this,
                config = this.attr.item.configuration,
                limitResults = config.searchParameters && _.isNumber(config.searchParameters.size);

            this.$node.text(i18n('dashboard.savedsearches.loading'));
            this.dataRequest('search', 'run', config.searchId, config.searchParameters)
                .then(function(results) {
                    if (results.elements.length) {
                        require(['util/element/list'], function(List) {
                            List.attachTo($('<div>').appendTo(self.$node.empty().css('overflow', 'auto')), {
                                edges: results.elements,
                                vertices: results.elements,
                                infiniteScrolling: !limitResults && (results.elements.length < results.totalHits),
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

        this.setConfiguring = function() {
            const self = this;

            this.select('resultsContainerSelector').teardownAllComponents();
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

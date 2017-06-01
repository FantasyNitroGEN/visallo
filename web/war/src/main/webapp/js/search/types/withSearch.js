define([
    'flight/lib/registry',
    '../filters/filters',
    './templates/type.hbs',
    'util/withDataRequest',
    'util/formatters',
    'util/element/list'
], function(
    registry,
    Filters,
    template,
    withDataRequest,
    F,
    ElementList
) {
    'use strict';

    return withSearch;

    function withSearch() {

        withDataRequest.call(this);

        this.attributes({
            resultsSelector: '.search-results',
            resultsContainerSelector: '.search-results .content > div',
            filtersSelector: '.search-filters'
        });

        this.after('initialize', function() {
            this.render();

            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);

            this.on('searchByParameters', this.onSearchByParameters);
            this.on('searchRequestCompleted', function(event, data) {
                if (data.success && data.result) {
                    var self = this,
                        result = data.result,
                        elements = result.elements,
                        $searchResults = this.select('resultsSelector'),
                        $resultsContainer = this.select('resultsContainerSelector')
                            .teardownAllComponents()
                            .empty(),
                        $hits = $searchResults.find('.total-hits').find('span').text(
                            i18n('search.results.none')
                        ).end().toggle(
                            _.isUndefined(result.totalHits) ?
                                result.elements.length === 0 :
                                result.totalHits === 0
                        );

                    if (result.totalHits === 0) {
                        $searchResults.hide();
                    } else {
                        $searchResults.show().children('.content').scrollTop(0);

                        ElementList.attachTo($resultsContainer, {
                            items: result.elements,
                            usageContext: 'searchresults',
                            nextOffset: result.nextOffset,
                            infiniteScrolling: this.attr.infiniteScrolling,
                            total: result.totalHits
                        });

                        this.makeResizable($searchResults);
                    }
                    this.trigger($searchResults, 'paneResized');
                }
            });
            this.on('clearSearch', function(event, data) {
                this.hideAndClearSearchResults();

                var filters = this.select('filtersSelector').find('.content')
                this.trigger(filters, 'clearfilters', data);
            });
            this.on('searchtypeloaded', function(event, data) {
                var filters = this.select('filtersSelector').find('.content')
                this.trigger(filters, 'enableMatchSelection', {
                    match: data.type === 'Visallo'
                })
            })
        });

        this.onToggleDisplay = function(event, data) {
            if (data.name === 'search') {
                if (this.$node.closest('.visible').length === 0) {
                    this.hideSearchResults();
                } else {
                    if (this.select('resultsContainerSelector').html().length > 0) {
                        this.showSearchResults();
                    }
                }
            }
        };

        this.onSearchByParameters = function(event, data) {
            var filtersNode = this.select('filtersSelector').find('.content')
            event.stopPropagation();
            if ($(event.target).is(filtersNode)) return;
            filtersNode.trigger(event.type, data);
        };

        this.render = function() {
            this.$node.html(template({}));

            this.hideSearchResults();

            this.on('filtersLoaded', function() {
                this.trigger('searchtypeloaded', { type: this.attr.searchType });
            });

            var filters = this.select('filtersSelector');
            Filters.attachTo(filters.find('.content'), {
                supportsHistogram: this.attr.supportsHistogram === true,
                supportsSorting: this.attr.supportsSorting !== false,
                searchType: this.attr.searchType,
                match: this.searchOptions && this.searchOptions.match || 'vertex'
            });
        };

        this.hideAndClearSearchResults = function() {
            return this.hideSearchResults(true);
        };

        this.hideSearchResults = function(clear) {
            this.select('resultsSelector')
                .hide();
            if (clear) {
                this.select('resultsContainerSelector')
                    .teardownAllComponents()
                    .empty();
            }
            this.trigger(document, 'paneResized');
        };

        this.showSearchResults = function() {
            this.select('resultsSelector')
                .show();
            this.trigger(document, 'paneResized');
        };

        this.makeResizable = function(node) {
            var self = this;

            // Add splitbar to search results
            return node.resizable({
                handles: 'e',
                minWidth: 200,
                maxWidth: 350,
                resize: function() {
                    self.trigger(document, 'paneResized');
                }
            });
        };

    }
});

define([
    'flight/lib/component',
    'util/withDataRequest',
    'util/element/list',
    'util/popovers/withPopover'
], function(
    defineComponent,
    withDataRequest,
    ElementList,
    withPopover) {
    'use strict';

    return defineComponent(SearchResultsPopover, withPopover, withDataRequest);

    function SearchResultsPopover() {

        this.defaultAttrs({
            openSearchSelector: '.open-in-search'
        });

        this.before('initialize', function(node, config) {
            config.template = '/dashboard/reportRenderers/resultsPopoverTpl';
            config.hideDialog = true;
        });

        this.after('initialize', function() {
            var self = this;

            this.after('setupWithTemplate', function() {
                var $content = this.popover.find('.popover-content'),
                    $list = $content.find('.list'),
                    position = function() {
                        self.dialog.show();
                        self.positionDialog();
                    },
                    error = function(message, isError) {
                        $('<div>')
                            .text(message)
                            .addClass('message')
                            .appendTo($list.empty());
                    },
                    finishedRender = this.dataRequest('dashboard', 'postData', this.attr.searchUrl || '/element/search', this.attr.search || {})
                        .then(function(results) {
                            if (_.isEmpty(results.elements)) {
                                $list.removeClass('loading-small-animate');
                                error(i18n('dashboard.search.results.none'));
                            } else {
                                return new Promise(function(f) {
                                    self.on(self.popover, 'renderFinished', function finished() {
                                        self.off('renderFinished', finished);
                                        $list.removeClass('loading-small-animate');
                                        f();
                                    });
                                    ElementList.attachTo($list.empty(), {
                                        items: results.elements
                                    });
                                });
                            }
                        })
                        .catch(function() {
                            $list.removeClass('loading-small-animate');
                            error(i18n('dashboard.search.results.error'));
                        })
                        .then(position);

                this.on(this.popover, 'click', {
                    openSearchSelector: this.onOpenInSearch
                })

                Promise.timeout(100).then(position);
            });
        });

        this.onOpenInSearch = function(event) {
            this.trigger('searchByParameters', {
                url: this.attr.searchUrl,
                parameters: this.attr.search
            });
            this.teardown();
        };
    }
});

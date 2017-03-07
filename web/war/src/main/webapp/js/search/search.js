define([
    'require',
    'flight/lib/component',
    './searchTpl.hbs',
    'tpl!util/alert',
    'util/withDataRequest',
    'util/formatters',
    'util/popovers/withElementScrollingPositionUpdates',
    'configuration/plugins/registry',
    'd3',
    './extensionToolbarPopover'
], function(
    require,
    defineComponent,
    template,
    alertTemplate,
    withDataRequest,
    F,
    withElementScrollingPositionUpdates,
    registry,
    d3,
    SearchToolbarExtensionPopover) {
    'use strict';

    var SEARCH_TYPES = ['Visallo'];

    /**
     * Search toolbar items display below the search query input field.
     *
     * They have access to the current search query (if available),
     * and can react to click events with content in a popover, or a custom event.
     * @param {string} tooltip What to display in `title` attribute
     * @param {string} icon URL path to icon to display. Use monochromatic icon with black as color. Opacity is adjusted automatically, and modified on hover.
     * @param {object} action Action to take place on click events. Must include `type` key set to one of:
     * @param {string} action.type Type of action event to occur: `popover` or `event`
     * @param {string} [action.componentPath] Required when `type='popover'`
     * Path to {@link org.visallo.search.toolbar~Component}
     * @param {string} [action.name] Required when `type='event'`, name of
     * event to fire
     *
     *     // action: { type: 'event', name: 'MyCustomEventName' }
     *     $(document).on('myCustomEventName', function(event, data) {
     *         // data.extension
     *         // data.currentSearch
     *     })
     * @param {org.visallo.search.toolbar~canHandle} [canHandle] Set a function to determine if the toolbar icon should be displayed.
     * @param {org.visallo.search.toolbar~onElementCreated} [onElementCreated] Will be called after icon ImageElement is created.
     * @param {org.visallo.search.toolbar~onClick} [onClick] Function will be called during click event, before the `action` behavior
     */
    registry.documentExtensionPoint('org.visallo.search.toolbar',
        'Add toolbar icons under search query box',
        function(e) {
            return (!e.canHandle || _.isFunction(e.canHandle)) &&
                (!e.onElementCreated || _.isFunction(e.onElementCreated)) &&
                (!e.onClick || _.isFunction(e.onClick)) &&
                _.isString(e.tooltip) &&
                _.isString(e.icon) &&
                e.action && (
                    (e.action.type === 'popover' && e.action.componentPath) ||
                    (e.action.type === 'event' && e.action.name)
                )
        },
        'http://docs.visallo.org/extension-points/front-end/searchToolbar'
    );

    /**
     * Alternate search interfaces to replace the content in the search pane.
     *
     * Each of the search interfaces has its own saved searches.
     *
     * @param {string} componentPath Path to Flight {@link org.visallo.search.advanced~Component}
     * @param {string} displayName The text to display in search dropdown (to
     * select the type of search interface)
     * @param {string} savedSearchUrl The endpoint to execute when search is changed
     */
    registry.documentExtensionPoint('org.visallo.search.advanced',
        'Add alternate search interfaces',
        function(e) {
            return (e.componentPath && e.displayName && e.savedSearchUrl);
        },
        'http://docs.visallo.org/extension-points/front-end/searchAdvanced'
    );

    /**
     * @undocumented
     */
    registry.documentExtensionPoint('org.visallo.search.filter',
        'Add new types of search filters',
        function(e) {
            return ('searchType' in e) &&
                ('componentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/searchFilters'
    );

    var SavedSearchPopover;

    return defineComponent(Search, withDataRequest, withElementScrollingPositionUpdates);

    function Search() {

        this.savedQueries = _.indexBy(SEARCH_TYPES.map(function(type) {
            return {
                type: type,
                query: '',
                filters: []
            }
        }), 'type');

        this.defaultAttrs({
            formSelector: '.navbar-search',
            querySelector: '.navbar-search .search-query',
            queryValidationSelector: '.search-query-validation',
            queryExtensionsSelector: '.below-query .extensions',
            queryExtensionsButtonSelector: '.below-query .extensions > button',
            hitsSelector: '.search-hits',
            advancedSearchDropdownSelector: '.search-dropdowns .advanced',
            advancedSearchTypeSelector: '.search-dropdowns .advanced-dropdown li a',
            savedSearchSelector: '.search-dropdowns .saved',
            queryContainerSelector: '.search-query-container',
            clearSearchSelector: '.search-query-container a',
            segmentedControlSelector: '.segmented-control',
            filtersInfoSelector: '.filter-info',
            searchTypeSelector: '.search-type'
        });

        this.after('initialize', function() {
            this.render();

            this.currentSearchByUrl = {};
            this.currentSearchUrl = '';

            this.triggerQueryUpdatedThrottled = _.throttle(this.triggerQueryUpdated.bind(this), 100);
            this.triggerQueryUpdated = _.debounce(this.triggerQueryUpdated.bind(this), 500);

            this.on('click', {
                segmentedControlSelector: this.onSegmentedControlsClick,
                clearSearchSelector: this.onClearSearchClick,
                advancedSearchTypeSelector: this.onAdvancedSearchTypeClick,
                savedSearchSelector: this.onSavedSearch,
                queryExtensionsButtonSelector: this.onExtensionToolbarClick
            });
            this.on(this.select('advancedSearchDropdownSelector'), 'click', this.onAdvancedSearchDropdown);
            this.on('change keydown keyup paste', {
                querySelector: this.onQueryChange
            });
            this.on(this.select('querySelector'), 'focus', this.onQueryFocus);

            this.on('savedQuerySelected', this.onSavedQuerySelected);
            this.on('setCurrentSearchForSaving', this.onSetCurrentSearchForSaving);
            this.on('filterschange', this.onFiltersChange);
            this.on('clearSearch', this.onClearSearch);
            this.on('searchRequestBegan', this.onSearchResultsBegan);
            this.on('searchRequestCompleted', this.onSearchResultsCompleted);
            this.on('searchtypeloaded', this.onSearchTypeLoaded);
            this.on(document, 'searchByParameters', this.onSearchByParameters);
            this.on(document, 'searchForPhrase', this.onSearchForPhrase);
            this.on(document, 'searchByRelatedEntity', this.onSearchByRelatedEntity);
            this.on(document, 'searchByProperty', this.onSearchByProperty);
            this.on(document, 'searchPaneVisible', this.onSearchPaneVisible);
            this.on(document, 'switchSearchType', this.onSwitchSearchType);
            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
        });

        /**
         * Fired when user selects a saved search.
         * {@link org.visallo.search.advanced~Component|AdvancedSearch}
         * components should listen and load the search
         *
         * @event org.visallo.search.advanced#savedQuerySelected
         * @property {object} data
         * @property {object} data.query
         * @property {object} data.query.url The search endpoint
         * @property {object} data.query.parameters The search endpoint
         * parameters
         */
        this.onSavedQuerySelected = function(event, data) {
            if ($(event.target).is(this.currentSearchNode)) return;
            this.trigger(
                'searchByParameters',
                {
                    ...data.query,
                    submit: true
                }
            );
        };

        this.onSearchByParameters = function(event, data) {
            var self = this;

            if ($(event.target).is(this.currentSearchNode)) return;

            this.currentSearchQuery = data;
            this.currentSearchByUrl[data.url] = data;

            var advancedSearch = data.url &&
                _.findWhere(registry.extensionsForPoint('org.visallo.search.advanced'), { savedSearchUrl: data.url })

            if (!data.url) data.url = '/vertex/search';
            if (!data.parameters) data.parameters = {};
            if (!data.parameters.filter) data.parameters.filter = '[]';

            this.openSearchType(advancedSearch ?
                { ...advancedSearch, advancedSearch: advancedSearch.componentPath } :
                'Visallo'
            ).then(function() {
                if (advancedSearch) {
                    self.currentSearchNode.trigger('savedQuerySelected', { query: data });
                } else {
                    var node = self.getSearchTypeNode().find('.search-filters > .content');
                    if ('q' in data.parameters) {
                        self.select('querySelector').filter(':visible')
                            .val(data.parameters.q)
                            .select()
                        self.updateClearSearch();
                    }
                    node.trigger(event.type, data);
                }
            });
        };

        /**
         * @event org.visallo.search.advanced#setCurrentSearchForSaving
         * @property {object} data
         * @property {string} data.url The endpoint url (should match extension
         * point savedSearchUrl)
         * @property {object} data.parameters The paramters to send to endpoint
         */
        this.onSetCurrentSearchForSaving = function(event, data) {
            if ($(event.target).is(this.attr.savedSearchSelector)) return;

            if (data && data.url) {
                var visalloFilter = /^\/(?:vertex|element|edge)\/search$/;
                this.currentSearchByUrl[data.url] = data;
                if (visalloFilter.test(data.url)) {
                    this.currentSearchByUrl['/vertex/search'] = data;
                }
                this.currentSearch = data;
            } else {
                this.currentSearchByUrl = {};
                this.currentSearch = null;
            }
            this.select('savedSearchSelector').trigger(event.type, this.currentSearchByUrl[this.currentSearchUrl]);
            this.updateQueryToolbarExtensions();
        };

        this.updateQueryToolbarExtensions = function() {
            var self = this,
                extensions = registry.extensionsForPoint('org.visallo.search.toolbar');
            if (!this.toolbarExtensionsById) {
                var inc = 0,
                    mapped = extensions.map(function(e) {
                        e.identifier = inc++;
                        return e;
                    });
                this.toolbarExtensionsById = _.indexBy(mapped, 'identifier')
            }

            var $container = this.select('queryExtensionsSelector'),
                items = _.filter(extensions, function(e) {
                    /**
                     * @callback org.visallo.search.toolbar~canHandle
                     * @param {object} [currentSearch] Could be null if
                     * current search is invalid
                     * @param {object} currentSearch.url
                     * @param {object} currentSearch.parameters
                     * @returns {boolean} If the item can handle the search
                     */
                    return !_.isFunction(e.canHandle) || e.canHandle(self.currentSearch);
                });

            d3.select($container.get(0))
                .selectAll('button')
                .data(items)
                .call(function() {
                    this.enter().append('button')
                        .each(function(e) {
                            if (_.isFunction(e.onElementCreated)) {
                                /**
                                 * @callback org.visallo.search.toolbar~onElementCreated
                                 * @param {Element} element The dom element that the
                                 * toolbar item is in
                                 */
                                e.onElementCreated(this)
                            }
                        })
                        .attr('title', _.property('tooltip'))
                        .style('background-image', function(e) {
                            return 'url(' + e.icon + ')'
                        })
                    this.order()
                    this.exit().remove();
                })
        };

        this.onExtensionToolbarClick = function(event) {
            var self = this,
                $toolbarButton = $(event.target),
                extension = d3.select(event.target).datum();

            event.preventDefault();
            if (extension) {
                if (_.isFunction(extension.onClick)) {
                    /**
                     * @callback org.visallo.search.toolbar~onClick
                     * @param {Event} event The click event
                     * @returns {boolean} If false the `action` defined in the
                     * extension won't execute.
                     */
                    if (extension.onClick(event) === false) {
                        event.stopPropagation();
                        return;
                    }
                }
                switch (extension.action && extension.action.type || '') {
                  case 'popover':
                    if ($toolbarButton.lookupComponent(SearchToolbarExtensionPopover)) {
                        _.defer(function() {
                            $toolbarButton.teardownAllComponents();
                        });
                        return;
                    }
                    Promise.require(extension.action.componentPath)
                        .then(function(Component) {
                            SearchToolbarExtensionPopover.attachTo($toolbarButton, {
                                Component: Component,
                                model: {
                                    search: self.currentSearch
                                },
                                extension: extension
                            })
                        });
                    break;

                  case 'event':
                    $toolbarButton.trigger(extension.action.name, {
                        extension: extension,
                        currentSearch: this.currentSearch
                    });
                    break;

                  default:
                      throw new Error('Unknown action for toolbar item extension: ' + JSON.stringify(extension));
                }
            }
        };

        this.onToggleDisplay = function(event, data) {
            if (data.name === 'search' && this.$node.closest('.visible').length === 0) {
                this.$node.find('.advanced-search-type-results').hide();
            }
            if (this.searchType && this.searchType === SEARCH_TYPES[0]) {
                this.select('hitsSelector').empty();
            }
        };

        this.onSearchTypeLoaded = function() {
            this.trigger('paneResized');
            this.updateQueryToolbarExtensions();
        };

        this.openSearchType = function(searchType) {
            var self = this,
                d = $.Deferred();

            new Promise(function(fulfill, reject) {
                if (self.$node.closest('.visible').length === 0) {
                    self.searchType = null;
                    self.on(document, 'searchPaneVisible', function handler(data) {
                        self.off(document, 'searchPaneVisible', handler);
                        fulfill();
                    })
                    self.trigger(document, 'menubarToggleDisplay', { name: 'search' });
                } else fulfill();
            }).done(function() {
                if (self.searchType === searchType) {
                    d.resolve();
                } else {
                    self.on('searchtypeloaded', function loadedHandler() {
                        self.off('searchtypeloaded', loadedHandler);
                        d.resolve();
                    });
                }
                self.switchSearchType(searchType);
            });

            return d;
        };

        this.onSearchForPhrase = function(event, data) {
            this.trigger('searchByParameters', {
                submit: true,
                parameters: {
                    q: '"' + data.query.replace(/"/g, '\\"') + '"'
                }
            });
        };

        this.onSearchByProperty = function(event, data) {
            var self = this;

            this.openSearchType('Visallo')
                .done(function() {
                    var node = self.getSearchTypeNode().find('.search-filters .content');
                    self.select('querySelector').val('');
                    self.trigger(node, 'searchByProperty', data);
                })
        };

        this.onSearchByRelatedEntity = function(event, data) {
            var self = this;

            this.openSearchType('Visallo')
                .done(function() {
                    var node = self.getSearchTypeNode().find('.search-filters .content');
                    self.select('querySelector').val('');
                    self.trigger(node, 'searchByRelatedEntity', data);
                });
        };

        this.onSearchPaneVisible = function(event, data) {
            var self = this;

            _.delay(function() {
                self.select('querySelector').focus();
            }, 250);
        };

        this.onSearchResultsBegan = function() {
            this.select('queryContainerSelector').addClass('loading');
        };

        this.onSearchResultsCompleted = function(event, data) {
            this.select('queryContainerSelector').removeClass('loading');
            this.updateQueryError(data);
        };

        this.updateQueryError = function(data) {
            var $error = this.select('queryValidationSelector'),
                $hits = this.select('hitsSelector');

            this.$node.toggleClass('hasError', !!(data && !data.success));

            if (!data || data.success) {
                $error.empty();

                if (data && data.message) {
                    $hits.text(data.message);
                } else $hits.empty();
            } else {
                $hits.empty();
                $error.html(
                    alertTemplate({ error: data.error || i18n('search.query.error') })
                )
            }

            this.updateTypeCss();
        };

        this.onFiltersChange = function(event, data) {
            var self = this,
                hadFilters = this.hasFilters();

            this.filters = data;

            var query = this.getQueryVal(),
                options = (data && data.options) || {},
                hasFilters = this.hasFilters();

            this.dataRequest('config', 'properties')
                .done(function(properties) {
                    if (!query && hasFilters && data.setAsteriskSearchOnEmpty) {
                        if (properties['search.disableWildcardSearch'] === 'true') {
                            self.updateClearSearch();
                            return;
                        } else {
                            self.select('querySelector').val('*');
                            query = self.getQueryVal();
                        }
                    }

                    const hasQuery = query && query.length;
                    const validSearch = hasQuery && (hasFilters || hadFilters || options.matchChanged);

                    if (options.isScrubbing) {
                        self.triggerQueryUpdatedThrottled();
                    } else {
                        self.triggerQueryUpdated();
                    }
                    if (validSearch || options.submit) {
                        self.triggerQuerySubmit();
                    }

                    self.updateClearSearch();
                });
        };

        this.onQueryChange = function(event) {
            if (event.which === $.ui.keyCode.ENTER) {
                if (event.type === 'keyup') {
                    if (this.select('querySelector').val().length) {
                        this.triggerQuerySubmit();
                        $(event.target).select()
                    }
                }
            } else if (event.which === $.ui.keyCode.ESCAPE) {
                if (event.type === 'keyup') {
                    if (this.canClearSearch) {
                        this.onClearSearchClick();
                    } else {
                        this.select('querySelector').blur();
                    }
                }
            } else if (event.keyCode === 191 && !event.shiftKey /* FORWARD SLASH */) {
                event.preventDefault();
                event.stopPropagation();
                if (event.type === 'keyup') {
                    this.switchSearchType(this.otherSearchType);
                }
            } else {
                this.updateClearSearch();
                this.triggerQueryUpdated();
            }
        };

        this.onClearSearchClick = function(event) {
            var node = this.getSearchTypeNode(),
                $query = this.select('querySelector'),
                $clear = this.select('clearSearchSelector');

            $clear.hide();
            _.defer($query.focus.bind($query));
            this.trigger(node, 'clearSearch', { clearMatch: false })
        };

        this.onAdvancedSearchTypeClick = function(event) {
            var $target = $(event.target),
                path = $target.data('componentPath'),
                savedSearchUrl = $target.data('savedSearchUrl');

            this.switchSearchType({
                advancedSearch: path,
                displayName: $target.text(),
                savedSearchUrl: savedSearchUrl
            });
        };

        this.onAdvancedSearchDropdown = function() {
            this.select('savedSearchSelector').teardownAllComponents();
        };

        this.onSavedSearch = function(event) {
            var self = this,
                $button = $(event.target).closest('button'),
                $advancedButton = this.$node.find('.search-dropdowns .advanced');

            event.stopPropagation();

            if ($advancedButton.next('.dropdown-menu').is(':visible')) {
                $advancedButton.dropdown('toggle');
            }

            var opened = SavedSearchPopover && !!$button.lookupComponent(SavedSearchPopover);
            if (opened) {
                $button.siblings('button').andSelf().teardownAllComponents();
            } else {
                $button.addClass('loading');
                require(['./save/popover'], function(Save) {
                    SavedSearchPopover = Save;
                    $button.removeClass('loading')
                    self.dataRequest('search', 'all', self.currentSearchUrl).done(function(searches) {
                        if (self.currentSearchQuery) {
                            self.currentSearchQuery = _.findWhere(searches, { id: self.currentSearchQuery.id });
                        }
                        Save.attachTo($button, {
                            list: searches,
                            update: self.currentSearchQuery,
                            query: self.currentSearchByUrl[self.currentSearchUrl]
                        });
                    })
                });
            }
        };

        this.onClearSearch = function(event) {
            var node = this.getSearchTypeNode(),
                $query = this.select('querySelector'),
                $clear = this.select('clearSearchSelector');

            this.currentSearchQuery = null;

            if (node.is(event.target)) {
                this.select('queryContainerSelector').removeClass('loading');
                if (this.getQueryVal()) {
                    this.setQueryVal('');
                }
                this.filters = null;
                this.updateQueryError();
                this.triggerQueryUpdated();
            } else {
                this.savedQueries[this.otherSearchType].query = '';
                this.savedQueries[this.otherSearchType].filters = [];
            }
        };

        this.onSegmentedControlsClick = function(event, data) {
            event.stopPropagation();

            this.switchSearchType(
                $(event.target).blur().data('type')
            );
            this.select('querySelector').focus();
        };

        this.onQueryFocus = function(event) {
            this.switchSearchType(this.searchType || SEARCH_TYPES[0]);
        };

        this.onSwitchSearchType = function(event, data) {
            if (data !== 'Visallo' && data !== 'Workspace' &&
               !_.isObject(data) && !data.advancedSearch) {
                throw new Error('Only Visallo/Workspace types supported');
            }
            this.switchSearchType(data);
        };

        this.switchSearchType = function(newSearchType) {
            var self = this,
                advanced = !_.isString(newSearchType);

            if (advanced) {
                var path = newSearchType.advancedSearch,
                    previousSearchType = this.searchType;

                if (!path) {
                    this.searchType = null;
                    this.switchSearchType(previousSearchType);
                    return;
                }

                this.advancedActive = true;
                this.updateAdvancedSearchDropdown(newSearchType);

                var cls = F.className.to(path),
                    $container = this.$node.find('.advanced-search-type.' + cls),
                    attach = false;

                if (!$container.length) {
                    attach = true;
                    $container = $('<div>')
                        .addClass('advanced-search-type ' + cls)
                        .appendTo(this.node)
                    $('<div>')
                        .addClass('advanced-search-type-results ' + cls)
                        .data('width-preference', path)
                        .html('<div class="content">')
                        .appendTo(this.node)
                        .resizable({
                            handles: 'e',
                            minWidth: 200,
                            maxWidth: 350,
                            resize: function() {
                                self.trigger(document, 'paneResized');
                            }
                        }).hide();
                }

                this.$node.find('.search-type.active').removeClass('active');
                this.$node.find('.search-query-container').hide();
                $container.show();

                if (attach) {
                    require([path], function(AdvancedSearchExtension) {
                        /**
                         * Responsible for displaying the interface for
                         * searching, and displaying the results.
                         *
                         * @typedef org.visallo.search.advanced~Component
                         * @property {string} resultsSelector Css selector to
                         * container that will hold results
                         * @see module:components/List
                         * @listens org.visallo.search.advanced#savedQuerySelected
                         * @fires org.visallo.search.advanced#setCurrentSearchForSaving
                         * @example <caption>Rendering results</caption>
                         * List.attachTo($(this.attr.resultsSelector), {
                         *     items: results
                         * })
                         */
                        AdvancedSearchExtension.attachTo($container, {
                            resultsSelector: '.search-pane .' + cls
                        });
                        self.trigger($container, 'searchtypeloaded', { type: newSearchType });
                    })
                } else {
                    self.trigger($container, 'searchtypeloaded', { type: newSearchType });
                }

                self.currentSearchUrl = newSearchType.savedSearchUrl
                self.currentSearchNode = $container;

                return;
            }

            this.$node.find('.advanced-search-type, .advanced-search-type-results').hide();
            this.$node.find('.search-query-container').show();

            if (!this.advancedActive && (!newSearchType || this.searchType === newSearchType)) {
                return;
            }

            this.advancedActive = false;

            this.updateQueryValue(newSearchType);

            this.updateAdvancedSearchDropdown(newSearchType);

            var segmentedButton = this.$node.find('.find-' + newSearchType.toLowerCase())
                    .addClass('active')
                    .siblings('button').removeClass('active').end(),
                node = this.getSearchTypeNode()
                    .addClass('active')
                    .siblings('.search-type').removeClass('active').end();

            require(['./types/type' + newSearchType], function(SearchType) {
                self.currentSearchUrl = SearchType.savedSearchUrl;
                var alreadyAttached = node.lookupComponent(SearchType);
                if (alreadyAttached) {
                    self.trigger(node, 'searchtypeloaded', { type: newSearchType });
                } else {
                    SearchType.attachTo(node);
                }
                self.currentSearchNode = node;
                self.updateTypeCss();
            });
        };

        this.updateAdvancedSearchDropdown = function(newSearchType) {
            var dropdownCaret = this.$node.find('.search-dropdowns .advanced .caret')[0];

            if (dropdownCaret) {
                dropdownCaret.previousSibling.textContent = (
                    _.isObject(newSearchType) && newSearchType.displayName ?
                        newSearchType.displayName :
                        i18n('search.advanced.default')
                ) + ' ';
            }
            this.$node.find('.search-dropdowns').toggle(_.isObject(newSearchType) || newSearchType === 'Visallo');
        }

        this.updateTypeCss = function() {
            this.$node.find('.search-type .search-filters').css(
                'top',
                this.select('formSelector').outerHeight(true)
            );
        };

        this.updateClearSearch = function() {
            this.canClearSearch = this.getQueryVal().length > 0 || this.hasFilters();
            this.select('clearSearchSelector').toggle(this.canClearSearch);
        };

        this.hasFilters = function() {
            return !!(this.filters && this.filters.hasSome);
        };

        this.updateQueryValue = function(newSearchType) {
            var $query = this.select('querySelector'),
                $hits = this.select('hitsSelector');

            if (this.searchType) {
                this.savedQueries[this.searchType].query = $query.val();
                this.savedQueries[this.searchType].hits = $hits.text().trim();
            }
            this.searchType = newSearchType;
            this.otherSearchType = _.without(SEARCH_TYPES, this.searchType)[0];

            $query.val(this.savedQueries[newSearchType].query);
            if (this.savedQueries[newSearchType].hits) {
                $hits.text(this.savedQueries[newSearchType].hits);
            } else $hits.empty();

            this.updateClearSearch();
        };

        this.triggerOnType = function(eventName) {
            var searchType = this.getSearchTypeNode();
            this.trigger(searchType, eventName, {
                value: this.getQueryVal(),
                filters: this.filters || {}
            });
        };

        this.triggerQuerySubmit = _.partial(this.triggerOnType, 'querysubmit');

        this.triggerQueryUpdated = _.partial(this.triggerOnType, 'queryupdated');

        this.getQueryVal = function() {
            return $.trim(this.select('querySelector').val());
        };

        this.setQueryVal = function(val) {
            return this.select('querySelector').val(val).change();
        };

        this.getSearchTypeNode = function() {
            return this.$node.find('.search-type-' + this.searchType.toLowerCase());
        };

        this.render = function() {
            var self = this,
                advancedSearch = registry.extensionsForPoint('org.visallo.search.advanced');

            this.$node.html(template({
                advancedSearch: advancedSearch,
                types: SEARCH_TYPES.map(function(type, i) {
                    return {
                        cls: type.toLowerCase(),
                        name: type,
                        displayName: {
                            Visallo: i18n('search.types.visallo'),
                            Workspace: i18n('search.types.workspace')
                        }[type],
                        selected: i === 0
                    }
                })
            }));
        };
    }
});

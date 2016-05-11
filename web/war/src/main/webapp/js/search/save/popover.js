
define([
    'flight/lib/component',
    'util/popovers/withPopover',
    'util/withDataRequest',
    'util/privileges'
], function(
    defineComponent,
    withPopover,
    withDataRequest,
    Privileges) {
    'use strict';

    var SCOPES = {
        GLOBAL: 'Global'
    };

    return defineComponent(SavedSearches, withPopover, withDataRequest);

    function normalizeParameters(params) {
        return _.chain(params)
            .map(function(value, key) {
                return [
                    key.replace(/\[\]$/, ''),
                    value
                ];
            })
            .object()
            .value();
    }

    function queryParametersChanged(param1, param2) {
        return !_.isEqual(
            normalizeParameters(param1),
            normalizeParameters(param2)
        );
    }

    function SavedSearches() {

        this.defaultAttrs({
            listSelector: 'li a',
            saveSelector: '.form button',
            nameInputSelector: '.form input.name',
            globalSearchSelector: '.form .global-search',
            globalInputSelector: '.form .global-search input',
            deleteSelector: 'ul .btn-danger'
        })

        this.before('initialize', function(node, config) {
            config.template = '/search/save/template';
            config.canSaveGlobal = Privileges.canADMIN;
            config.maxHeight = $(window).height() / 2;
            config.name = config.update && config.update.name || '';
            config.updatingGlobal = config.update && config.update.scope === SCOPES.GLOBAL;
            config.text = i18n('search.savedsearches.button.' + (config.update ? 'update' : 'create'));
            config.teardownOnTap = true;
            config.list = config.list.map(function(item) {
                var isGlobal = item.scope === SCOPES.GLOBAL,
                    canDelete = true;
                if (isGlobal) {
                    canDelete = Privileges.canADMIN;
                }
                return _.extend({}, item, {
                    isGlobal: isGlobal,
                    canDelete: canDelete
                })
            })

            this.after('setupWithTemplate', function() {
                this.on(this.popover, 'click', {
                    listSelector: this.onClick,
                    saveSelector: this.onSave,
                    deleteSelector: this.onDelete
                })

                this.on(this.popover, 'keyup change', {
                    nameInputSelector: this.onChange,
                    globalInputSelector: this.onChange
                })

                this.validate();
                this.positionDialog();
            });
        });

        this.after('initialize', function() {
            this.on('setCurrentSearchForSaving', this.onSetCurrentSearchForSaving);
        });

        this.onSetCurrentSearchForSaving = function(event, data) {
            this.attr.query = data;
            this.validate();
        };

        this.onChange = function(event) {
            if (this.attr.update) {
                var $button = this.popover.find('.form button'),
                    query = this.getQueryForSaving();

                $button.text(
                    ('id' in query) ?
                    'Update' : 'Create'
                );
            }

            if (this.validate() && event.type === 'keyup' && event.which === 13) {
                this.save();
            }
        };

        this.validate = function() {
            var $input = this.popover.find(this.attr.nameInputSelector),
                $button = this.popover.find(this.attr.saveSelector),
                $global = this.popover.find(this.attr.globalSearchSelector),
                query = this.getQueryForSaving(),
                noParameters = _.isEmpty(query.parameters);

            $button.prop('disabled', !query.name || noParameters);
            $input.prop('disabled', noParameters || !query.url);
            $global.toggle(!(noParameters || !query.url));

            return query.name && query.url && !noParameters;
        };

        this.getQueryForSaving = function() {
            var $nameInput = this.popover.find(this.attr.nameInputSelector),
                $globalInput = this.popover.find(this.attr.globalInputSelector),
                query = {
                    name: $nameInput.val().trim(),
                    global: $globalInput.is(':checked'),
                    url: this.attr.query && this.attr.query.url,
                    parameters: this.attr.query && this.attr.query.parameters
                };

            if (this.attr.update && query.parameters) {
                var nameChanged = this.attr.update.name !== query.name,
                    queryChanged = queryParametersChanged(this.attr.update.parameters, query.parameters);

                if ((nameChanged && !queryChanged) || (!nameChanged && queryChanged) ||
                   (!nameChanged && !queryChanged)) {
                    query.id = this.attr.update.id;
                }
            }

            return query;
        };

        this.onSave = function(event) {
            this.save();
        };

        this.save = function() {
            var self = this,
                $button = this.popover.find(this.attr.saveSelector).addClass('loading'),
                query = this.getQueryForSaving();

            this.dataRequest('search', 'save', query)
                .then(function() {
                    self.teardown();
                })
                .finally(function() {
                    $button.removeClass('loading');
                })
        };

        this.onDelete = function(event) {
            var self = this,
                $li = $(event.target).closest('li'),
                index = $li.index(),
                query = this.attr.list[index],
                $button = $(event.target).addClass('loading');

            $li.addClass('loading');

            this.dataRequest('search', 'delete', query.id)
                .then(function() {
                    if ($li.siblings().length === 0) {
                        $li.closest('ul').html(
                            $('<li class="empty">No Saved Searches Found</li>')
                        );
                    } else $li.remove();

                    self.attr.list.splice(index, 1);

                    if (self.attr.update && self.attr.update.id === query.id) {
                        self.popover.find(self.attr.nameInputSelector).val('');
                        self.popover.find(self.attr.saveSelector).text('Create');
                        self.attr.update = null;
                        self.validate();
                    }
                })
                .finally(function() {
                    $button.removeClass('loading');
                    $li.removeClass('loading');
                })
        };

        this.onClick = function(event) {
            var query = this.attr.list[$(event.target).closest('li').index()];

            this.trigger('savedQuerySelected', {
                query: query
            });

            this.teardown();
        };
    }
});

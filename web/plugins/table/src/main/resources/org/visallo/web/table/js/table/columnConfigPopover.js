define([
    'public/v1/api',
    'util/popovers/withPopover'
], function(api, withPopover) {
    'use strict';

    return api.defineComponent(ColumnConfigPopover, withPopover);

    function ColumnConfigPopover() {

        this.defaultAttrs({
            inputSelector: '.popover-content input',
            selectAllSelector: '.select-all',
            rowNumbersSelector: '.row-numbers',
            sortConfigSelector: '.sort-property-select',
            columns: null,
            showRowNumbers: null,
            sortConfig: null,
            template: '/org/visallo/web/table/hbs/columnConfigPopover'
        });

        this.after('initialize', function() {
            this.selectedColumnIris = _.chain(this.attr.columns)
                .filter(({ visible }) => visible)
                .map(({ title }) => title)
                .value();

            this.after('setupWithTemplate', function() {
                this.updateSelectAllCheckbox();
                this.on(this.popover, 'change', {
                    inputSelector: this.onChange,
                    selectAllSelector: this.onSelectAllChange,
                    rowNumbersSelector: this.onRowNumbersChange,
                    sortConfigSelector: this.onSortConfigChange
                });

                this.popover.find('ul')
                    .sortable({
                        axis: 'y',
                        cursor: 'move',
                        tolerance: 'pointer',
                        containment: 'parent'
                    })
                    .on('sortupdate', this.onChange.bind(this));
            });
        });

        this.onSelectAllChange = function(event) {
            var target = $(event.target),
                checked = target.is(':checked');

            $(this.attr.inputSelector, this.popover).prop('checked', checked).trigger('change');
        };

        this.onRowNumbersChange = function(event) {
            var target = $(event.target),
                checked = target.is(':checked');

            this.trigger('columnsConfigured', {
                showRowNumbers: checked
            });
        };

        this.onSortConfigChange = function(event) {
            var $target = $(event.target),
                value = $target.val();

            this.trigger('sortConfigured', { sortColumn: this.attr.sortConfig.property, propertyName: value });
        };

        this.onChange = function() {
            this.selectedColumnIris = _.unique(this.popover.find('ul li input:checked')
                .map(function() {
                   return $(this).val();
                }));

            this.updateSelectAllCheckbox();

            this.trigger('columnsConfigured', {
                selectedColumns: this.selectedColumnIris
            });
        };

        this.updateSelectAllCheckbox = function() {
            if (this.selectedColumnIris.length === 0) {
                $(this.attr.selectAllSelector, this.popover).prop('checked', false).prop('indeterminate', false);
            } else if (this.selectedColumnIris.length === $(this.attr.inputSelector, this.popover).size()) {
                $(this.attr.selectAllSelector, this.popover).prop('checked', true).prop('indeterminate', false);
            } else {
                $(this.attr.selectAllSelector, this.popover).prop('checked', true).prop('indeterminate', true);
            }
        }
    }
});

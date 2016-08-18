define([
    'flight/lib/component',
    'util/ontology/propertySelect',
    'hbs!./sortTpl',
    'util/requirejs/promise!util/service/ontologyPromise',
    'd3',
    'jquery-ui'
], function(
    defineComponent,
    FieldSelection,
    template,
    ontologyPromise,
    d3) {
    'use strict';

    return defineComponent(SortByFields);

    function SortByFields() {

        this.attributes({
            sorts: [],
            restrictToConceptId: '',
            propertySelector: '.property-select',
            listSelector: 'ul.fields',
            listItemSelector: 'ul.fields li',
            removeSelector: 'ul.fields li .remove-sort'
        });

        this.after('initialize', function() {
            this.sortFields = this.attr.sorts ? this.attr.sorts.slice() : [];
            this.on('click', {
                listItemSelector: this.onItemClick,
                removeSelector: this.onRemoveItemClick
            })
            this.on('setSortFields', this.onSetSortFields);
            this.on('filterProperties', this.onFilterProperties);
            this.on('propertyselected', this.onPropertySelected);
            this.on('drag dragcreate dragstart dropcreate drop dropover dropout resizecreate resizestart resizestop sort sortstart sortend sortupdate sortreceive sortbeforestop', function(event) {
                event.stopPropagation();
            });
            this.calculateSort = _.debounce(this.calculateSort.bind(this), 250);
            this.$node.html(template({}));

            this.attachPropertyField();
            this.updateSortFields(true);
        });

        this.onFilterProperties = function(event, data) {
            if ($(event.target).is('.property-select')) return;

            if (!data || _.isEmpty(data.properties)) {
                this.filteredProperties = null;
            } else {
                this.filteredProperties = _.reject(data.properties, function(p) {
                    return p.sortable === false;
                });
            }

            this.$node.find('.property-select').trigger(event.type, {
                properties: this.filteredProperties
            });
        };

        this.onSetSortFields = function(event, data) {
            this.sortFields = data && data.sortFields || [];
            this.updateSortFields();
        };

        this.onItemClick = function(event) {
            var $li = $(event.target).closest('li'),
                index = $li.index(),
                sortField = this.sortFields[index],
                direction = sortField.direction;

            sortField.direction = direction === 'ASCENDING' ? 'DESCENDING' : 'ASCENDING';
            this.updateSortFields();
        };

        this.onRemoveItemClick = function(event) {
            event.stopPropagation();
            var self = this,
                $li = $(event.target).closest('li');

            event.target.blur();
            _.defer(function() {
                self.sortFields.splice($li.index(), 1);
                self.updateSortFields();
            })
        };

        this.attachPropertyField = function() {
            var node = this.$node.find('.property-select');
            node.teardownComponent(FieldSelection);
            FieldSelection.attachTo(node, {
                properties: _.reject(this.filteredProperties || ontologyPromise.properties.list, function(p) {
                    return p.searchable === false || p.sortable === false;
                }),
                onlySearchable: true,
                placeholder: 'Add Sort...'
            });
        };

        this.onPropertySelected = function(event, data) {
            event.stopPropagation();
            this.attachPropertyField();

            var hasSort = _.some(this.sortFields, function(sort) {
                return sort.field === data.property.title;
            });
            if (!hasSort) {
                this.sortFields.push({
                    field: data.property.title,
                    direction: 'ASCENDING'
                });
                this.updateSortFields();
            }
        };

        this.updateSortFields = function(preventTrigger) {
            var self = this,
                $list = this.select('listSelector');

            d3.select($list.get(0))
                .selectAll('li')
                .data(this.sortFields)
                .call(function() {
                    this.enter().append('li')
                        .call(function() {
                            this.append('div').call(function() {
                                this.append('a');
                                this.append('span');
                            })
                            this.append('button').attr('class', 'remove-sort remove-icon').html('&times');
                        })
                    this.exit().remove();
                })
                .order()
                .each(function(d) {
                    $(this)
                        .toggleClass('ascending', d.direction === 'ASCENDING')
                        .data('field', d.field);
                })
                .call(function() {
                    this.select('span')
                        .text(function(d) {
                            return ontologyPromise.properties.byTitle[d.field].displayName;
                        })
                        .attr('title', function(d) {
                            return ontologyPromise.properties.byTitle[d.field].displayName;
                        })
                    this.select('a').attr('title', function(d) {
                        return d.direction.toLowerCase();
                    });
                })

            $list.off('sortupdate')
                .sortable({
                    axis: 'y',
                    cursor: 'move',
                    tolerance: 'pointer',
                    containment: 'parent'
                })
                .on('sortupdate', function(event, ui) {
                    self.calculateSort();
                });

            if (preventTrigger !== true) {
                this.trigger('sortFieldsUpdated', {
                    sortFields: this.sortFields
                })
            }
        };

        this.calculateSort = function() {
            var $list = this.select('listSelector');
            this.sortFields = _.sortBy(this.sortFields, function(field) {
                var $li = $list.find('li').filter(function() {
                    return $(this).data('field') === field.field;
                })
                return $li.index();
            })
            this.updateSortFields();
        }

    }
});

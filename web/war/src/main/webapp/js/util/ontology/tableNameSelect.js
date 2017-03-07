define([
    'flight/lib/component',
    './tableNames.hbs',
    './tableName.hbs',
    'util/withDataRequest',
    'util/requirejs/promise!util/service/ontologyPromise',
    './withSelect'
], function (defineComponent,
             template,
             tableNameTemplate,
             withDataRequest,
             ontology,
             withSelect) {
    'use strict';

    return defineComponent(TableNameSelector, withDataRequest, withSelect);

    function TableNameSelector() {
        this.defaultAttrs({
            defaultText: i18n('tableName.field.placeholder'),
            fieldSelector: 'input',
            limitParentConceptId: '',
            maxItems: withSelect.maxItemsFromConfiguration('typeahead.tableNames.maxItems')
        });

        this.after('initialize', function () {
            this.$node.html(template(this.attr));

            this.on('click', {
                fieldSelector: this.showTypeahead
            });
            this.on('limitParentConceptId', this.onLimitParentConceptId);
            this.on('selectTableIri', this.onSetTableIri);

            this.setupTypeahead();
        });

        this.onSetTableIri = function (event, data) {
            const tableProperty = data && data.tablePropertyIri && ontology.properties.byTitle[data.tablePropertyIri];
            this.select('fieldSelector').val(tableProperty && tableProperty.displayName || '');
        };

        this.showTypeahead = function () {
            this.select('fieldSelector').typeahead('lookup').select();
        };

        this.onLimitParentConceptId = function (event, data) {
            this.attr.limitParentConceptId = data.conceptId;
            this.transformTableProperties();
        };

        this.setupTypeahead = function () {
            const self = this;

            const propertyOntology = ontology.properties;
            const transformed = self.transformTableProperties();
            const placeholderForTableNames = function () {
                return transformed.length ?
                    self.attr.defaultText :
                    i18n('tableName.field.no_valid');
            };
            const isPlaceholder = function (placeholder) {
                return placeholder === self.attr.defaultText ||
                    placeholder === i18n('tableName.field.no_valid');
            };
            const placeholder = placeholderForTableNames(transformed);
            const field = self.select('fieldSelector').attr('placeholder', placeholder)

            field.typeahead({
                minLength: 0,
                items: self.attr.maxItems,
                source: function (query) {
                    const properties = self.transformTableProperties();
                    const placeholder = placeholderForTableNames(properties);

                    properties.splice(0, 0, placeholder);

                    return properties;
                },
                matcher: function (property) {
                    if ($.trim(this.query) === '') {
                        return true;
                    }
                    if (isPlaceholder(property)) {
                        return false;
                    }

                    return Object.getPrototypeOf(this).matcher.call(this, property.displayName);
                },
                sorter: _.identity,
                updater: function (propertyTitle) {
                    const property = propertyOntology.byTitle[propertyTitle];

                    self.currentPropertyTitle = property && property.title;
                    self.trigger('tableSelected', {
                        tableProperty: property
                    });
                    return property && property.displayName || '';
                },
                highlighter: function (property) {
                    return tableNameTemplate(
                        isPlaceholder(property)
                            ? {
                                table: {
                                    displayName: property
                                }
                            }
                            : {
                                table: property
                            }
                    );
                }
            });

            if (self.attr.focus) {
                _.defer(function () {
                    field.focus();
                })
            }

            self.allowEmptyLookup(field);
            self.trigger('rendered');
        };

        this.transformTableProperties = function () {
            const self = this;
            const properties = this.attr.limitParentConceptId
                ? ontology.propertiesByConceptId[this.attr.limitParentConceptId]
                : ontology.properties;
            const list = properties.list.filter((prop) => {
                return prop.dataType === 'extendedDataTable'
            });

            let previousSelectionFound = false;
            const transformed = _.chain(list)
                .sortBy('displayName')
                .reject(function (prop) {
                    return prop.userVisible === false;
                })
                .map(function (prop) {
                    if (prop.title === self.currentTableNameTitle) {
                        previousSelectionFound = true;
                    }
                    return _.extend({}, prop, {
                        toString: function () {
                            return prop.title;
                        }
                    })
                })
                .value();

            if (this.currentTableNameTitle && !previousSelectionFound) {
                this.currentTableNameTitle = null;
                this.select('fieldSelector').val('');
                this.trigger('tableSelected', {
                    tableProperty: null
                });
            }

            return transformed;
        }
    }
});

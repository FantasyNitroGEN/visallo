require(['public/v1/api'], function(visallo) {
    'use strict';

    visallo.registry.registerExtension('org.visallo.dashboard.item', {
        title: 'Saved Search Table',
        description: 'Display tabular saved search results',
        identifier: 'org-visallo-saved-search-table',
        componentPath: 'org/visallo/web/table/dist/card',
        configurationPath: 'org/visallo/web/table/js/card/Config',
        grid: {
            width: 8,
            height: 4
        }
    });

    visallo.registry.registerExtension('com.visallo.export.transformer', {
            className: 'com.visallo.export.transformers.SavedSearchTransform',
            canHandle: function(json) {
                if (json.extension) {
                    if (json.extension.identifier === 'org-visallo-saved-search-table') {
                        return Boolean(json.item &&
                            json.item.configuration &&
                            json.item.configuration.searchId &&
                            json.item.configuration.searchParameters &&
                            !json.item.configuration.aggregations);
                    }
                }
                return false;
            },
            exporterConfiguration: function(json) {
                var exporterConfig = { orientation: 'landscape' };
                var configuration = json.item.configuration;
                var searchId = json.item.configuration.searchId;
                var tableSettings = searchId && configuration.tableSettings[searchId];

                if (tableSettings) {
                    var selectedTabIri = _.findKey(tableSettings, function(tab) { return tab.active || false});
                    var sheetsWithColumns = _.mapObject(tableSettings, function(tabSettings) {
                        return _.chain(tabSettings.columns)
                            .map(function(column) {return column.visible ? column.title : null})
                            .compact()
                            .value();
                    });
                    var columns = sheetsWithColumns[selectedTabIri];

                    if (columns) exporterConfig.columns = columns;

                    if (selectedTabIri && !exporterConfig.columns) {
                        exporterConfig.columnsForConceptIri = selectedTabIri;
                    }

                    exporterConfig.selectedTabIri = selectedTabIri;
                    exporterConfig.sheetsWithColumns = sheetsWithColumns;
                }

                if (configuration) {
                    var title = configuration.title || configuration.initialTitle || (
                        json.extension && json.extension.title);
                    if (title) {
                        exporterConfig.title = title;
                    }
                }

                return exporterConfig;
            },
            // Optional, transforms json into what className expects
            prepareForTransform: function(json) {
                var config = json.item.configuration,
                    model = _.pick(config, 'searchId', 'searchParameters'),
                    tableSettings = config.tableSettings,
                    selectedTabIri = tableSettings && _.findKey(tableSettings, function(tab) { return tab.active || false;});

                if (selectedTabIri) {
                    model.searchParameters.conceptType = selectedTabIri;
                }

                return model;
            }
        });

});

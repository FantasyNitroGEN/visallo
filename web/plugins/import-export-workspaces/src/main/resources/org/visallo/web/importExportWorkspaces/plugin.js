require(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.visallo.admin', {
        componentPath: 'org/visallo/web/importExportWorkspaces/import-plugin',
        section: i18n('admin.workspace.section'),
        name: i18n('admin.workspace.button.import'),
        subtitle: i18n('admin.workspace.import.subtitle')
    });

    registry.registerExtension('org.visallo.admin', {
        componentPath: 'org/visallo/web/importExportWorkspaces/export-plugin',
        section: i18n('admin.workspace.section'),
        name: i18n('admin.workspace.button.export'),
        subtitle: i18n('admin.workspace.export.subtitle')
    });
});

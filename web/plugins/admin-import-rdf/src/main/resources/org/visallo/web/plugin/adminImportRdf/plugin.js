define([
  'configuration/plugins/registry',
  'util/messages'
], function(registry, i18n) {
  'use strict';

  registry.registerExtension('org.visallo.admin', {
    componentPath: 'org/visallo/web/plugin/adminImportRdf/admin-import-rdf',
    section: i18n('admin.import.section'),
    name: i18n('admin.import.rdf.name'),
    subtitle: i18n('admin.import.rdf.subtitle')
  });
});

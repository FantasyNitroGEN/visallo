define([
    'configuration/plugins/registry',
    'util/messages'
], function(registry, i18n) {
    'use strict';

    var adminExtensionPoint = 'org.visallo.admin';

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/adminUserTools/user-plugin',
        section: i18n('admin.user.section'),
        name: i18n('admin.user.editor'),
        subtitle: i18n('admin.user.editor.subtitle')
    });
});

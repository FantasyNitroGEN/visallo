define([
    'configuration/plugins/registry'
], function(registry) {
    'use strict';

    var adminExtensionPoint = 'org.visallo.admin';

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'admin/bundled/uiExtensionList/index',
        section: 'Plugin',
        name: 'UI Extensions',
        subtitle: 'Extensions Available / Usages'
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'jsx!admin/bundled/pluginList/PluginList',
        section: 'Plugin',
        name: 'List',
        subtitle: 'Loaded plugins'
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'admin/bundled/notifications/list',
        section: 'System Notifications',
        name: 'List',
        subtitle: 'View all Notifications'
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'admin/bundled/notifications/create',
        section: 'System Notifications',
        name: 'Create',
        subtitle: 'Create a New Notification'
    });
})

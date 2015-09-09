require([
    'configuration/plugins/registry',
    'util/messages'
], function(registry, i18n) {
    'use strict';

    var adminExtensionPoint = 'org.visallo.admin';

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/route-runner-plugin',
        section: i18n('admin.dev.section'),
        name: i18n('admin.route.runner.name'),
        subtitle: i18n('admin.route.runner.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/element-editor-plugin',
        section: i18n('admin.element.section'),
        name: i18n('admin.element.editor.name'),
        subtitle: i18n('admin.element.editor.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/requeue-plugin',
        section: i18n('admin.element.section'),
        name: i18n('admin.element.requeue'),
        subtitle: i18n('admin.element.requeue.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/ontology-edit-concept-plugin',
        section: i18n('admin.ontology.section'),
        name: i18n('admin.ontology.concepts'),
        subtitle: i18n('admin.ontology.concepts.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/ontology-edit-property-plugin',
        section: i18n('admin.ontology.section'),
        name: i18n('admin.ontology.properties'),
        subtitle: i18n('admin.ontology.properties.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/ontology-edit-relationship-plugin',
        section: i18n('admin.ontology.section'),
        name: i18n('admin.ontology.relationships'),
        subtitle: i18n('admin.ontology.relationships.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/ontology-upload-plugin',
        section: i18n('admin.ontology.section'),
        name: i18n('admin.ontology.upload'),
        subtitle: i18n('admin.ontology.upload.subtitle')
    });

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/devTools/user-plugin',
        section: i18n('admin.user.section'),
        name: i18n('admin.user.editor'),
        subtitle: i18n('admin.user.editor.subtitle')
    });
});

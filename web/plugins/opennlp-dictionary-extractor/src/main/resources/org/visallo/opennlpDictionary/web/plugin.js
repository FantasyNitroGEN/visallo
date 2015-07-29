require(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.visallo.admin', {
        componentPath: 'org/visallo/opennlpDictionary/web/add-plugin',
        section: 'Dictionary',
        name: 'Add',
        subtitle: 'Create new dictionary entries'
    });

    registry.registerExtension('org.visallo.admin', {
        componentPath: 'org/visallo/opennlpDictionary/web/list-plugin',
        section: 'Dictionary',
        name: 'List',
        subtitle: 'Current dictionary list'
    });
});

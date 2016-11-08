#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
require(['public/v1/api'], function(visallo) {
    'use strict';

    visallo.registry.registerExtension('org.visallo.detail.toolbar', {
        title: i18n('${package}.web.detail.toolbar.google'),
        event: 'google',
        canHandle: function(objects) {
            return objects.vertices.length === 1 && objects.edges.length === 0
                && objects.vertices[0].conceptType === '${ontologyBaseIri}#person';
        }
    });

    visallo.connect().then(function(api) {
        ${symbol_dollar}(document).on('google', function(e, data) {
            var person = data.vertices[0];
            var name = api.formatters.vertex.prop(person, '${ontologyBaseIri}#fullName');
            var url = 'http://www.google.com/#safe=on&q=' + name;
            window.open(url, '_blank');
        });
    });
});

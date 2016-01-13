define([
    'configuration/plugins/registry',
    './layoutComponents/generic',
    './layoutComponents/vertex',
    './layoutComponents/edge',
    './layoutComponents/elements'
], function(registry) {
    'use strict';

    var register = _.partial(registry.registerExtension, 'org.visallo.layout.component'),
        componentArrays = _.rest(arguments, 1);

    componentArrays.forEach(function(components) {
        components.forEach(register);
    });
});

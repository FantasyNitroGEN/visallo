
define([
    '../util/ajax',
    '../store'
], function(ajax, store) {
    'use strict';

    const api = {
        properties: (locale) => store.getOrWaitForNestedState(s => s.configuration.properties),
        messages: (locale) => store.getOrWaitForNestedState(s => s.configuration.messages)
    };

    return api;

});

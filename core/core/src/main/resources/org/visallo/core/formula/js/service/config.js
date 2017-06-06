define([], function() {
    'use strict';

    var json = JSON.parse(CONFIG_JSON);
    var api = {
        properties: function(locale) { return Promise.resolve(json.properties) },
        messages: function(locale) { return Promise.resolve(json.messages) }
    };

    return api;
});


define([
    '../util/ajax',
    '../util/memoize'
], function(ajax, memoize) {
    'use strict';

    var getConfiguration = memoize(function(locale) {
            var data = {};
            if (locale) {
                if (locale.language) {
                    data.localeLanguage = locale.language;
                }
                if (locale.country) {
                    data.localeCountry = locale.country;
                }
                if (locale.variant) {
                    data.localeVariant = locale.variant;
                }
            }
            return ajax('GET', '/configuration', data);
        }),
        api = {
            properties: memoize(function(locale) {
                return getConfiguration(locale).then(_.property('properties'));
            }),

            messages: memoize(function(locale) {
                return getConfiguration(locale).then(_.property('messages'));
            })
        };

    return api;
});

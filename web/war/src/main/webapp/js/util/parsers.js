define([
], function() {
    'use strict';

    var FULL_NUMERIC_REGEX = /^[\-\+]?\d+\.?\d*$/,
        NUMERIC_WITH_UNITS_REGEX = /^[\-\+]?\d+\.?\d*[a-z]+$/,
        PARSERS = {

            number: {
                isValid: function(s) {
                    return _.isString(s) && FULL_NUMERIC_REGEX.test(s);
                },

                isValidWithUnits: function(s) {
                    return _.isString(s) && NUMERIC_WITH_UNITS_REGEX.test(s);
                },

                parseFloat: function(s) {
                    return PARSERS.number.isValid(s) ? parseFloat(s, 10) : NaN;
                },

                parseInt: function(s) {
                    return PARSERS.number.isValid(s) ? parseInt(s, 10) : NaN;
                },

                parseFloatWithUnits: function(s) {
                    return PARSERS.number.isValidWithUnits(s) ? parseFloat(s, 10) : NaN;
                },

                parseIntWithUnits: function(s) {
                    return PARSERS.number.isValidWithUnits(s) ? parseInt(s, 10) : NaN;
                }
            }
        };

    return PARSERS;
});

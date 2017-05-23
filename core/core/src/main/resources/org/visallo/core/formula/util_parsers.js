define([
], function() {
    'use strict';

    var FULL_NUMERIC_REGEX = /^[\-\+]?\d+\.?\d*$/,
        NUMERIC_WITH_UNITS_REGEX = /^[\-\+]?\d+\.?\d*[a-z]+$/,
        prepare = function(str) {
            return str.replace(/,/g, '');
        },
        PARSERS = {

            number: {
                isValid: function(s) {
                    return _.isString(s) && FULL_NUMERIC_REGEX.test(s);
                },

                isValidWithUnits: function(s) {
                    return _.isString(s) && NUMERIC_WITH_UNITS_REGEX.test(s);
                },

                parseFloat: function(s) {
                    return PARSERS.number.isValid(s) ? parseFloat(prepare(s), 10) : NaN;
                },

                parseInt: function(s) {
                    return PARSERS.number.isValid(s) ? parseInt(prepare(s), 10) : NaN;
                },

                parseFloatWithUnits: function(s) {
                    return PARSERS.number.isValidWithUnits(s) ? parseFloat(prepare(s), 10) : NaN;
                },

                parseIntWithUnits: function(s) {
                    return PARSERS.number.isValidWithUnits(s) ? parseInt(prepare(s), 10) : NaN;
                }
            },

            bool: {
                parse: function(s) {
                    if (typeof s === 'boolean') {
                        return s;
                    } else if (typeof s === 'string') {
                        if (s.toLocaleLowerCase() === 'true') {
                            return true;
                        } else if (s.toLocaleLowerCase() === 'false') {
                            return false;
                        }
                    }
                    throw new Error('Could not parse boolean "' + s + '"');
                }
            }
        };

    return PARSERS;
});

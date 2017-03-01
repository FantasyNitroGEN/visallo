/**
 * Get message bundles and configuration strings
 *
 * @module services/config
 * @see module:dataRequest
 */
define([
    '../util/ajax',
    '../store'
], function(ajax, store) {
    'use strict';

    /**
     * @alias module:services/config
     */
    const api = {

        /**
         * Configuration properties
         *
         * _This is limited to configuration values prefixed with `web.ui`_
         *
         * @function
         */
        properties: (locale) => store.getOrWaitForNestedState(s => s.configuration.properties),

        /**
         * Message bundle strings. Used by `i18n` function
         *
         * @function
         */
        messages: (locale) => store.getOrWaitForNestedState(s => s.configuration.messages)
    };

    return api;

});

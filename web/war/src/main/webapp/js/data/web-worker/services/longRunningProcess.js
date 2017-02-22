/**
 * Information regarding long-running processes.
 *
 * @module services/longRunningProcess
 * @see module:util/withDataRequest
 */
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    /**
     * @alias module:services/longRunningProcess
     */
    var api = {

        /**
         * Get details for a process
         *
         * @param {string} id
         */
        get: function(processId) {
            return ajax('GET', '/long-running-process', {
                longRunningProcessId: processId
            });
        },

        /**
         * Cancel process (up to process implementation to handle correctly
         *
         * @param {string} id
         */
        cancel: function(processId) {
            return ajax('POST', '/long-running-process/cancel', {
                longRunningProcessId: processId
            });
        },

        /**
         * Delete process
         *
         * @param {string} id
         */
        'delete': function(processId) {
            return ajax('DELETE', '/long-running-process', {
                longRunningProcessId: processId
            });
        }
    };

    return api;
});

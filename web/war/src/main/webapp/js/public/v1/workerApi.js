/**
 * Plugins should `require` this module for access to Visallo helpers
 * available in the worker thread.
 *
 * @module
 * @classdesc Visallo Top-Level API for Web Workers
 * @example
 * require(['public/v1/workerApi'], function(workerApi) {
 *     // ...
 * })
 */
define([
    'data/web-worker/util/ajax'
], function(ajax) {
    'use strict';

    /**
     * @alias module:public/v1/workerApi
     */
    return {

        /**
         * Make an `XmlHttpRequest` to the server.
         *
         * This function expects the server to respond in JSON.
         * If that is not that case you must pass a special method formatted
         * like: `[method]->HTML`. For example, `GET->HTML`.
         *
         * @function
         * @param {string} method The request method type (GET, PUT, POST, DELETE, etc)
         * @param {string} endpoint The endpoint
         * @param {object} [parameters] Any parameters to send
         * @returns {Promise}
         * @example <caption>GET</caption>
         * require(['public/v1/workerApi'], function(workerApi) {
         *     workerApi.ajax('GET', '/user/me')
         *      .then(function(user) {
         *      })
         * })
         * @example <caption>POST</caption>
         * require(['public/v1/workerApi'], function(workerApi) {
         *     workerApi.ajax('POST', '/user/logout')
         *      .then(function() {
         *      })
         * })
         * @example <caption>DELETE with Parameters</caption>
         * require(['public/v1/workerApi'], function(workerApi) {
         *     workerApi.ajax('DELETE', '/sample/object', { objectId: 'o1' })
         *      .then(function() {
         *      })
         * })
         * @example <caption>Handle Errors</caption>
         * require(['public/v1/workerApi'], function(workerApi) {
         *     workerApi.ajax('DELETE', '/sample/object', { objectId: 'o1' })
         *      .catch(function(err) {
         *          console.error(err);
         *      })
         *
         * })
         */
        ajax: ajax
    };
});

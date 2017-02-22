/**
 * Routes for running searches, and saved searches.
 *
 * @module services/search
 * @see module:util/withDataRequest
 */
define(['../util/ajax'], function(ajax) {
    'use strict';

    /**
     * @alias module:services/search
     */
    var api = {

        /**
         * Get saved searches optionally filtered
         * by search url
         *
         * @param {string} urlFilter Limit searches to those of this URL
         */
        all: function(urlFilter) {
            var visalloFilter = /^\/(?:vertex|element|edge)\/search$/;
            return ajax('GET', '/search/all')
                .then(function(result) {
                    return _.chain(result.searches)
                        .filter(function(search) {
                            if (urlFilter) {
                                if (visalloFilter.test(urlFilter)) {
                                    return visalloFilter.test(search.url);
                                }
                                return search.url === urlFilter;
                            }
                            return true;
                        })
                        .sortBy(function(search) {
                            return search.name.toLowerCase();
                        })
                        .value();
                })
        },

        /**
         * Save a search
         *
         * @param {object} query
         * @param {string} query.url The url to invoke for saved search
         * @param {object} query.parameters The search parameters valid for the
         * url
         * @param {object} [query.id] If updating previous
         * @param {object} [query.name] The name of search
         * @param {boolean} [query.global=false] If this is global search
         * _Requires special privilege_
         * @example
         * dataRequest('search', 'save', {
         *     url: 'element/search',
         *     name: 'My new Search',
         *     parameters: {
         *         q: 'Search text'
         *     }
         * }).then(function(s) { console.log('saved'); })
         */
        save: function(query) {
            var toFix = [],
                params = query.parameters;

            if (params) {
                _.each(params, function(value, name) {
                    if (_.isArray(value)) {
                        toFix.push(name);
                    }
                });
                toFix.forEach(function(name) {
                    if (!(/\[\]$/).test(name)) {
                        params[name + '[]'] = params[name];
                        delete params[name];
                    }
                });
            }
            return ajax('POST', '/search/save', query);
        },

        /**
         * Delete a search
         *
         * @param {string} id
         */
        delete: function(queryId) {
            return ajax('DELETE->HTML', '/search', {
                id: queryId
            });
        },

        /**
         * Get search object
         *
         * @param {string} id
         */
        get: function(queryId) {
            return ajax('GET', '/search', {
                id: queryId
            });
        },

        /**
         * Execute a search and get results
         *
         * Optionally accepts new parameters that take precedent over saved
         * ones.
         *
         * @param {string} id
         * @param {object} [overrideSearchParameters={}] Allows overriding or
         * adding criteria to saved search
         */
        run: function(queryId, otherParams) {
            return ajax('GET', '/search/run', _.extend({}, otherParams || {}, {
                id: queryId
            }));
        }

    };

    return api;
});

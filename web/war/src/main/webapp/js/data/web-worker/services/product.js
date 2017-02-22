/**
 * Get details about work products
 *
 * @module services/product
 * @see module:util/withDataRequest
 */
define(['../util/ajax', '../store'], function(ajax, store) {
    'use strict';

    /**
     * @alias module:services/product
     */
    return {

        /**
         * Get all products
         */
        list() {
            return ajax('GET', '/product/all')
        },

        /**
         * Create a new product
         *
         * Built-in Kinds:
         * * `org.visallo.web.product.graph.GraphWorkProduct`
         * * `org.visallo.web.product.map.MapWorkProduct`
         *
         * @param {string} kind The type of work product
         * @param {string} [title=Untitled]
         */
        create(kind, title) {
            return ajax('POST', '/product', { title, kind })
        },

        /**
         * Delete a work product
         *
         * @param {string} id
         */
        deleteProduct(productId) {
            return ajax('DELETE', '/product', { productId })
        },

        /**
         * Get work product
         *
         * @param {string} id
         * @param {boolean} [includeExtended=false] Include extendedData in
         * response
         */
        get(productId, includeExtended) {
            return ajax('GET', '/product', { productId, includeExtended })
        }
    }
})

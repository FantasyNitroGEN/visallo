define(['../util/ajax', '../store'], function(ajax, store) {
    'use strict';
    return {

        list() {
            return ajax('GET', '/product/all')
        },

        create(kind) {
            return ajax('POST', '/product', {
                title: 'My Created Graph: ' + new Date(),
                kind
            })
        },

        deleteProduct(productId) {
            return ajax('DELETE', '/product', { productId })
        },

        get(productId, includeExtended) {
            return ajax('GET', '/product', { productId, includeExtended })
        }
    }
})

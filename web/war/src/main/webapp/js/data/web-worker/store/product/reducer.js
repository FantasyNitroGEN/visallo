define(['updeep'], function(u) {
    'use strict';

    return function product(state, { type, payload }) {
        if (!state) return { workspaces: {}, types: [] };

        switch (type) {
            case 'PRODUCT_LIST': return updateList(state, payload);
            case 'PRODUCT_UPDATE_TYPES': return updateTypes(state, payload);
            case 'PRODUCT_UPDATE_TITLE': return updateTitle(state, payload);
            case 'PRODUCT_SELECT': return selectProduct(state, payload);
            case 'PRODUCT_UPDATE': return updateProduct(state, payload);
            case 'PRODUCT_PREVIEW_UPDATE': return updatePreview(state, payload);
            case 'PRODUCT_REMOVE': return removeProduct(state, payload);
            case 'PRODUCT_UPDATE_VIEWPORT': return updateViewport(state, payload);
            case 'PRODUCT_UPDATE_DATA': return updateData(state, payload);
            case 'PRODUCT_UPDATE_EXTENDED_DATA': return updateExtendedData(state, payload);
        }

        return state;
    };

    function updateTitle(state, { workspaceId, productId, title, loading }) {
        var update;
        if (loading) {
            update = { loading: true }
        } else if (title) {
            update = { loading: false, title }
        }
        if (update) {
            return u({
                workspaces: {
                    [workspaceId]: {
                        products: {
                            [productId]: update
                        }
                    }
                }
            }, state);
        }
        return state;
    }

    function updateViewport(state, { workspaceId, viewport, productId }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    viewports: {
                        [productId]: u.constant(viewport)
                    }
                }
            }
        }, state);
    }

    function updateTypes(state, { types }) {
        return u({ types: u.constant(types) }, state);
    }

    function updatePreview(state, { workspaceId, productId, md5 }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: { previewMD5: md5 }
                    }
                }
            }
        }, state);
    }

    function updateList(state, { loading, loaded, workspaceId, products }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: u.constant(_.indexBy(products || [], 'id')),
                    loading,
                    loaded
                }
            }
        }, state);
    }

    function selectProduct(state, { productId, workspaceId }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    selected: productId ? productId : null
                }
            }
        }, state);
    }

    function updateProduct(state, { product }) {
        const { id, workspaceId } = product;
        const existing = state.workspaces[workspaceId].products[id];
        const localData = existing && existing.localData || {};
        product.localData = localData;

        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [id]: u.constant(product)
                    }
                }
            }
        }, state);
    }

    function removeProduct(state, { productId, workspaceId }) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: u.omit(productId)
                }
            }
        }, state);
    }

    function updateData(state, {workspaceId, productId, key, value}) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            data: {
                                [key]: u.constant(value)
                            }
                        }
                    }
                }
            }
        }, state);
    }

    function updateExtendedData(state, {workspaceId, productId, key, value}) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            extendedData: {
                                [key]: u.constant(value)
                            }
                        }
                    }
                }
            }
        }, state);
    }

    function updateLocalData(state, {workspaceId, productId, localData}) {
        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            localData: u.constant(localData)
                        }
                    }
                }
            }
        }, state);
    }
});

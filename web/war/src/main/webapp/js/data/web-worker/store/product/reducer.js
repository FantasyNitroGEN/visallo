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
            case 'PRODUCT_REMOVE_ELEMENTS': return removeElements(state, payload);
            case 'PRODUCT_UPDATE_DATA': return updateData(state, payload);
            case 'PRODUCT_UPDATE_EXTENDED_DATA': return updateExtendedData(state, payload);

            case 'ELEMENT_UPDATE': return updateViewable(state, payload);
        }

        return state;
    };

    function removeElements(state, { workspaceId, productId, elements }) {
        const { vertexIds, edgeIds } = elements;
        const updates = {};

        if (vertexIds) updates.vertices = u.reject(v => vertexIds.includes(v.id));
        if (edgeIds) updates.edges = u.reject(e => edgeIds.includes(e.edgeId));

        return u({
            workspaces: {
                [workspaceId]: {
                    products: {
                        [productId]: {
                            extendedData: updates
                        }
                    }
                }
            }
        }, state);
    }

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

    function updateViewable(state, {workspaceId, vertices}) {
        const updateExtendedDataViewable = (product) => {
            return u.if(product.extendedData && product.extendedData.vertices, {
                extendedData: {
                    vertices: transformVertexData
                }
            }, product);
        };
        const transformVertexData = (vertexIds) => {
            return vertexIds.map((vertexId) => {
                const { id, unauthorized, ...rest } = vertexId;
                const update = vertices.find((vertex) => vertex.id === id);
                if (update !== undefined) {
                    if (update._DELETED !== true) {
                        return { id, ...rest }
                    } else {
                        return { id, unauthorized: true, ...rest };
                    }
                } else {
                    return vertexId;
                }
            });
        };

        return u.if(vertices, {
            workspaces: {
                [workspaceId]: {
                    products: u.map(updateExtendedDataViewable)
                }
            }
        }, state);
    }
});

define(['reselect', '../element/selectors'], function(reselect, elementSelectors) {
    const { createSelector } = reselect;

    const getWorkspaceId = (state) => state.workspace.currentId;

    const getSelection = (state) => state.selection.idsByType;

    const getFocused = (state) => state.element.focusing;

    const getProductWorkspaces = (state) => state.product.workspaces;

    const getProductTypes = (state) => state.product.types || [];

    const getProductState = createSelector([getWorkspaceId, getProductWorkspaces], (workspaceId, productWorkspaces) => {
        return productWorkspaces[workspaceId];
    })

    const getProductsById = createSelector([getProductState], (productState) => {
        return productState ? productState.products : {};
    })

    const getViewportsByProductId = createSelector([getProductState], (productState) => {
        return productState && productState.viewports || {};
    })

    const getProducts = createSelector([getProductsById], (productsById) => {
        return _.chain(productsById)
            .values()
            .sortBy('id')
            .sortBy('title')
            .sortBy('kind')
            .value()
    });

    const getSelectedId = createSelector([getProductState], (productState) => {
        return productState ? productState.selected : null
    });

    const getViewport = createSelector([getViewportsByProductId, getSelectedId], (viewports, productId) => {
        return productId ? viewports[productId] : null
    })

    const getProduct = createSelector([getProductsById, getSelectedId], (productsById, productId) => {
        return productId ? productsById[productId] : null;
    })

    const getElementIdsInProduct = createSelector([getProduct], (product) => {
        if (product && product.extendedData) {
            const elementIds = { vertices: product.extendedData.vertices, edges: product.extendedData.edges };
            return _.mapObject(elementIds, (elements) => _.pick(elements, e => e.unauthorized !== true));
        } else {
            return { vertices: {}, edges: {} };
        }
    });

    const getElementsInProduct = createSelector([getElementIdsInProduct, elementSelectors.getElements], (elementIds, elements) => {
        const { vertices, edges } = elementIds;
        return {
            vertices: _.pick(elements.vertices, Object.keys(vertices)),
            edges: _.pick(elements.edges, Object.keys(edges))
        };
    })

    const getSelectedElementsInProduct = createSelector([getSelection, getElementIdsInProduct], (selection, elementIds) => {
        const { vertices, edges } = elementIds;
        return {
            vertices: _.indexBy(_.intersection(selection.vertices, Object.keys(vertices))),
            edges: _.indexBy(_.intersection(selection.edges, Object.keys(edges)))
        };
    });

    const getFocusedElementsInProduct = createSelector([getFocused, getElementIdsInProduct], (focusing, elementIds) => {
        const { vertices, edges } = elementIds;
        const focused = { vertices: {}, edges: {} };
        Object.keys(vertices).forEach(vertexId => {
            if (vertexId in focusing.vertexIds) {
                focused.vertices[vertexId] = true;
            }
        });
        Object.keys(edges).forEach(edgeId => {
            if (edgeId in focusing.edgeIds) {
                focused.edges[edgeId] = true;
            }
        })
        return focused;
    });

    const getStatus = createSelector([getProductState], (productState) => {
        return productState ? _.pick(productState, 'loading', 'loaded') : { loading: false, loaded: false }
    })

    return {
        getStatus,
        getSelectedId,
        getProduct,
        getViewport,
        getProducts,
        getProductsById,
        getProductTypes,
        getElementIdsInProduct,
        getElementsInProduct,
        getSelectedElementsInProduct,
        getFocusedElementsInProduct
    };
})


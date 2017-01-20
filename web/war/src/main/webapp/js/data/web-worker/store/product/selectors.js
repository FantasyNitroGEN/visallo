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

    const getElementIdsInProduct = createSelector([getProduct, elementSelectors.getElements], (product, elements) => {
        if (!product || !product.extendedData) return { vertices: [], edges: [] };

        const { vertices, edges } = product.extendedData
        const viewable = v => v.unauthorized !== true || (elements.vertices && elements.vertices[v.id])

        return { vertices: vertices.filter(viewable), edges }
    });

    const getElementsInProduct = createSelector([getElementIdsInProduct, elementSelectors.getElements], (elementIds, elements) => {
        return {
            vertices: _.pick(elements.vertices, _.pluck(elementIds.vertices, 'id')),
            edges: _.pick(elements.edges, _.pluck(elementIds.edges, 'edgeId'))
        };
    })

    const getSelectedElementsInProduct = createSelector([getSelection, getElementIdsInProduct], (selection, elementIds) => {
        const { vertices, edges } = elementIds;
        return {
            vertices: _.indexBy(_.intersection(selection.vertices, _.pluck(vertices, 'id'))),
            edges: _.indexBy(_.intersection(selection.edges, _.pluck(edges, 'edgeId')))
        };
    });

    const getFocusedElementsInProduct = createSelector([getFocused, getElementIdsInProduct], (focusing, elementIds) => {
        const { vertices, edges } = elementIds;
        const focused = { vertices: {}, edges: {} };
        vertices.forEach(v => {
            if (v.id in focusing.vertexIds) {
                focused.vertices[v.id] = true;
            }
        });
        edges.forEach(e => {
            if (e.edgeId in focusing.edgeIds) {
                focused.edges[e.edgeId] = true;
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


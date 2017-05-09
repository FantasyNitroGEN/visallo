define([
    'data/web-worker/store/actions',
    'data/web-worker/store/product/actions-impl',
    'data/web-worker/store/product/selectors',
    'data/web-worker/util/ajax'
], function(actions, productActions, productSelectors, ajax) {
    actions.protectFromMain();

    const api = {
        dropElements: ({ productId, elements }) => (dispatch, getState) => {
            const { vertexIds, edgeIds } = elements;
            // TODO: get edges from store first
            var edges = (edgeIds && edgeIds.length) ? (
                ajax('POST', '/edge/multiple', { edgeIds })
                    .then(function({ edges }) {
                        return _.flatten(edges.map(e => [e.inVertexId, e.outVertexId]));
                    })
                ) : Promise.resolve([]);

            edges.then(function(edgeVertexIds) {
                const product = productSelectors.getProductsById(getState())[productId];
                const existing = product.extendedData ? Object.keys(product.extendedData.vertices) : [];
                const combined = _.without(_.uniq(edgeVertexIds.concat(vertexIds)), ..._.pluck(existing, 'id'));
                if (!combined.length) return;

                ajax('POST', '/product', {
                    productId,
                    params: {
                        updateVertices: _.object(combined.map(id => [id, {}]))
                    }
                })
                dispatch(productActions.select({ productId }));
            })
        },
    };

    return api;
})


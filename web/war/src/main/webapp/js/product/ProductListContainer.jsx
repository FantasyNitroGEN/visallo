define([
    'react',
    'react-redux',
    './ProductList',
    'data/web-worker/store/product/actions',
    'data/web-worker/store/product/selectors',
    'configuration/plugins/registry'
], function(React, redux, ProductList, productActions, productSelectors, registry) {
    'use strict';

    registry.markUndocumentedExtensionPoint('org.visallo.workproduct')

    return redux.connect(

        (state, props) => {
            return {
                status: productSelectors.getStatus(state),
                types: productSelectors.getProductTypes(state),
                selected: productSelectors.getSelectedId(state),
                products: productSelectors.getProducts(state),
                workspace: state.workspace.currentId ?
                    state.workspace.byId[state.workspace.currentId] : null
            }
        },

        (dispatch, props) => {

            $(document).on('didToggleDisplay', function(event, data) {
                if (data.name === 'products' && data.visible) {
                    dispatch(productActions.list());
                }
            })
            return {
                onCreate: (type) => { dispatch(productActions.create('Untitled', type)) },
                onDeleteProduct: (productId) => { dispatch(productActions.delete(productId)) },
                onUpdateTitle: (productId, title) => {
                    dispatch(productActions.updateTitle(productId, title));
                },
                onSelectProduct: (productId) => {
                    dispatch(productActions.select(productId))
                }
            }
        }

    )(ProductList);
});

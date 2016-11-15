define([
    'react',
    'react-redux',
    'configuration/plugins/registry',
    './ProductDetail',
    './ProductDetailEmpty',
    'data/web-worker/store/product/actions',
    'data/web-worker/store/product/selectors'
], function(
    React,
    redux,
    registry,
    ProductDetail,
    ProductDetailEmpty,
    productActions,
    productSelectors) {
    'use strict';

    const ProductDetailContainer = React.createClass({
        render() {
            var { props } = this;

            if (props.product) {
                return (<ProductDetail {...props} />);
            } else if (props.extensions) {
                return (<ProductDetailEmpty {...props} />);
            }

            return null
        }
    })

    return redux.connect(

        (state, props) => {
            const product = productSelectors.getProduct(state);
            const { loading, loaded } = productSelectors.getStatus(state);
            const extensions = registry.extensionsForPoint('org.visallo.workproduct');

            if (product) {
                const productExtensions = _.where(extensions, { identifier: product.kind });

                if (productExtensions.length === 0) {
                    throw Error('No org.visallo.workproduct extensions registered for: ' + product.kind)
                }
                if (productExtensions.length !== 1) {
                    throw Error('Multiple org.visallo.workproduct extensions registered for: ' + product.kind)
                }
                return {
                    padding: state.panel.padding,
                    product,
                    extension: productExtensions[0]
                }
            } else if (extensions.length && loaded) {
                const workspace = state.workspace.currentId ?
                    state.workspace.byId[state.workspace.currentId] : null;
                const user = state.user.current;
                return {
                    padding: state.panel.padding,
                    extensions,
                    editable: workspace && user ?
                        workspace.editable && user.privileges.includes('EDIT') :
                        false
                };
            }
            return {}
        },

        (dispatch) => {
            return {
                onGetProduct: (id) => dispatch(productActions.get(id)),
                onCreateProduct: (kind) => dispatch(productActions.create(i18n('product.item.title.default'), kind))
            }
        }

    )(ProductDetailContainer);
});

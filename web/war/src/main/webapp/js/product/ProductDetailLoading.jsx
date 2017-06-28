define([
    'create-react-class', 'prop-types'
], function(createReactClass, PropTypes) {
    'use strict';

    const ProductDetailLoading = function(props) {
        const { padding, type, title } = props;
        return (
            <div className="products-empty-wrap" style={{ ...padding }}>
                <div className="products-empty">
                    <h1>{i18n('product.item.loading', type)}</h1>
                    <h2>{title}</h2>
                </div>
            </div>
        )
    };

    return ProductDetailLoading;
});

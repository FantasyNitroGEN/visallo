define([
    'create-react-class', 'prop-types'
], function(createReactClass, PropTypes) {
    'use strict';

    const ProductDetailNoSelection = function(props) {
        const { padding } = props;
        return (
            <div className="products-empty-wrap" style={{ ...padding }}>
                <div className="products-empty">
                    <h1>{i18n('product.item.noselection')}</h1>
                    <h2>{i18n('product.item.noselection.subtitle')}</h2>
                </div>
            </div>
        )
    };

    return ProductDetailNoSelection;
});

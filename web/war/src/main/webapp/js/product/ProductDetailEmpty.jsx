define([
    'react'
], function(React) {
    'use strict';

    const PropTypes = React.PropTypes;
    const ProductDetailEmpty = React.createClass({
        propTypes: {
            editable: PropTypes.bool.isRequired,
            extensions: PropTypes.array.isRequired,
            padding: PropTypes.shape({
                left: PropTypes.number.isRequired,
                right: PropTypes.number.isRequired,
                top: PropTypes.number.isRequired,
                bottom: PropTypes.number.isRequired
            }),
            onCreateProduct: PropTypes.func.isRequired
        },
        render() {
            const { extensions, padding, editable } = this.props;
            const extensionItems = _.sortBy(extensions, e => i18n(e.identifier + '.name').toLowerCase())
                .map(e => {
                    const { identifier } = e;
                    return (
                        <li key={identifier}><button onClick={this.onClick.bind(null, identifier)} className="btn">{i18n(identifier + '.name')}</button></li>
                    );
                })
            return (
                <div className="products-empty-wrap" style={{ ...padding }}>
                    <div className="products-empty">
                        <h1>{i18n('product.empty.message')}</h1>
                        {editable ? (<h2>{i18n('product.empty.create')}</h2>) : null}
                        {editable ? (<ul>{extensionItems}</ul>) : null}
                    </div>
                </div>
            )
        },
        onClick(identifier) {
            this.props.onCreateProduct(identifier);
        }
    });

    return ProductDetailEmpty;
});

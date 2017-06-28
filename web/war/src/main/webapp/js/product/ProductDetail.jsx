define([
    'create-react-class',
    'prop-types',
    './ProductDetailLoading'
], function(createReactClass, PropTypes, ProductDetailEmpty) {
    'use strict';

    const ProductDetail = createReactClass({
        propTypes: {
            product: PropTypes.shape({
                id: PropTypes.string.isRequired
            }).isRequired,
            extension: PropTypes.shape({
                componentPath: PropTypes.string.isRequired
            }).isRequired
        },
        getInitialState: () => ({ Component: null }),
        requestComponent(props) {
            props.onGetProduct(props.product.id)
            if (props.extension.componentPath !== this.props.extension.componentPath || !this.state.Component) {
                this.setState({ Component: null })
                Promise.require(props.extension.componentPath).then((C) => this.setState({ Component: C }))
            }
        },
        componentDidMount() {
            this.requestComponent(this.props);
        },
        componentWillReceiveProps(nextProps) {
            this.requestComponent(nextProps);
        },
        render() {
            var { Component } = this.state;
            var { product, editable } = this.props;
            var { extendedData, title } = product;

            return (
                Component && extendedData ?
                    (<Component product={this.props.product}></Component>) :
                    (<ProductDetailEmpty
                        editable={editable}
                        type={i18n(this.props.extension.identifier + '.name')}
                        title={title}
                        padding={this.props.padding} />)
            )
        }
    });

    return ProductDetail;
});

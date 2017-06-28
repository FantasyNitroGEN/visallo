define([
    'create-react-class',
    'prop-types',
    './ProductListItem',
    'components/RegistryInjectorHOC'
], function(createReactClass, PropTypes, ProductListItem, RegistryInjectorHOC) {
    'use strict';

    const ProductList = createReactClass({
        render() {
            const { products, status, onCreate, types, registry, workspace, user, ...rest } = this.props;
            const { loading, loaded } = status;
            if (!workspace) return null;
            if (!user) return null;
            const editable = workspace.editable && user.privileges.includes('EDIT')

            if (!loaded && !loading) {
                this.props.onLoadProducts();
                return null;
            }

            var itemElements = products.map(product => <ProductListItem key={product.id} registry={registry} product={product} editable={editable} {...rest} />),
                content = loading ? (<div className="message">{i18n('product.empty.message')}</div>) :
                    loaded && itemElements.length ? itemElements :
                    loaded ? (<div className="message">{i18n('product.empty.message')}</div>) :
                    (<div></div>);

            return (
                <div className="products-container">
                    <ul className="products-list nav nav-list">
                        <li className="nav-header">{i18n('product.list.header')}<span className={loading ? 'loading badge' : 'badge'}></span></li>

                        {editable ? (<li className="toolbar">
                            <div className="new btn-group">
                                <a className="btn dropdown-toggle btn-mini" data-toggle="dropdown">{i18n('product.list.create')}</a>
                                <ul className="dropdown-menu">
                                {_.sortBy(types, t => i18n(`${t}.name`)).map(type => {
                                    return (
                                        <li key={type}><a onClick={onCreate.bind(this, type)} key={type}>{i18n(type + '.name')}</a></li>
                                    )
                                })}
                                </ul>
                            </div>
                        </li>) : null}
                    </ul>
                    <div className="products-list-items">
                        {content}
                    </div>
                </div>
            );
        }
    });

    return RegistryInjectorHOC(ProductList, ['org.visallo.workproduct']);
});

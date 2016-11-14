define([
    'react'
], function(React) {
    'use strict';

    const GraphEmpty = function(props) {
        const { panelPadding, onSearch, onCreate, editable } = props;

        return (
            <div className="products-empty-wrap" style={{ ...panelPadding }}>
                <div className="products-empty">
                    <h1>{i18n('org.visallo.web.product.graph.empty.title')}</h1>
                    {editable ? (<h2>{i18n('org.visallo.web.product.graph.empty.subtitle')}</h2>) : null}
                    {editable ? (
                    <ul style={{ textAlign: 'left', maxWidth: '22em' }}>
                        <li>
                            <strong>{i18n('org.visallo.web.product.graph.empty.search')}</strong>
                            <p>{i18n('org.visallo.web.product.graph.empty.search.description.prefix')} <a href="#" onClick={onSearch}>{i18n('org.visallo.web.product.graph.empty.search')}</a> {i18n('org.visallo.web.product.graph.empty.search.description.suffix')}</p>
                        </li>
                        <li>
                            <strong>{i18n('org.visallo.web.product.graph.empty.upload')}</strong>
                            <p>{i18n('org.visallo.web.product.graph.empty.upload.description.prefix')} <a href="#" onClick={onCreate}>{i18n('org.visallo.web.product.graph.empty.upload')}</a> {i18n('org.visallo.web.product.graph.empty.upload.description.suffix')}</p>
                        </li>
                        <li>
                            <strong>{i18n('org.visallo.web.product.graph.empty.create')}</strong>
                            <p>{i18n('org.visallo.web.product.graph.empty.create.description.prefix')} <a href="#" onClick={onCreate}>{i18n('org.visallo.web.product.graph.empty.create')}</a> {i18n('org.visallo.web.product.graph.empty.create.description.suffix')}</p>
                        </li>
                    </ul>
                    ) : null}
                </div>
            </div>
        )
    };

    return GraphEmpty;
});

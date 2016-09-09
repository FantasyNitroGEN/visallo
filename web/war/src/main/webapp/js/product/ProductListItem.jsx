define([
    'react',
    'components/DroppableHOC'
], function(React, DroppableHOC) {
    'use strict';

    const MaxTitleLength = 128;
    const PropTypes = React.PropTypes;
    const ProductListItem = React.createClass({
        propTypes: {
            selected: PropTypes.string,
            editable: PropTypes.bool.isRequired,
            product: PropTypes.shape({
                id: PropTypes.string.isRequired,
                title: PropTypes.string.isRequired,
                workspaceId: PropTypes.string.isRequired,
                kind: PropTypes.string.isRequired,
                previewMD5: PropTypes.string,
                loading: PropTypes.bool
            }).isRequired
        },
        getInitialState() {
            return {
                confirmDelete: true,
                editing: false,
                loading: false
            };
        },
        componentWillReceiveProps(nextProps) {
            const { product } = nextProps;
            if ('loading' in product && !product.loading && this.state.loading && this.state.editing) {
                this.setState({ editing: false, loading: false })
            }
        },
        componentDidUpdate() {
            if (this.state.editing && this.refs.titleField) {
                this.refs.titleField.focus();
                this.refs.titleField.select();
            }
        },
        render() {
            const { product, selected, registry, editable } = this.props;
            const { confirmDelete, editing, loading } = this.state;
            const { previewMD5, id, kind, title, workspaceId } = product;
            const isSelected = selected === id;
            const previewStyle = previewMD5 ? {
                backgroundImage: `url(product/preview?productId=${encodeURIComponent(id)}&workspaceId=${encodeURIComponent(workspaceId)}&md5=${previewMD5})`
            } : {};
            const buttons = loading ?
                ([
                    <button key="save" className="loading btn btn-primary btn-mini" disabled>{i18n('product.item.edit.save')}</button>
                ]) :
                editing ?
                ([
                    <button key="cancel" onClick={this.onCancel} className="cancel btn btn-link btn-default btn-mini">{i18n('product.item.edit.cancel')}</button>,
                    <button key="save" onClick={this.onSave} className="btn btn-primary btn-mini">{i18n('product.item.edit.save')}</button>
                ]) :
                ([
                    <button key="edit" onClick={this.onEdit} className="btn btn-default btn-mini">{i18n('product.item.edit')}</button>,
                    (confirmDelete ?
                        (<button key="delete" className="btn btn-danger btn-mini" onClick={this.onConfirmDelete}>{i18n('product.item.delete')}</button>) :
                        (<button key="confirmDelete" className="btn btn-danger btn-mini" onClick={this.onDelete}>{i18n('product.item.delete.confirm')}</button>)
                    )
                ]);
            const cls = ['products-list-item']

            if (isSelected) cls.push('active');
            if (editing) cls.push('editing');
            const inputAttrs = loading ? { disabled: true } : {};

            return (
                <div title={title}
                    className={cls.join(' ')}
                    onClick={this.onSelect}
                    onMouseLeave={this.onLeaveItem}>
                    <div className="buttons">{editable ? buttons : null}</div>
                    <div className={previewMD5 ? 'preview' : 'no-preview'} style={previewStyle}/>
                    <div className="content">
                        <h1>{ editing ? (
                            <input maxLength={MaxTitleLength}
                                onKeyUp={this.onTitleKeyUp}
                                ref="titleField"
                                type="text" defaultValue={title} {...inputAttrs} />
                        ) : title}</h1>
                        <h2>{i18n(`${kind}.name`)}</h2>
                    </div>
                </div>
            );
        },
        onSelect(event) {
            if (event.target !== this.refs.titleField) {
                this.props.onSelectProduct(this.props.product.id);
            }
        },
        onConfirmDelete(event) {
            event.stopPropagation();
            this.setState( { confirmDelete: false })
        },
        onLeaveItem() {
            if (!this.state.confirmDelete) {
                this.setState({ confirmDelete: true });
            }
        },
        onDelete(event) {
            event.stopPropagation();
            this.props.onDeleteProduct(this.props.product.id);
        },
        onEdit(event) {
            event.stopPropagation();
            this.setState({ editing: true })
        },
        onSave(event) {
            event.stopPropagation();
            this.setState({ loading: true })
            this.props.onUpdateTitle(this.props.product.id, this.refs.titleField.value);
        },
        onCancel(event) {
            event.stopPropagation();
            this.setState({ editing: false })
        },
        onTitleKeyUp(event) {
            if (event.keyCode === 13) {
                this.onSave(event);
            } else if (event.keyCode === 27) {
                this.setState({ editing: false })
            }
        }
    });

    const ProductListItemWithDroppable = DroppableHOC(ProductListItem);
    const ProductListItemDroppable = function(props) {
        return (<ProductListItemWithDroppable {...props}
            mimeTypes={[VISALLO_MIMETYPES.ELEMENTS]}
            onDrop={e => {
                const extension = _.findWhere(props.registry['org.visallo.workproduct'], {
                    identifier: props.product.kind
                });

                if (_.isFunction(extension.handleDrop)) {
                    if (extension.handleDrop(e, props.product)) {
                        e.preventDefault();
                        e.stopPropagation();
                    }
                }
            }}
        />);
    }

    return ProductListItemDroppable;
});

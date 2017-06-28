define(['create-react-class', 'util/formatters'], function(createReactClass, F) {
    'use strict';

    const BrowserDirectoryItem = createReactClass({

        render() {
            const { name, type, size, selected } = this.props;
            return (
                <li tabIndex="-1"
                    className={type + (selected ? ' active' : '')}
                    onClick={this.onClick}>
                    <span className="name" title={name}>{name}</span>
                    {type === 'file' ? (
                        <span className="size">{F.bytes.pretty(size)}</span>
                    ) : null}
                </li>
            );
        },

        onClick() {
            const { name, type } = this.props;
            if (type === 'file') this.props.onSelectItem(name);
            else this.props.onOpenDirectory(name)
        }

    });

    return BrowserDirectoryItem;
});

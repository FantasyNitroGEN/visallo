define([
    'create-react-class',
    'prop-types',
    './BrowserDirectoryItem'
], function(createReactClass, PropTypes, BrowserDirectoryItem) {
    'use strict';

    const BrowserDirectory = createReactClass({
        propTypes: {
            contents: PropTypes.array,
            selected: PropTypes.array.isRequired,
            onOpenDirectory: PropTypes.func.isRequired,
            onSelectItem: PropTypes.func.isRequired
        },
        render() {
            const { contents, selected, onOpenDirectory, onSelectItem } = this.props;

            if (contents) {
                if (contents.length) {
                    return (
                        <ul>
                            {contents.map(item => {
                                return (
                                    <BrowserDirectoryItem
                                        {...item}
                                        key={item.name}
                                        selected={selected.includes(item.name)}
                                        onSelectItem={onSelectItem}
                                        onOpenDirectory={onOpenDirectory} />
                                );
                            })}
                        </ul>
                    );
                }

                return (<ul><li>No items found</li></ul>);
            }

            return (<ul><li>Loading...</li></ul>);
        }
    });

    return BrowserDirectory;
});

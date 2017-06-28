define([
    'create-react-class',
    'prop-types',
    './ScrollButtons'
], function(
    createReactClass,
    PropTypes,
    ScrollButtons) {
    'use strict';

    const Tabs = createReactClass({
        propTypes: {
            tabs: PropTypes.object.isRequired,
            activeTab: PropTypes.string,
            onTabClick: PropTypes.func
        },

        getInitialState() {
            return { offset: 0 }
        },

        componentDidMount() {
            this.setState({  overflow: this._container.offsetWidth < this._tabs.scrollWidth });
        },

        onScrollClick(direction) {
            const { offset } = this.state;
            const width = this._container.offsetWidth;
            const totalWidth = this._tabs.scrollWidth;
            const newOffset = direction === 'left' ? Math.max(offset - width, 0) : Math.min(offset + width, totalWidth - width);
            this.setState({ offset: newOffset, overflow: totalWidth - newOffset > width });
        },

        render() {
            const { offset, overflow } = this.state;
            const { tabs, activeTab, onTabClick } = this.props;

            return (
                <div className="tabs">
                    <div className="list-container" ref={(ref) => {this._container = ref}}>
                        <ul className="tab-list" ref={(ref) => {this._tabs = ref}} style={{marginLeft: -offset + 'px'}}>
                            {Object.keys(tabs).map((key) => {
                                const { count, displayName } = tabs[key];
                                const tabClass = key === activeTab ? 'active' : '';

                                return (
                                    <li onClick={() => onTabClick(key)} key={key} className={tabClass}>
                                        <span className="name">{displayName}</span>
                                        <span className="count">{count}</span>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                    <ScrollButtons offset={offset} overflow={overflow} onScrollClick={this.onScrollClick}/>
                </div>
            );
        }
    });

    return Tabs;
});
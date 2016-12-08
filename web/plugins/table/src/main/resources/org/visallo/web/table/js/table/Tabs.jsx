define([
    'react'
], function(
    React) {
    'use strict';

    const Tabs = ({ tabs, activeTab, onTabClick }) => {
        return (
            <ul className="tabs">
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
        );
    };

    Tabs.propTypes =  {
        tabs: React.PropTypes.object.isRequired,
        activeTab: React.PropTypes.string,
        onTabClick: React.PropTypes.func
    };

    return Tabs;
});
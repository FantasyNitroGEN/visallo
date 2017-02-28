define([
    'react',
    'classnames'
], function(
    React,
    classNames) {
    'use strict';

    const ScrollButtons = ({ offset, overflow, onScrollClick }) => {
        const buttonClass = ['scrollButton', 'disable-text-selection'];

        return (
            <div className="tabScrollButtons">
                <div className={classNames(buttonClass, { disabled: offset === 0 })} onClick={() => onScrollClick('left')}> ◀ </div>
                <div className={classNames(buttonClass, { disabled: !overflow })} onClick={() => onScrollClick('right')}> ▶ </div>
            </div>
        );
    };

    ScrollButtons.propTypes = {
        offset: React.PropTypes.number,
        overflow: React.PropTypes.bool,
        onScrollClick: React.PropTypes.func
    };

    return ScrollButtons;
});
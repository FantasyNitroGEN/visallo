define([
    'react'
], function(React) {
    'use strict';

    const PropTypes = React.PropTypes;
    const DashboardCard = React.createClass({
        propTypes: {
            item: PropTypes.shape({
                id: PropTypes.string.isRequired
            }).isRequired
        },
        render() {
            var props = this.props,
                item = props.item.item;

            return (
                <div className="card-inner">
                    {item.extensionId}
                    <br />
                    {item.id.substring(0, 15)}
                </div>
            )
        }
    });

    return DashboardCard;
});

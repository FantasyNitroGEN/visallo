define([
    'react'
], function (React) {
    'use strict';

    const PropTypes = React.PropTypes;
    const ReactParams = React.createClass({
        handleClick: function() {
            this.props.customBehavior('param1')
        },
        render: function(){
            const { customBehavior } = this.props;
            return (
                <div onClick={this.handleClick}>Click Me</div>
            );
        },
        propTypes: {
            visalloApi: PropTypes.object.isRequired,
            customBehavior: PropTypes.func.isRequired
        }
    })

    return ReactParams;
});

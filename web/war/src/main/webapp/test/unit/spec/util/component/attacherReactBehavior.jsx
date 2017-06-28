define([
    'create-react-class',
    'prop-types'
], function(createReactClass, PropTypes) {
    'use strict';

    const ReactParams = createReactClass({
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

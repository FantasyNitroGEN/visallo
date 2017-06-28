define([
    'create-react-class'
], function (createReactClass) {
    'use strict';

    const ReactParams = createReactClass({
        render: function(){
            return (
              <div>{this.props.param}</div>
            );
        }
    })

    return ReactParams;
});

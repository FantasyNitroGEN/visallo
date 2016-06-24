define([
    'react'
], function (React) {
    'use strict';

    const ReactParams = React.createClass({
        render: function(){
            return (
              <div>{this.props.param}</div>
            );
        }
    })

    return ReactParams;
});

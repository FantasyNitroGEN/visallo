define([
    'react'
], function (React) {
    'use strict';

    const ReactReattach = React.createClass({
        click: function() {
            this.props.changeParam('changed')
        },
        render: function(){
            return (
              <div onClick={this.click}>{this.props.param}</div>
            );
        }
    })

    return ReactReattach;
});

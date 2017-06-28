define([
    'create-react-class'
], function(createReactClass) {
    'use strict';

    const ReactReattach = createReactClass({
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

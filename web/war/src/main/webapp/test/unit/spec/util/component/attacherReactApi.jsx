define([
    'react'
], function (React) {
    'use strict';

    const ReactParams = React.createClass({
        render: function(){
            return (
              <div>{this.props.visalloApi.v1.formatters.string.plural(1, 'cat')}</div>
            );
        }
    })

    return ReactParams;
});

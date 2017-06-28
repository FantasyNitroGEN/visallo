define([
    'create-react-class'
], function (createReactClass) {
    'use strict';

    const ReactParams = createReactClass({
        render: function(){
            return (
              <div>{this.props.visalloApi.v1.formatters.string.plural(1, 'cat')}</div>
            );
        }
    })

    return ReactParams;
});

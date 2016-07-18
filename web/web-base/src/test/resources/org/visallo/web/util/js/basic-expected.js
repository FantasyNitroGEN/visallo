'use strict';

define(['react'], function (React) {
    var Basic = React.createComponent({
        render: function render() {
            return React.createElement('div', { x: this.props.x });
        }
    });
});
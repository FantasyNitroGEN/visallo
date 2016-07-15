'use strict';

define(['react'], function (React) {
    var ObjectSpread = React.createComponent({
        render: function render() {
            var divProps = { x: 1, y: 2 };
            return React.createElement('div', divProps);
        }
    });
});
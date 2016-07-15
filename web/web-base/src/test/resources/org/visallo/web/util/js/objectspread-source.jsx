define(['react'], function(React) {
    const ObjectSpread = React.createComponent({
        render() {
            var divProps = {x: 1, y: 2};
            return <div {...divProps} />
        }
    });
});
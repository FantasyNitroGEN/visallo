define([
    'react',
    'util/component/attacher'
], function(React, attacher) {
    'use strict';

    const PropTypes = React.PropTypes;
    const Attacher = React.createClass({

        propTypes: {
            componentPath: PropTypes.string.isRequired,
            nodeType: PropTypes.string
        },

        getDefaultProps() {
            return { nodeType: 'div' };
        },

        componentDidMount() {
            this.reattach(this.props);
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.componentPath !== this.props.componentPath) {
                this.reattach(nextProps);
            }
        },

        componentWillUnmount() {
            this.attacher.teardown();
        },

        render() {
            const { nodeType } = this.props;

            return React.createElement(nodeType, { ref: 'node' });
        },

        reattach(props) {
            this.attacher = attacher()
                .node(this.refs.node)
                .path(props.componentPath)
                .params(props);

            this.attacher.attach({
                teardown: true,
                empty: true
            })
        }
    });

    return Attacher;
});

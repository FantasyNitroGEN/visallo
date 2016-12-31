define([
    'react',
    'util/component/attacher'
], function(React, attacher) {
    'use strict';

    const PropTypes = React.PropTypes;
    const Attacher = React.createClass({

        propTypes: {
            componentPath: PropTypes.string.isRequired,
            behavior: PropTypes.object,
            legacyMapping: PropTypes.object,
            nodeType: PropTypes.string
        },

        getDefaultProps() {
            return { nodeType: 'div' };
        },

        getInitialState() {
            return { element: null }
        },

        componentDidMount() {
            this.reattach(this.props);
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps !== this.props) {
                this.reattach(nextProps);
            }
        },

        componentWillUnmount() {
            this.attacher.teardown();
        },

        render() {
            const { nodeType } = this.props;
            const { element } = this.state;

            return element ? element : React.createElement(nodeType, { ref: 'node' });
        },

        reattach(props) {
            const { componentPath, legacyMapping, behavior, nodeType, ...rest } = props;

            this.attacher = attacher({ preferDirectReactChildren: true })
                .path(componentPath)
                .params(rest);

            if (this.refs.node) {
                this.attacher.node(this.refs.node)
            }

            if (behavior) {
                this.attacher.behavior(behavior)
            }

            if (legacyMapping) {
                this.attacher.legacyMapping(legacyMapping)
            }

            this.attacher.attach({
                teardown: true,
                teardownOptions: { react: false },
                emptyFlight: true
            }).then(attach => {
                if (attach._reactElement) {
                    this.setState({ element: attach._reactElement })
                }
            })
        }
    });

    return Attacher;
});

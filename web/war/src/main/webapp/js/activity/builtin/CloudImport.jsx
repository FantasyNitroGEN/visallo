define([
    'create-react-class', 'prop-types'
], function(createReactClass, PropTypes) {
    'use strict';

    const CloudImport = createReactClass({
        propTypes: {
        },

        handleClick() {
            const { vertexIds } = this.props.process;
            $(this.button).trigger('selectObjects', {
                vertexIds
            });
        },

        render() {
            return (
                <button
                    className="btn btn-mini"
                    onClick={this.handleClick}
                    ref={(button) => { this.button = button; }}
                >
                    Open
                </button>
            )
        }
    });

    return CloudImport;
});

define([
    'react'
], function(React) {
    'use strict';

    const PropTypes = React.PropTypes;
    const CloudImport = React.createClass({
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

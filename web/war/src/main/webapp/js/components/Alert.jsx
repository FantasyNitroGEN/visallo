define([
    'react'
], function(React) {
    'use strict';

    const Alert = React.createClass({
        propTypes: {
            error: React.PropTypes.any,
            onDismiss: React.PropTypes.func
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.error && nextProps.error !== this.props.error) {
                console.error(nextProps.error);
            }
        },

        renderMessage() {
            var info;
            if (_.isArray(this.props.error)) {
                info = this.props.error;
            } else if (this.props.error.statusText) {
                info = this.props.error.statusText;
            } else {
                info = i18n('admin.plugin.error');
            }

            if (_.isArray(info) && info.length > 1) {
                return (
                    <ul>
                        {info.map((i)=> {
                            return (<li>{i}</li>);
                        })}
                    </ul>
                )
            } else if (_.isArray(info)) {
                return (<div>{info[0]}</div>);
            } else {
                return (<div>{info}</div>);
            }
        },

        renderType() {
            if (this.props.error.type) {
                return (<strong>{this.props.error.type}</strong>);
            }
            return null;
        },

        handleDismissClick(e) {
            if (this.props.onDismiss) {
                this.props.onDismiss(e);
            }
        },

        render() {
            if (!this.props.error) {
                return null;
            }

            return (
                <div className="alert alert-error">
                    <button type="button" className="close" onClick={this.handleDismissClick}>&times;</button>
                    {this.renderType()}
                    {this.renderMessage()}
                </div>
            );
        }
    });

    return Alert;
});

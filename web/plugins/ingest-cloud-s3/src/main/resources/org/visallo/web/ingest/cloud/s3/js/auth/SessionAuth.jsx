define([
   'create-react-class',
   'prop-types',
    '../i18n'
], function(createReactClass, PropTypes, i18n) {

    const SessionAuth = createReactClass({
        propTypes: {
            errorMessage: PropTypes.string,
            onConnect: PropTypes.func.isRequired
        },

        getInitialState() {
            return {
                accessKey: '', secret: '', token: ''
            }
        },

        render() {
            const { loading, errorMessage } = this.props;
            const { accessKey, secret, token } = this.state;
            const valid = Boolean(accessKey.length && secret.length && token.length);

            return (
                <div>
                    {errorMessage ? (
                        <div className="alert alert-error">{errorMessage}</div>
                    ) : null}

                    <label>{i18n('access_key')}
                        <input type="text" value={accessKey} onChange={this.handleChange('accessKey')} onKeyDown={this.onKeyDown} />
                    </label>

                    <label>{i18n('secret')}
                        <input type="password" value={secret} onKeyDown={this.onKeyDown} onChange={this.handleChange('secret')} />
                    </label>

                    <label>{i18n('token')}
                        <input type="password" value={token} onKeyDown={this.onKeyDown} onChange={this.handleChange('token')} />
                    </label>

                    <div className="help"><a target="_blank" href={i18n('session_auth.help_url')}>{i18n('help')}</a></div>

                    <div className="buttons">
                        <button
                            disabled={!valid}
                            onClick={this.connect}
                            className={(loading ? "loading " : "") + "btn btn-primary"}>{i18n('connect')}</button>
                    </div>
                </div>
            );
        },

        onKeyDown(event) {
            event.stopPropagation()
        },

        handleChange(stateField) {
            return event => {
                this.setState({ [stateField]: event.target.value })
            }
        },

        connect() {
            const { accessKey, secret, token } = this.state;
            this.props.onConnect({ accessKey, secret, token });
        }
    });

    return SessionAuth;
});


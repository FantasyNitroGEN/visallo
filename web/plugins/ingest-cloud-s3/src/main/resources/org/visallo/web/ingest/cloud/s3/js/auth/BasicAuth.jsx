define([
    'create-react-class',
    'prop-types',
    '../i18n'
], function(createReactClass, PropTypes, i18n) {

    const BasicAuth = createReactClass({
        propTypes: {
            errorMessage: PropTypes.string,
            onConnect: PropTypes.func.isRequired
        },

        getInitialState() {
            return {
                accessKey: '', secret: ''
            }
        },

        render() {
            const { loading, errorMessage } = this.props;
            const { accessKey, secret } = this.state;
            const valid = Boolean(accessKey.length && secret.length);

            return (
                <div>
                    {errorMessage ? (
                        <div className="alert alert-error">{errorMessage}</div>
                    ) : null}

                    <label>
                        {i18n('access_key')}
                        <input type="text" value={accessKey} onChange={this.handleChange('accessKey')} onKeyDown={this.onKeyDown} />
                    </label>
                    <label>
                        {i18n('secret')}
                        <input type="password" value={secret} onKeyDown={this.onKeyDown} onChange={this.handleChange('secret')} />
                    </label>

                    <div className="help"><a target="_blank" href={i18n('basic_auth.help_url')}>{i18n('help')}</a></div>

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
            event.stopPropagation();
        },

        handleChange(stateField) {
            return event => {
                this.setState({ [stateField]: event.target.value })
            }
        },

        connect() {
            const { accessKey, secret } = this.state;
            this.props.onConnect({ accessKey, secret });
        }
    });

    return BasicAuth;
});


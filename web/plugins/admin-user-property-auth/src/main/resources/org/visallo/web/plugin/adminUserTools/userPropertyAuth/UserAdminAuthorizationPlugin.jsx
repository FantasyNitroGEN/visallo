define([
    'react',
    'public/v1/api',
    'components/Alert'
], function (React,
             visallo,
             Alert) {

    const UserAdminAuthorizationPlugin = React.createClass({
        propTypes: {
            // The user for which the authorizations will be edited
            user: React.PropTypes.shape({
                userName: React.PropTypes.string.isRequired,
                authorizations: React.PropTypes.array.isRequired
            })
        },

        dataRequest: null,

        getInitialState() {
            return {
                error: null,
                authorizations: this.props.user.authorizations,
                saveInProgress: false,
                addAuthorizationValue: ''
            };
        },

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                })
        },

        handleAddAuthorizationSubmit(e) {
            e.preventDefault();
            this.addAuthorization(this.state.addAuthorizationValue);
        },

        addAuthorization(authorization){
            this.setState({
                saveInProgress: true
            });

            const newAuthorizations = [...this.state.authorizations, authorization];

            this.dataRequest('com-visallo-userAdminAuthorization', 'userAuthAdd', this.props.user.userName, authorization)
                .then(() => {
                    this.setState({
                        addAuthorizationValue: '',
                        authorizations: newAuthorizations,
                        saveInProgress: false,
                        error: null
                    });
                })
                .catch((e) => {
                    this.setState({error: e, saveInProgress: false});
                });
        },

        handleAuthorizationDeleteClick(authorization) {
            this.setState({
                saveInProgress: true
            });

            const newAuthorizations = this.state.authorizations.filter((a)=>a !== authorization);

            this.dataRequest('com-visallo-userAdminAuthorization', 'userAuthRemove', this.props.user.userName, authorization)
                .then(() => {
                    this.setState({
                        authorizations: newAuthorizations,
                        saveInProgress: false,
                        error: null
                    });
                })
                .catch((e) => {
                    this.setState({error: e});
                });
        },

        handleAlertDismiss() {
            this.setState({
                error: null
            });
        },

        handleAddAuthorizationInputChange(e) {
            this.setState({
                addAuthorizationValue: e.target.value
            });
        },

        render() {
            return (
                <div>
                    <div className="nav-header">{i18n('admin.user.editor.userAdminAuthorization.authorizations')}</div>
                    <Alert error={this.state.error} onDismiss={this.handleAlertDismiss}/>
                    <ul>
                        { this.state.authorizations.map((auth) => (
                            <li key={auth} className="auth-item highlight-on-hover">
                                <button className="btn btn-mini btn-danger show-on-hover"
                                        onClick={()=>this.handleAuthorizationDeleteClick(auth)}
                                        disabled={this.state.saveInProgress}>
                                    {i18n('admin.user.editor.userAdminAuthorization.deleteAuthorization')}
                                </button>
                                <span style={{lineHeight: '1.2em'}}>{auth}</span>
                            </li>
                        )) }
                    </ul>

                    <form onSubmit={this.handleAddAuthorizationSubmit}>
                        <input style={{marginTop: '0.5em'}}
                               className="auth"
                               placeholder={i18n('admin.user.editor.userAdminAuthorization.addAuthorizationPlaceholder')}
                               type="text"
                               value={this.state.addAuthorizationValue}
                               onChange={this.handleAddAuthorizationInputChange}
                               disabled={this.state.saveInProgress}/>
                        <button
                            disabled={this.state.saveInProgress}>
                            {i18n('admin.user.editor.userAdminAuthorization.addAuthorization')}
                        </button>
                    </form>
                </div>
            );
        }
    });

    return UserAdminAuthorizationPlugin;
});

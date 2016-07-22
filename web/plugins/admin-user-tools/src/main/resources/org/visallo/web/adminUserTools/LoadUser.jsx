define([
    'react',
    'public/v1/api',
    './UserTypeaheadInput',
    'components/Alert'
], function (React,
             visallo,
             UserTypeaheadInput,
             Alert) {

    const LoadUser = React.createClass({
        dataRequest: null,

        getInitialState() {
            return {
                username: null,
                error: null
            };
        },

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });
        },

        componentWillReceiveProps(nextProps) {
            if (!this.props.reload && nextProps.reload) {
                this.reloadUser();
            }
        },

        handleUsernameChange(username) {
            this.setState({
                username: username,
                error: null
            });
        },

        handleUsernameSelected(username) {
            this.setState({
                username: username
            });
            this.loadUser(username);
        },

        handleFormSubmit(e) {
            e.preventDefault();
            this.loadUser(this.state.username);
        },

        reloadUser() {
            this.loadUser(this.state.username);
        },

        loadUser(username) {
            this.dataRequest('user', 'get', username)
                .then((user) => {
                    this.setState({error: null});
                    this.props.onUserLoaded(user);
                })
                .catch((e) => {
                    this.setState({error: e});
                })
        },

        handleAlertDismiss() {
            this.setState({
                error: null
            });
        },

        render() {
            return (
                <form onSubmit={this.handleFormSubmit}>
                    <Alert error={this.state.error} onDismiss={this.handleAlertDismiss}/>
                    <div className="nav-header">{i18n('admin.user.editor.username')}</div>
                    <UserTypeaheadInput username={this.state.username} onChange={this.handleUsernameChange}
                                        onSelected={this.handleUsernameSelected}/>
                    <button className="btn">{i18n('admin.user.editor.loadUser')}</button>
                </form>
            );
        }
    });

    return LoadUser;
});
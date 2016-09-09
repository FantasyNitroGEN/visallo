define([
    'react',
    'public/v1/api',
    './UserTypeaheadInput',
    'components/Alert'
], function(React,
            visallo,
            UserTypeaheadInput,
            Alert) {

    const LoadUser = React.createClass({
        dataRequest: null,

        getInitialState() {
            return {
                user: null,
                userInput: '',
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

        handleUserInputChange(userInput) {
            this.setState({
                user: null,
                userInput: userInput,
                error: null
            });
        },

        handleUserSelected(user) {
            this.setState({
                user: user,
                error: null
            });
            this.loadUser(user);
        },

        handleFormSubmit(e) {
            e.preventDefault();
            this.loadUser(this.state.user);
        },

        reloadUser() {
            this.loadUser(this.state.user);
        },

        loadUser(user) {
            const userName = user ? user.userName : this.state.userInput;
            this.dataRequest('user', 'get', userName)
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
                    <UserTypeaheadInput user={this.state.user}
                                        onInputChange={this.handleUserInputChange}
                                        onUserSelected={this.handleUserSelected}/>
                    <button className="btn">{i18n('admin.user.editor.loadUser')}</button>
                </form>
            );
        }
    });

    return LoadUser;
});
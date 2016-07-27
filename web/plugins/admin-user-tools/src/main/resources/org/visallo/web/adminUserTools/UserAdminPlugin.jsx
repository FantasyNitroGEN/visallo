define([
    'react',
    'public/v1/api',
    './WorkspaceList',
    './LoadUser'
], function (React,
             visallo,
             WorkspaceList,
             LoadUser) {

    const UserAdminPlugin = React.createClass({
        dataRequest: null,

        getInitialState() {
            return {
                user: null,
                reloadUser: false
            };
        },

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });

            const privilegesPlugins = visallo.registry.extensionsForPoint('org.visallo.admin.user.privileges');
            if (privilegesPlugins.length != 1) {
                throw new Error('Exactly one "org.visallo.admin.user.privileges" is required. Found: ' + privilegesPlugins.length);
            }

            const authorizationsPlugins = visallo.registry.extensionsForPoint('org.visallo.admin.user.authorizations');
            if (authorizationsPlugins.length != 1) {
                throw new Error('Exactly one "org.visallo.admin.user.authorizations" is required. Found: ' + authorizationsPlugins.length);
            }

            Promise.all([privilegesPlugins[0].componentPath, authorizationsPlugins[0].componentPath]
                .map(Promise.require))
                .spread((PrivilegesComponent, AuthorizationsComponent) => {
                    this.PrivilegesPlugin = PrivilegesComponent;
                    this.AuthorizationsPlugin = AuthorizationsComponent;
                });
        },

        handleUserLoaded(user) {
            this.setState({
                user: user,
                reloadUser: false,
                disableDelete: user.id === visalloData.currentUser.id
            });
        },

        handleWorkspaceChanged() {
            this.setState({
                reloadUser: true
            });
        },

        handleUserDeleted() {
            var self = this;
            this.dataRequest('admin', 'userDelete', this.state.user.userName)
                .then(function() {
                    self.setState({
                        user: null,
                        reloadUser: false
                    });
                });
        },

        render() {
            return (
                <div className="user-admin">
                    <LoadUser
                        reload={this.state.reloadUser}
                        username={this.state.user ? this.state.user.userName : ''}
                        onUserLoaded={this.handleUserLoaded}/>
                    { this.state.user ? (
                        <div>
                            <div className="nav-header">{i18n('admin.user.editor.info.header')}</div>
                            <ul>
                                <li>
                                    <label className="nav-header">{i18n('admin.user.editor.info.id')}</label>
                                    <span>{this.state.user.id}</span>
                                </li>
                                <li>
                                    <label className="nav-header">{i18n('admin.user.editor.info.email')}</label>
                                    <span>{this.state.user.email || i18n('admin.user.editor.notSet')}</span>
                                </li>
                                <li>
                                    <label className="nav-header">{i18n('admin.user.editor.info.displayName')}</label>
                                    <span>{this.state.user.displayName || i18n('admin.user.editor.notSet')}</span>
                                </li>
                                <li>
                                    <label className="nav-header">{i18n('admin.user.editor.info.status')}</label>
                                    <span>{this.state.user.status}</span>
                                </li>
                            </ul>

                            <div>
                                <this.PrivilegesPlugin user={this.state.user}/>
                            </div>

                            <div>
                                <this.AuthorizationsPlugin user={this.state.user}/>
                            </div>

                            <div>
                                <WorkspaceList user={this.state.user}
                                               onWorkspaceChanged={this.handleWorkspaceChanged}
                                />
                            </div>
                            <div>
                                <button
                                    className="btn btn-danger"
                                    disabled={this.state.disableDelete}
                                    onClick={this.handleUserDeleted}
                                >
                                    {i18n('admin.user.editor.deleteUser')}
                                </button>
                            </div>
                        </div>
                    ) : null }
                </div>
            );
        }
    });

    return UserAdminPlugin;
});

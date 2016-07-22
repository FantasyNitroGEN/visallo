define([
    'react',
    'public/v1/api',
    'components/Alert'
], function (React, visallo, Alert) {

    const WorkspaceList = React.createClass({
        propTypes: {
            // The user for which the workspaces will be edited
            user: React.PropTypes.shape({
                userName: React.PropTypes.string.isRequired,
                workspaces: React.PropTypes.array.isRequired
            }),

            // Callback when a workspace is shared or deleted
            onWorkspaceChanged: React.PropTypes.func.isRequired
        },

        dataRequest: null,

        getInitialState() {
            return {
                error: null
            };
        },

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });
        },

        handleShareWithMeClick(workspace) {
            const workspaceId = workspace.workspaceId;
            this.dataRequest('admin', 'workspaceShare', workspaceId, this.props.user.userName)
                .then(()=> {
                    this.setState({error: null});
                    this.props.onWorkspaceChanged();
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

        renderWorkspace(workspace) {
            return (
                <li key={workspace.workspaceId} className="highlight-on-hover">
                    <button className="share show-on-hover btn btn-mini btn-primary"
                            onClick={()=>this.handleShareWithMeClick(workspace)}>
                        {i18n('admin.user.editor.shareWorkspace')}
                    </button>
                    <span className="nav-list-title">{workspace.title} {workspace.isCurrent ? '(current)' : ''}</span>
                    <span className="nav-list-subtitle">{workspace.workspaceId}</span>
                    <ul className="inner-list">
                        {
                            workspace.users.map((workspaceUser) => {
                                return (
                                    <li key={workspace.workspaceId + '-'+workspaceUser.userId}
                                        className="nav-list-subtitle">{workspaceUser.access}: {workspaceUser.userId}</li>
                                );
                            })
                        }
                    </ul>
                </li>
            );
        },

        render() {
            return (
                <div>
                    <div className="nav-header">{i18n('admin.user.editor.workspaces.header')}</div>
                    <Alert error={this.state.error} onDismiss={this.handleAlertDismiss}/>
                    <ul>
                        {
                            this.props.user.workspaces.map((workspace) => {
                                return this.renderWorkspace(workspace);
                            })
                        }
                    </ul>
                </div>
            );
        }
    });

    return WorkspaceList;
});

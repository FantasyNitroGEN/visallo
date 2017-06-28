define([
    'create-react-class',
    'prop-types',
    'public/v1/api',
    'util/requirejs/promise!util/service/propertiesPromise',
    'components/Alert'
], function(
    createReactClass,
    PropTypes
    visallo,
    configProperties,
    Alert) {

    const UserAdminPrivilegesPlugin = createReactClass({
        propTypes: {
            // The user for which the authorizations will be edited
            user: PropTypes.shape({
                userName: PropTypes.string.isRequired,
                privileges: PropTypes.array.isRequired
            })
        },

        dataRequest: null,

        getInitialState() {
            const privileges = configProperties.privileges.reduce((obj, p)=> {
                obj[p] = _.contains(this.props.user.privileges, p);
                return obj;
            }, {});

            return {
                privileges: privileges,
                saveInProgress: false,
                error: null
            };
        },

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                })
        },

        handleCheckboxChange(priv) {
            this.setState({
                saveInProgress: true
            });

            const newPrivileges = {
                ...this.state.privileges,
                [priv]: !this.state.privileges[priv]
            };

            const privilegesList = Object.keys(newPrivileges)
                .filter(priv=>newPrivileges[priv])
                .join(',');

            this.dataRequest('com-visallo-userAdminPrivileges', 'userUpdatePrivileges', this.props.user.userName, privilegesList)
                .then(() => {
                    this.setState({
                        privileges: newPrivileges,
                        saveInProgress: false,
                        error: null
                    });
                })
                .catch((e) => {
                    this.setState({error: e, saveInProgress: false});
                });
        },

        renderPrivilege(priv, hasPrivilege) {
            return (
                <li key={priv}>
                    <label>
                        <input value={priv} type="checkbox" onChange={()=>this.handleCheckboxChange(priv)}
                               disabled={this.state.saveInProgress}
                               checked={hasPrivilege}/> {priv}
                    </label>
                </li>
            )
        },

        handleAlertDismiss() {
            this.setState({
                error: null
            });
        },

        render() {
            return (
                <div>
                    <div className="nav-header">{i18n('admin.user.editor.userAdminPrivilege.privileges')}</div>
                    <Alert error={this.state.error} onDismiss={this.handleAlertDismiss}/>
                    <ul>
                        { Object.keys(this.state.privileges).sort().map((priv) => {
                            return this.renderPrivilege(priv, this.state.privileges[priv]);
                        }) }
                    </ul>
                </div>
            );
        }
    });

    return UserAdminPrivilegesPlugin;
});

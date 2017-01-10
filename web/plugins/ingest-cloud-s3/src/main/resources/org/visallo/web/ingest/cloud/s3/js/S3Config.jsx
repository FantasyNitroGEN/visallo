define([
    'react',
    './Browser',
    './CredentialsChooser'
], function(
    React,
    Browser,
    CredentialsChooser) {

    const S3Configuration = React.createClass({
        getInitialState() {
            return {
                changeCredentials: false
            };
        },

        onChangeCredentials() {
            this.setState({
                changeCredentials: true
            });
        },

        componentWillReceiveProps(nextProps) {
            if (this.props.auth !== nextProps.auth) {
                this.setState({
                    changeCredentials: false
                });
            }
        },

        render() {
            const { authenticated } = this.props;
            const { changeCredentials } = this.state;

            return authenticated && !changeCredentials ?
                <Browser onChangeCredentials={this.onChangeCredentials} {...({onOpenDirectory, onRefreshDirectories, onSelectItem, onImport, contentsByDir, cwd} = this.props)} /> :
                <CredentialsChooser {...({onConnect, loading, errorMessage, authenticationId} = this.props)} />
        }
    });

    return S3Configuration;
});


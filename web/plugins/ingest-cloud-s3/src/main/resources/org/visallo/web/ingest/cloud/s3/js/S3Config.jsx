define([
    'react',
    './Browser',
    './CredentialsChooser'
], function(
    React,
    Browser,
    CredentialsChooser) {

    const S3Configuration = function(props) {
        const { authenticated } = props;

        return authenticated ?
            <Browser {...({onOpenDirectory, onRefreshDirectories, onSelectItem, onImport, contentsByDir, cwd} = props)} /> :
            <CredentialsChooser {...({onConnect, loading, errorMessage, authenticationId} = props)} />
    };

    return S3Configuration;
});


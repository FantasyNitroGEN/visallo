define([
    'react',
    './i18n',
    'components/RegistryInjectorHOC',
    'components/Attacher'
], function(React, i18n, RegistryInjectorHOC, Attacher) {
    'use strict';

    const DEFAULT_AUTH_ID = 'basic_auth'
    const AUTH_EXTENSION_POINT = 'org.visallo.ingest.cloud.s3.auth';
    RegistryInjectorHOC.registry.documentExtensionPoint(
        AUTH_EXTENSION_POINT,
        'Provide credential managers to S3 ingest',
        function(e) {
            return _.every(
                ['id', 'componentPath', 'displayName', 'providerClass'],
                p => _.isString(e[p]) && e[p]
            );
        }
    );
    RegistryInjectorHOC.registry.registerExtension(AUTH_EXTENSION_POINT, {
        id: 'basic_auth',
        displayName: i18n('basic_auth'),
        componentPath: 'org/visallo/web/ingest/cloud/s3/dist/BasicAuth',
        providerClass: 'org.visallo.web.ingest.cloud.s3.authentication.BasicAuthProvider'
    });
    RegistryInjectorHOC.registry.registerExtension(AUTH_EXTENSION_POINT, {
        id: 'session_auth',
        displayName: i18n('session_auth'),
        componentPath: 'org/visallo/web/ingest/cloud/s3/dist/SessionAuth',
        providerClass: 'org.visallo.web.ingest.cloud.s3.authentication.SessionAuthProvider'
    });

    const PropTypes = React.PropTypes;
    const CredentialsChooser = React.createClass({

        propTypes: {
            authenticationId: PropTypes.string,
            onConnect: PropTypes.func.isRequired,
            errorMessage: PropTypes.string,
            loading: PropTypes.bool
        },

        getInitialState() {
            return {
                selected: this.props.authenticationId || DEFAULT_AUTH_ID,
                hideError: false
            }
        },

        render() {
            const { registry, errorMessage, loading } = this.props;
            const { selected, hideError } = this.state;
            const types = _.sortBy(registry[AUTH_EXTENSION_POINT], r => r.displayName.toLocaleLowerCase());
            const selectedType = _.findWhere(types, { id: selected });

            return (
                <div className="import-s3-credentials">
                    <select defaultValue={selected} onChange={this.onChange}>
                        {types.length === 1 ? null : (
                            <option value=''>{i18n(types.length ? 'credentials' : 'nocredentials')}</option>
                        )}
                        {types.map(r => (<option key={r.id} value={r.id}>{r.displayName}</option>)) }
                    </select>

                    {selectedType ? (
                        <Attacher behavior={{onConnect: _.partial(this.onConnect, _, selectedType.providerClass)}}
                                  loading={loading}
                                  errorMessage={hideError ? null : errorMessage}
                                  componentPath={selectedType.componentPath} />
                    ) : null}
                </div>
            );
        },

        onChange(event) {
            const id = event.target.value;
            this.setState({ selected: id, hideError: true })
        },

        onConnect(attacher, providerClass, credentials) {
            this.props.onConnect(providerClass, credentials)
            this.setState({ hideError: false })
        }

    });

    return RegistryInjectorHOC(CredentialsChooser, [AUTH_EXTENSION_POINT]);
});

define([
    'react',
    'configuration/plugins/registry'
], function(React, registry) {
    'use strict';

    const { PropTypes } = React;
    const RegistryInjectorHOC = (WrappedComponent, identifiers) => {
        if (!_.isArray(identifiers)) throw new Error('identifiers must be an array');

        return React.createClass({
            displayName: `RegistryInjectorHOC(${WrappedComponent.displayName || 'Component'})`,
            componentDidMount() {
                if (identifiers.length === 0) {
                    console.warn('RegistryInjectorHOC invoked with no identifiers')
                }
                $(document).on('extensionsChanged.registryInjector', (event, {extensionPoint}) => {
                    if (identifiers.includes(extensionPoint)) {
                        this.forceUpdate();
                    }
                });
            },
            componentWillUnmount() {
                $(document).off('extensionsChanged.registryInjector');
            },
            render() {
                const registrySubset = _.object(identifiers.map(i => [i, registry.extensionsForPoint(i)]));
                return (<WrappedComponent ref="wrapped" {...this.props} registry={registrySubset} />);
            }
        })
    };

    RegistryInjectorHOC.registry = registry;

    return RegistryInjectorHOC;
});

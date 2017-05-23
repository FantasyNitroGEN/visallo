define([
    'react',
    'configuration/plugins/registry',
    './BooleanSetting'
], function(React, registry, BooleanSetting) {
    'use strict';

    /**
     * Adds new settings to the general settings page
     *
     * @param {string} identifier Unique identifier for this setting
     * @param {string} group The group i18n message id
     * @param {string} displayName The display name i18n message id
     * @param {string} type One of 'boolean', 'custom'
     * @param {string=} componentPath Required when type=custom
     * @param {string=} uiPreferenceName Required when type=boolean, if getInitialValue and onChange are not provided
     * @param {string=} getInitialValue Required when type=boolean, if uiPreferenceName is not provided
     * @param {string=} onChange Required when type=boolean, if uiPreferenceName is not provided
     */
    registry.documentExtensionPoint('org.visallo.user.account.page.setting',
        'Add new settings to the general settings page',
        function(e) {
            if (!(('identifier' in e) && ('group' in e) && ('displayName' in e) && ('type' in e))) {
                return false;
            }
            switch (e.type) {
                case 'boolean':
                    return ('uiPreferenceName' in e) || (('getInitialValue' in e) && ('onChange' in e));

                case 'custom':
                    return ('componentPath' in e);

                default:
                    return false;
            }
        },
        'http://docs.visallo.org/extension-points/front-end/userAccount'
    );

    const TIMEZONE_SETTING = {
        identifier: 'org.visallo.user.account.page.setting.timezone',
        group: 'useraccount.page.settings.setting.group.locale',
        displayName: 'org.visallo.user.account.page.setting.timezone.displayName',
        type: 'custom',
        componentPath: 'workspaces/userAccount/bundled/settings/TimeZoneSetting'
    };

    const SettingsSetting = React.createClass({
        getInitialState() {
            return {
                component: null
            };
        },

        componentWillMount() {
            this.update(this.props);
        },

        componentWillReceiveProps(nextProps) {
            this.update(nextProps);
        },

        update(props) {
            if (props.setting) {
                const setting = props.setting;
                switch (setting.type) {
                    case 'boolean':
                        this.setState({
                            component: BooleanSetting
                        });
                        break;

                    case 'custom':
                        if (setting.componentPath) {
                            require([setting.componentPath], component => {
                                this.setState({
                                    component: component
                                });
                            });
                        } else {
                            console.error('custom settings must include "componentPath"');
                        }
                        break;

                    default:
                        console.error(`invalid setting type "${setting.type}"`);
                        break;
                }
            }
        },

        render() {
            return (<li className="setting">
                {this.state.component ? (<this.state.component setting={this.props.setting}/>) : (<div></div>)}
            </li>);
        }
    });

    const SettingsGroup = React.createClass({
        render() {
            const settings = _.sortBy(this.props.settings, s => i18n(s.displayName));

            return (<li>
                <h4 className="settings-group-title">{i18n(this.props.groupKey)}</h4>
                <ul className="settings-group">
                    {settings.map(setting => {
                        return (<SettingsSetting setting={setting}/>);
                    })}
                </ul>
            </li>);
        }
    });

    const Settings = React.createClass({
        getSettingsExtensions() {
            const settings = registry.extensionsForPoint('org.visallo.user.account.page.setting');

            if (!_.findWhere(settings, {identifier: TIMEZONE_SETTING.identifier})) {
                registry.registerExtension('org.visallo.user.account.page.setting', TIMEZONE_SETTING);
                settings.push(TIMEZONE_SETTING);
            }

            return settings;
        },

        render() {
            const settings = this.getSettingsExtensions();
            const groups = _.groupBy(settings, s => s.group);
            const groupKeys = _.sortBy(Object.keys(groups), key => i18n(key));

            return (
                <ul className="general-settings">
                    {groupKeys.map(groupKey => {
                        return (<SettingsGroup groupKey={groupKey} settings={groups[groupKey]}/>);
                    })}
                </ul>
            );
        }
    });

    return Settings;
});

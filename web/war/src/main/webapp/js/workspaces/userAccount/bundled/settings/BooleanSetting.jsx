define([
    'react',
    'configuration/plugins/registry',
    'util/withDataRequest',
    'util/parsers'
], function (React, registry, withDataRequest, P) {
    'use strict';

    const BooleanSetting = React.createClass({
        getInitialState() {
            return {
                disabled: true,
                value: false
            }
        },

        componentWillMount() {
            if (this.props.setting.uiPreferenceName) {
                this.setState({
                    disabled: false,
                    value: P.bool.parse(visalloData.currentUser.uiPreferences[this.props.setting.uiPreferenceName])
                });
            } else if (this.props.setting.getInitialValue) {
                Promise.resolve(this.props.setting.getInitialValue())
                    .then(value => {
                        this.setState({
                            disabled: false,
                            value: P.bool.parse(value)
                        });
                    });
            } else {
                console.error('boolean properties require a "uiPreferenceName" or "getInitialValue"');
            }
        },

        handleCheckboxChange(evt) {
            const checked = evt.target.checked;
            this.setState({
                disabled: true
            });
            this.handleChange(checked)
                .then(newValue => {
                    this.setState({
                        disabled: false,
                        value: P.bool.parse(newValue)
                    });
                });
        },

        handleChange(value) {
            const onChange = this.props.setting.onChange || Promise.resolve;
            return onChange(value)
                .then(value => {
                    if (this.props.setting.uiPreferenceName) {
                        value = value.toString();
                        return withDataRequest.dataRequest('user', 'preference', this.props.setting.uiPreferenceName, value)
                            .then(() => {
                                visalloData.currentUser.uiPreferences[this.props.setting.uiPreferenceName] = value;
                                return value;
                            });
                    } else {
                        return value;
                    }
                });
        },

        render() {
            return (<label>
                <div className="display-name">{i18n(this.props.setting.displayName)}</div>
                <input type="checkbox" disabled={this.state.disabled} value={this.state.value}
                       onChange={this.handleCheckboxChange}/>
            </label>);
        }
    });

    return BooleanSetting;
});

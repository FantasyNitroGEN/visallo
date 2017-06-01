define([
    'react',
    'jstz',
    'util/formatters'
], function (React, jstz, F) {
    'use strict';

    const TimeZoneDropDown = React.createClass({
        propTypes: {
            value: React.PropTypes.string.isRequired,
            onChange: React.PropTypes.func.isRequired,
            disabled: React.PropTypes.bool
        },

        handleTimeZoneChange(evt) {
            const name = evt.target.value;
            this.props.onChange(name);
        },

        render() {
            const disabled = 'disabled' in this.props ? this.props.disabled : false;

            return (<select disabled={disabled} onChange={this.handleTimeZoneChange}
                            value={this.props.value}>
                {Object.keys(jstz.olson.timezones).map(key => {
                    const tz = jstz.olson.timezones[key];
                    const tzInfo = F.timezone.lookupTimezone(tz);
                    const components = key.split(',');
                    const offsetMinutes = parseInt(components[0], 10);
                    const offset = F.timezone.offsetDisplay(offsetMinutes);
                    const tzPretty = tz.replace(/_/g, ' ');
                    return (<option value={tz}>{offset} {tzPretty}{tzInfo.dst ? '*' : ''}</option>);
                })}
            </select>);
        }
    });

    return TimeZoneDropDown;
});

define([
    'react',
    './ConceptSelector'
], function(React, ConceptsSelector) {
    'use strict';

    const PropTypes = React.PropTypes;
    const PropertyForm = React.createClass({
        propTypes: {
        },
        getInitialState() {
            return {
                type: 'string'
            };
        },
        getValue() {
            const { displayName } = this.state;
            const { displayName: defaultValue } = this.props;
            return _.isString(displayName) ? displayName : defaultValue;
        },
        render() {
            const value = this.getValue();
            const disabled = _.isEmpty(value);
            return (
                <div>
                    <input type="text"
                        onChange={this.onDisplayNameChange}
                        value={value} />

                    <label>Concept</label>
                    { /* TODO limit to current concept's hierarchy */ }
                    <ConceptsSelector
                        value={this.state.domain}
                        creatable={false}
                        onSelected={this.onConceptSelected} />

                    <label>Type</label>
                    <select value={this.state.type} onChange={this.handleTypeChange}>
                        <option value="string">String</option>
                        <option value="integer">Integer</option>
                        <option value="double">Double</option>
                        <option value="currency">Currency</option>
                        <option value="dateOnly">Date</option>
                        <option value="datetime">Datetime</option>
                        <option value="geolocation">Geolocation</option>
                        <option value="duration">Duration</option>
                        <option value="link">Link</option>
                        <option value="bytes">Bytes</option>
                    </select>

                    <div style={{textAlign: 'right'}}>
                    <button
                        disabled={disabled}
                        onClick={this.onCreate}
                        className="btn btn-small btn-primary"
                        style={{ width: 'auto', marginBottom: '1em'}}>{
                            disabled ? 'Create' : `Create "${value}"`
                        }</button>
                    </div>
                </div>
            )
        },
        onConceptSelected(option) {
            this.setState({ domain: option ? option.title : null })
        },
        onDisplayNameChange(event) {
            this.setState({ displayName: event.target.value })
        },
        handleTypeChange(event) {
            this.setState({ type: event.target.value });
        },
        onCreate() {
            this.props.onCreate({
                domain: this.state.domain,
                type: this.state.type,
                displayName: this.getValue()
            })
        }
    });

    return PropertyForm;
});

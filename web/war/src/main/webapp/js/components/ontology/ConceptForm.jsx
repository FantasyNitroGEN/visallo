define([
    'react',
    './Concepts'
], function(React, ConceptsSelector) {
    'use strict';

    const PropTypes = React.PropTypes;
    const ConceptForm = React.createClass({
        propTypes: {
        },
        getInitialState() {
            return {}
        },
        getValue() {
            const { displayName } = this.state;
            const { displayName: defaultValue } = this.props;
            const value = _.isString(displayName) ? displayName : defaultValue;
            return value;
        },
        render() {
            const value = this.getValue();
            const disabled = _.isEmpty(value);
            return (
                <div>
                    <input type="text"
                        onChange={this.onDisplayNameChange}
                        value={value} />

                    <label>Parent Concept</label>
                    <ConceptsSelector
                        value={this.state.parentConcept}
                        creatable={false}
                        onSelected={this.onConceptSelected} />

                    <label>Icon</label>
                    <select><option>Choose Icon...</option></select>

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
            this.setState({ parentConcept: option.title })
        },
        onDisplayNameChange(event) {
            this.setState({ displayName: event.target.value })
        },
        onCreate() {
            this.props.onCreate({
                parentConcept: this.state.parentConcept,
                displayName: this.getValue()
            })
        }
    });

    return ConceptForm;
});

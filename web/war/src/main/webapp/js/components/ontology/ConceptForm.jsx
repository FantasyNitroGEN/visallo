define([
    'react',
    './ConceptSelector',
    '../GlyphSelector'
], function(React, ConceptsSelector, GlyphSelector) {
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
            return _.isString(displayName) ? displayName : defaultValue;
        },
        render() {
            const value = this.getValue();
            const disabled = _.isEmpty(value);
            return (
                <div>
                    <input type="text"
                        placeholder="Display Name"
                        onChange={this.onDisplayNameChange}
                        value={value} />

                    <ConceptsSelector
                        value={this.state.parentConcept}
                        placeholder="Concept to Inherit (optional)"
                        creatable={false}
                        onSelected={this.onConceptSelected} />

                    <GlyphSelector search={value} onSelected={this.onIconSelected} />

                    <div style={{textAlign: 'right'}}>
                    <button
                        onClick={this.props.onCancel}
                        className="btn btn-link btn-small"
                        style={{ width: 'auto', marginBottom: '1em'}}>Cancel</button>
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
        onIconSelected(imgSrc) {
            this.setState({ imgSrc })
        },
        onConceptSelected(option) {
            this.setState({ parentConcept: option ? option.title : null })
        },
        onDisplayNameChange(event) {
            this.setState({ displayName: event.target.value })
        },
        onCreate() {
            this.props.onCreate({
                parentConcept: this.state.parentConcept,
                displayName: this.getValue(),
                glyphIconHref: this.state.imgSrc
            })
        }
    });

    return ConceptForm;
});

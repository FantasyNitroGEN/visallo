define([
    'react',
    './ConceptSelector',
    '../GlyphSelector',
    '../ColorSelector'
], function(React, ConceptsSelector, GlyphSelector, ColorSelector) {

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
            const { color } = this.state;
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

                    <ColorSelector value={color} onSelected={this.onColorSelected} />
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
        onColorSelected(color) {
            this.setState({ color })
        },
        onIconSelected(imgSrc) {
            this.setState({ imgSrc })
        },
        onConceptSelected(option) {
            const newState = { parentConcept: null, color: null };
            if (option) {
                newState.color = option.color;
                newState.parentConcept = option.title;
            }

            this.setState(newState);
        },
        onDisplayNameChange(event) {
            this.setState({ displayName: event.target.value })
        },
        onCreate() {
            const { parentConcept, color, imgSrc } = this.state;
            this.props.onCreate({
                parentConcept: parentConcept,
                displayName: this.getValue(),
                glyphIconHref: imgSrc,
                color: color || 'rgb(0,0,0)'
            })
        }
    });

    return ConceptForm;
});

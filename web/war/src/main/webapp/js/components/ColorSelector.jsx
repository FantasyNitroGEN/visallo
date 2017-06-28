define([
    'create-react-class',
    'prop-types',
    'colorjs'
], function(createReactClass, PropTypes, colorjs) {

    const BLACK = 'rgb(0,0,0)';
    const saturation = 0.7;
    const lightness = 0.5;
    const shades = [
        { s: 21, v: 'Red' },
        { s: 52, v: 'Orange' },
        { s: 68, v: 'Yellow' },
        { s: 154, v: 'Green' },
        { s: 190, v: 'Teal' },
        { s: 249, v: 'Blue' },
        { s: 290, v: 'Purple' },
        { s: 330, v: 'Pink' },
        { s: 361, v: 'Red' }
    ];
    const ColorSelector = createReactClass({
        propTypes: {
            onSelected: PropTypes.func.isRequired,
            value: PropTypes.string
        },
        getDefaultProps() {
            return { value: BLACK };
        },
        getInitialState() {
            return { value: this.props.value };
        },
        componentDidMount() {
            this.publish = _.debounce(this._publish, 100);
        },
        componentWillReceiveProps(nextProps) {
            if (this.state.value !== nextProps.value) {
                this.setState({ value: nextProps.value || BLACK })
            }
        },
        render() {
            const { value, colorTooltip } = this.state;
            const black = this.isBlack();
            const color = colorjs(value);
            const hue = color.getHue();
            const colorStyle = `hsl(${hue}, ${saturation * 100}%, ${lightness * 100}%);`
            const style = black ? '' : `
                .color-selector input[type=range]::-moz-range-thumb { background: ${colorStyle} }
                .color-selector input[type=range]::-ms-thumb { background: ${colorStyle} }
                .color-selector input[type=range]::-webkit-slider-thumb { background: ${colorStyle} }`;
            const percent = hue / 360;
            const shade = black ? '' : _.find(shades, s => hue < s.s).v;

            return (
                <div className={`color-selector ${black ? 'black' : ''}`} style={{ display: 'flex' }}>
                    <div title="Set to Black" className="black">
                        <button onClick={this.onClickBlack} onMouseDown={this.onMouseDownBlack}>Set to Black</button>
                    </div>
                    <div title="Set to Color" className="gradient" style={{position: 'relative'}}>
                        <style>{style}</style>
                        <input value={hue} min="0" max="359" step="1" onChange={this.onChange} type="range" />
                        { black || !colorTooltip ? null : (
                            <div className="tooltip bottom" style={{
                                opacity: 1,
                                left: percent * 100 + '%',
                                marginLeft: ((1 - percent) * (11 * 2) - 11) + 'px',
                                top: '100%',
                                transform: 'translate(-50%, 0px)'
                            }}>
                                <div className="tooltip-arrow"></div>
                                <div style={{ background: 'black' }} className="tooltip-inner">{shade}</div>
                            </div>
                        )}
                    </div>
                </div>
            )
        },
        isBlack() {
            return this.state.value === BLACK;
        },
        onClickBlack() {
            if (!this.isBlack()) {
                this.update(BLACK);
            }
        },
        onMouseDownBlack() {
            if (!this.isBlack()) {
                this.update(BLACK);
            }
        },
        onChange(event) {
            const color = colorjs({ hue: event.target.value, saturation, lightness }).toCSSHex();
            this.update(color);
            clearTimeout(this._hideTooltip);
            if (!this.state.colorTooltip) {
                this.setState({ colorTooltip: true })
            }
            this._hideTooltip = _.delay(() => {
                this.setState({ colorTooltip: false })
            }, 750);
        },
        update(newValue) {
            this.setState({ value: newValue });
            this.publish(newValue);
        },
        _publish(newValue) {
            this.props.onSelected(newValue);
        }
    });

    return ColorSelector;
});

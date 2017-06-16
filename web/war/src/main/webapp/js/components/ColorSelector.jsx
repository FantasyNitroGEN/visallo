define([
    'react',
    'colorjs'
], function(React, colorjs) {

    const saturation = 0.7;
    const lightness = 0.5;
    const PropTypes = React.PropTypes;
    const ColorSelector = React.createClass({
        propTypes: {
            value: PropTypes.shape({
                black: PropTypes.bool,
                hue: PropTypes.number
            }),
            onSelected: PropTypes.func.isRequired
        },
        getDefaultProps() {
            return { value: { black: true, hue: 360 / 2 } };
        },
        getInitialState() {
            return {};
        },
        componentDidMount() {
            this.publish = _.debounce(this._publish, 100);
        },
        render() {
            const { value, colorTooltip } = this.state;
            const { black, hue } = value || this.props.value;
            const color = { hue, saturation, lightness };

            const style = black ? '' : `
                .color-selector input[type=range]::-webkit-slider-thumb {
                    background: ${`hsl(${color.hue}, ${color.saturation * 100}%, ${color.lightness * 100}%)`};
                }`;
            const percent = hue / 360;
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
            const shade = black ? '' : _.find(shades, s => hue < s.s).v;
            return (
                <div className={`color-selector ${black ? 'black' : ''}`} style={{ display: 'flex' }}>
                    <div title="Set to Black" className="black">
                        <button onMouseDown={this.onMouseDown}>Set to Black</button>
                    </div>
                    <div title="Set to Color" className="gradient" style={{position: 'relative'}}>
                        <style>{style}</style>
                        <input data-color="Color" value={hue} min="0" max="360" step="1" onChange={this.onChange} type="range" />
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
        onMouseDown() {
            if (!(this.state.value || this.props.value).black) {
                const value = { black: true }
                this.update(value);
            }
        },
        onChange(event) {
            const value = { black: false, hue: event.target.value };
            this.update(value);
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
            var color = 'rgb(0,0,0)';
            if (!newValue.black) {
                color = colorjs({ hue: +newValue.hue, saturation, lightness }).toCSSHex();
            }
            this.props.onSelected(color);
        }
    });

    return ColorSelector;
});

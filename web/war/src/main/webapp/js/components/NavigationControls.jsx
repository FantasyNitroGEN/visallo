define([
    'react',
    './Attacher'
], function(React, Attacher) {
    'use strict';

    const PAN_INACTIVE_AREA = 8;
    const PAN_AREA_DRAG_SIZE = 75;
    const PAN_SPEED = 10;
	const PAN_MIN_PERCENT_SPEED = 0.25;
	const PAN_DISTANCE = 10;
    const STATE_PANNING = { state: 'panning' };
    const STATE_START = { state: 'panningStart' };
    const STATE_END = { state: 'panningEnd' };
    const EMPTY = { x: 0, y: 0 };

    const PropTypes = React.PropTypes;

    const NavigationControls = React.createClass({

        propTypes: {
            zoom: PropTypes.bool,
            pan: PropTypes.bool,
            tools: PropTypes.array,
            rightOffset: PropTypes.number,
            onZoom: PropTypes.func,
            onPan: PropTypes.func,
            onFit: PropTypes.func
        },

        getDefaultProps() {
            const noop = () => {}
            return {
                zoom: true,
                pan: true,
                tools: [],
                rightOffset: 0,
                onZoom: noop,
                onPan: noop,
                onFit: noop
            }
        },

        getInitialState() {
            return {
                panning: false,
                optionsOpen: false
            }
        },

        componentDidMount() {
            window.addEventListener('click', this.handleClick, false);
        },

        componentWillUnmount() {
            window.removeEventListener('click', this.handleClick);
        },

        handleClick(event) {
            if (this.state.optionsOpen && !this.refs.options.contains(event.target)) {
                this.setState({ optionsOpen: false });
            }
        },

        render() {
            const { optionsOpen } = this.state;
            const { tools, rightOffset } = this.props;
            const options = !_.isEmpty(tools) ?
                (
                    <div ref="options" className="options">
                        <button className={optionsOpen ? 'active' : ' '}
                            onClick={() => this.setState({ optionsOpen: !optionsOpen})}
                            title={i18n('controls.options.toggle')}>Options</button>
                        <div style={{display: (optionsOpen ? 'block' : 'none')}} className="options-container">
                            <ul>{
                                tools.map((tool, i) => {
                                    return <li key={tool.identifier}><Attacher componentPath={tool.optionComponentPath} {...tool.getProps()} /></li>
                                })
                            }</ul>
                        </div>
                    </div>
                ) : null,
                panningCls = 'panner' + (this.state.panning ? ' active' : ''),
                panningStyle = this.state.panning && this.state.pan ? {
                    background: `radial-gradient(circle at ${calculatePosition(this.state.pan)}, #575757, #929292 60%)`
                } : {}

            return (
                <div className="controls" style={{transform: `translate(-${rightOffset}px, 0)`}}>
                    <div className="tools">{options}</div>
                    <button onClick={this.onFit}>{i18n('controls.fit')}</button>
                    <div ref="panner" style={panningStyle} className={panningCls} onMouseDown={this.onPanMouseDown}><div className="arrow-bottom"/><div className="arrow-right"/><div className="arrow-top"/><div className="arrow-left"/></div>
                    <button
                        onMouseDown={this.onZoom}
                        onMouseUp={this.onZoom}
                        className="zoom" data-type="out">-</button>
                    <button
                        onMouseDown={this.onZoom}
                        onMouseUp={this.onZoom}
                        className="zoom" data-type="in">+</button>
                </div>
            );
        },

        onPanMouseDown(event) {
            this.props.onPan(EMPTY, STATE_START);
            this._pannerClientBounds = this.refs.panner.getBoundingClientRect();
            this._handlePanMove(event.nativeEvent);
            window.addEventListener('mousemove', this._handlePanMove, false);
            window.addEventListener('mouseup', this._handlePanUp, false);

            this.setState({ panning: true })
        },

        onFit(event) {
            this.props.onFit();
        },

        onZoom(event) {
            const e = event.nativeEvent;
            const zoomType = event.target.dataset.type;
            switch (e.type) {
                case 'mousedown':
                    this.zoomTimer = setInterval(() => {
                        this.props.onZoom(zoomType);
                    }, PAN_SPEED);
                    break;
                case 'mouseup':
                    clearInterval(this.zoomTimer);
                    break;
            }
        },

        _handlePanMove(event) {
            event.preventDefault();
            event.stopPropagation();
            clearInterval(this.panInterval);

            var pan = eventToPan(this._pannerClientBounds, event);
            if (isNaN(pan.x) || isNaN(pan.y)) {
                this.setState({ pan: null })
                return;
            }

            var self = this;
            this.panInterval = setInterval(() => {
                this.setState({ pan })
                this.props.onPan(pan, STATE_PANNING);
            }, PAN_SPEED);
        },

        _handlePanUp(event) {
            this.props.onPan(EMPTY, STATE_END);
            clearInterval(this.panInterval);
            window.removeEventListener('mousemove', this._handlePanMove);
            window.removeEventListener('mouseup', this._handlePanUp);
            this.setState({ panning: false })
        }
    });

    return NavigationControls;

    // Ported from jquery.cytoscape-panzoom plugin
    function eventToPan(bounds, e) {
        var v = {
                x: Math.round(e.pageX - bounds.left - bounds.width / 2),
                y: Math.round(e.pageY - bounds.top - bounds.height / 2)
            },
            r = PAN_AREA_DRAG_SIZE,
            d = Math.sqrt(v.x * v.x + v.y * v.y),
            percent = Math.min(d / r, 1);

        if (d < PAN_INACTIVE_AREA) {
            return {
                x: NaN,
                y: NaN
            };
        }

        v = {
            x: v.x / d,
            y: v.y / d
        };

        percent = Math.max(PAN_MIN_PERCENT_SPEED, percent);

        var vnorm = {
            x: -1 * v.x * (percent * PAN_DISTANCE),
            y: -1 * v.y * (percent * PAN_DISTANCE)
        };

        return vnorm;
    }

    function calculatePosition({x, y}) {
        const angle = Math.atan(y / x) + (x > 0 ? Math.PI : 0)
        const cX = 0.5 * Math.cos(angle);
        const cY = 0.5 * Math.sin(angle);
        const toPercent = v => (v + 0.5) * 100;
        const position = `${toPercent(cX)}% ${toPercent(cY)}%`
        return position
    }

});

define([
    'react',
    'react-virtualized-select'
], function(
    React,
    { default: VirtualizedSelect }) {

    const PropTypes = React.PropTypes;
    const GlyphSelector = React.createClass({
        propTypes: {
            onSelected: PropTypes.func.isRequired
        },
        getInitialState() {
            return {};
        },
        componentDidMount() {
            Promise.require('text!../imgc/sprites/glyphicons.json_array').then(json => {
                var obj = JSON.parse(json);
                this.setState({ options: obj.list });
            })
        },
        render() {
            const { value = null, options = [] } = this.state;
            return (
                <VirtualizedSelect
                    options={options}
                    simpleValue
                    clearable
                    searchable
                    value={value}
                    onChange={this.onChange}
                    optionRenderer={GlyphOptionRenderer}
                    optionHeight={28}
                    placeholder="Select Icon (optional)"
                    valueRenderer={GlyphValueRenderer}
                    //labelKey="label"
                    //valueKey="value"
                    ////matchProp="label"
                />
            )
        },
        onChange(value) {
            this.setState({ value })

            if (!value) {
                this.props.onSelected();
            } else {
                this.props.onSelected(value)
            }
        }
    });

    return GlyphSelector;

    function GlyphValueRenderer (option) {
        return (
            <div style={{ paddingLeft: '33px' }}
                title={option.label}>
            <div className="icon" style={{
                position: 'absolute',
                left: '7px',
                top: '4px',
                backgroundImage: 'url(imgc/sprites/glyphicons.png)',
                backgroundPosition: option.backgroundPosition,
                backgroundSize: option.backgroundSize,
                width: option.width,
                height: option.height,
                transform: `scale(${option.scale})`,
                transformOrigin: '0 0',
                margin: '0'
            }}></div>{option.label}</div>
        )
    }

    function GlyphOptionRenderer ({
        focusedOption, focusedOptionIndex, focusOption,
        key, labelKey,
        option, optionIndex, options,
        selectValue,
        style,
        valueArray
    }) {
        const className = ['VirtualizedSelectOption']
        if (option.className) {
            className.push(option.className);
        }
        if (option === focusedOption) {
            className.push('VirtualizedSelectFocusedOption')
        }
        if (option.disabled) {
            className.push('VirtualizedSelectDisabledOption')
        }
        if (option.header) {
            className.push('VirtualizedSelectHeader');
        }
        if (valueArray && valueArray.indexOf(option) >= 0) {
            className.push('VirtualizedSelectSelectedOption')
        }
        const events = option.disabled ? {} : {
            onClick: () => selectValue(option),
            onMouseOver: () => focusOption(option)
        };
        const { px } = option;

        return (
            <div className={className.join(' ')}
                key={key}
                style={{ ...style, paddingLeft: '33px' }}
                title={option[labelKey]}
                {...events}>
            <div className="icon" style={{
                position: 'absolute',
                left: '7px',
                backgroundImage: 'url(imgc/sprites/glyphicons.png)',
                backgroundPosition: option.backgroundPosition,
                backgroundSize: option.backgroundSize,
                width: option.width,
                height: option.height,
                transform: `scale(${option.scale})`,
                transformOrigin: '0 50%',
                margin: '0'
            }}></div>{option[labelKey]}</div>
        );
    }
});

define([
    'react',
    'react-virtualized-select'
], function(
    React,
    { default: VirtualizedSelect }) {

    const PropTypes = React.PropTypes;
    const GlyphSelector = React.createClass({
        propTypes: {
            search: PropTypes.string,
            onSelected: PropTypes.func.isRequired
        },
        getInitialState() {
            return { isLoading: true };
        },
        componentDidMount() {
            Promise.require('text!../imgc/sprites/glyphicons.json_array').then(json => {
                var obj = JSON.parse(json);
                this.setState({ isLoading: false, options: obj.list });
                this.checkForMatch();
            })
        },
        componentWillReceiveProps(nextProps) {
            this.checkForMatch(nextProps)
        },
        checkForMatch(props) {
            const { value, options, similar } = this.state;
            const { search } = props || this.props;

            if (!value && options && search && search.length > 2) {
                var option = _.find(options, option => option.label.toLowerCase().indexOf(search.toLowerCase()) >= 0);
                if (option) {
                    if (option.value !== similar) {
                        this.setState({ similar: option.value })
                        this.props.onSelected(option.value);
                    }
                } else if (similar) {
                    this.setState({ similar: null });
                    this.props.onSelected();
                }
            }
        },
        render() {
            const { value = null, options = [], isLoading, similar } = this.state;
            const { search } = this.props;

            return (
                <VirtualizedSelect
                    options={options}
                    simpleValue
                    clearable
                    searchable
                    value={_.isString(value) ? value : similar}
                    onChange={this.onChange}
                    optionRenderer={GlyphOptionRenderer}
                    optionHeight={28}
                    isLoading={isLoading}
                    placeholder="Select Icon (optional)"
                    valueRenderer={GlyphValueRenderer}
                />
            )
        },
        onChange(value) {
            this.setState({ value: value ? value : '' })

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

define([
    'react',
    'react-virtualized-select'
], function(
    React,
    { default: VirtualizedSelect }) {

    var _counter = 1;
    const PropTypes = React.PropTypes;
    const keyCounter = () => _counter++;

    const createFixedCreatable = Creatable => {
        class CreatablePutLast extends Creatable {
            constructor(props) {
                super(props);

                // Wrap filter options to move the create option to last
                // This will be a valid configuration in future releases
                // https://github.com/JedWatson/react-select/pull/1436
                this.filterOptions = _.wrap(this.filterOptions, (fn, ...rest) => {
                    const filtered = fn.apply(this, rest);
                    if (filtered.length > 1 && filtered[0] === this._createPlaceholderOption) {
                        filtered.push(filtered.shift());
                    }
                    return filtered;
                })
            }
        }
        return CreatablePutLast;
    };

    const BaseSelect = React.createClass({
        propTypes: {
            onSelected: PropTypes.func.isRequired,
            valueKey: PropTypes.string.isRequired,
            labelKey: PropTypes.string.isRequired,
            options: PropTypes.array.isRequired,
            createForm: PropTypes.string,
            value: PropTypes.string,
            creatable: PropTypes.bool
        },
        getInitialState() {
            return { creating: false }
        },
        getDefaultProps() {
            return { creatable: true, labelKey: 'displayName', valueKey: 'title' }
        },
        updateValue(value) {
            this.setState({ value })

            if (!value) {
                this.props.onSelected();
            } else {
                this.props.onSelected(this.getOptionByValue(value))
            }
        },
        getOptionByValue(value, props) {
            const { valueKey, options } = props || this.props;
            return _.findWhere(options, { [valueKey]: value });
        },
        componentDidMount() {
            if (this.props.creatable) {
                // Hack to get the internal Creatable from the Select dependency of
                // virtualized. Requiring 'react-select' in amd doesn't work
                const ref = this._virtualized;
                const Select = ref._getSelectComponent();
                if (Select) {
                    const { Creatable } = Select;
                    this.setState({ selectComponent: createFixedCreatable(Creatable) });
                } else {
                    throw new Error('Internal structure of select has changed');
                }
                if (this.props.createForm) {
                    Promise.require(this.props.createForm).then(CreateForm => {
                        this.setState({ CreateForm })
                    })
                } else throw new Error('Create form prop required when creatable')
            }
        },
        componentWillReceiveProps(nextProps) {
            const { key } = this.state;
            if (key && nextProps.iriKeys && nextProps.iriKeys[key]) {
                const value = nextProps.iriKeys[key];
                this.setState({ value, key: false })
                this.props.onSelected(this.getOptionByValue(value));
            }
        },
        render() {
            const { creating, value, CreateForm, selectComponent } = this.state;
            const { options, placeholder, value: defaultValue, valueKey, labelKey, creatable, createForm } = this.props;
            return (
                <div>
                {
                    (creating && CreateForm) ? (
                        <CreateForm
                            displayName={creating}
                            onCancel={this.onCancel}
                            onCreate={this.onCreate} />
                    ) : (
                        <VirtualizedSelect
                            ref={r => { this._virtualized = r}}
                            options={options}
                            autofocus
                            simpleValue
                            clearable
                            searchable
                            selectComponent={selectComponent}
                            promptTextCreator={label => `Create "${label}"`}
                            // Bug in Creatable? that the default optioncreator doesn't
                            // work when create option is not first because it's
                            // returning a new object. Memoize on label to fix
                            newOptionCreator={_.memoize(({ label, labelKey, valueKey }) => ({
                                [valueKey]: label,
                                [labelKey]: label,
                                className: 'Select-create-option-placeholder'
                            }), ({ label }) => label)}
                            value={_.isString(value) ? value : defaultValue}
                            onChange={this.updateValue}
                            onNewOptionClick={this.onNewOptionClick}
                            optionRenderer={NameOptionRenderer}
                            optionHeight={28}
                            placeholder={placeholder}
                            labelKey={labelKey}
                            valueKey={valueKey}
                            matchProp="label"
                        />
                    )
                }
                </div>
            );
        },
        onCancel() {
            this.setState({ creating: false })
        },
        onCreate(option) {
            const key = keyCounter();
            this.props.onCreate(option, { key });
            // TODO: should have a loading state
            this.setState({ creating: false, key })
        },
        onNewOptionClick(option) {
            this.setState({ creating: option[this.props.labelKey] })
        }
    });

    return BaseSelect;

    function NameOptionRenderer ({
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
        const indent = (option.depth || 0) * 15;

        if (option.header) {
            //TODO divider
            return (
                <div
                    className={className.join(' ')}
                    key={key}
                    style={style}
                >
                    {option[labelKey]}
                </div>
            );
        }

        return (
            <div className={className.join(' ')}
                key={key}
                style={{ ...style, paddingLeft: `${indent}px` }}
                title={option.path}
                {...events}>
            <div className="icon" style={{
                backgroundImage: option.glyphIconHref ?
                    `url(${option.glyphIconHref})` : null
            }}></div>{option[labelKey]}</div>
        );
    }
});

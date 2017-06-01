define([
    'react',
    'react-redux',
    'react-virtualized-select',
    'data/web-worker/store/ontology/selectors',
    'data/web-worker/store/ontology/actions'
], function(
    React,
    redux,
    { default: VirtualizedSelect },
    ontologySelectors,
    ontologyActions) {


    const PropTypes = React.PropTypes;
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
    const ConceptsSelector = React.createClass({
        propTypes: {
            onConceptSelected: PropTypes.func.isRequired
        },
        getInitialState() {
            return { }
        },
        updateValue(value) {
            this.setState({ value })
            if (!value) {
                this.props.onConceptSelected({ concept: null });
            } else {
                this.props.onConceptSelected(_.findWhere(this.props.concepts, { id: value }))
            }
        },
        componentDidMount() {
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
        },
        render() {
            // TODO: Check for ontology editor priv
            const { selectComponent, creating } = this.state;
            const { concepts } = this.props;
            return (<div>
                {creating ? (
                <div>
                    <input type="text" onChange={this.onDisplayNameChange} value={this.state.displayName} />
                    <label>Parent Concept</label>
                    <VirtualizedSelect
                        options={concepts}
                        simpleValue
                        clearable
                        searchable
                        value={this.state.parentConcept}
                        onChange={val => this.setState({ parentConcept: val }) }
                        optionRenderer={NameOptionRenderer}
                        optionHeight={28}
                        labelKey="displayName"
                        valueKey="id"
                        matchProp="label"
                    />
                    <label>Icon</label>
                    <select><option>Choose Icon...</option></select>
                    <div style={{textAlign: 'right'}}>
                    <button onClick={this.onCreate} className="btn btn-small btn-primary" style={{ width: 'auto', marginBottom: '1em'}}>Create {`"${creating.displayName}"`}</button>
                    </div>
                </div>
            ) : (
                <VirtualizedSelect
                    ref={r => { this._virtualized = r}}
					options={concepts}
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
					value={this.state.value}
					onChange={this.updateValue}
                    onNewOptionClick={this.onNewOptionClick}
                    optionRenderer={NameOptionRenderer}
                    optionHeight={28}
					labelKey="displayName"
					valueKey="id"
                    matchProp="label"
				/>
            )}</div>);
        },
        onNewOptionClick(option) {
            console.log(option)
            this.setState({ creating: option, displayName: option.displayName })
        },
        onDisplayNameChange(e) {
            console.log(arguments)
            this.setState({ displayName: e.value })
        },
        onCreate() {
            this.props.onCreateConcept({
                parentConcept: this.state.parentConcept,
                displayName: this.state.displayName
            })
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                concepts: ontologySelectors.getVisibleConceptsWithHeaders(state),
                ...props
            };
        },

        (dispatch, props) => ({
            onCreateConcept: (concept) => {
                dispatch(ontologyActions.addConcept(concept));
            }
        })
    )(ConceptsSelector);

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
        if (valueArray && valueArray.indexOf(option) >= 0) {
            className.push('VirtualizedSelectSelectedOption')
        }
        const events = option.disabled ? {} : {
            onClick: () => selectValue(option),
            onMouseOver: () => focusOption(option)
        };
        const indent = (option.depth || 0) * 15;

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

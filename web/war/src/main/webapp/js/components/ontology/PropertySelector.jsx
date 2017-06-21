define([
    'react',
    'react-redux',
    './BaseSelect',
    'data/web-worker/store/user/selectors',
    'data/web-worker/store/ontology/selectors',
    'data/web-worker/store/ontology/actions'
], function(
    React,
    redux,
    BaseSelect,
    userSelectors,
    ontologySelectors,
    ontologyActions) {

    const ontologyDisplayTypes = {
        bytes: {
            displayType: 'bytes',
            dataType: 'integer'
        },
        dateOnly: {
            displayType: 'dateOnly',
            dataType: 'dateTime'
        },
        duration: {
            displayType: 'duration',
            dataType: 'double'
        },
        link: {
            displayType: 'link',
            dataType: 'string'
        }
    }
    const FilterProps = ['dataType', 'deleteable', 'searchable', 'sortable', 'updateable', 'userVisible'];
    const FilterPropDefaults = {
        userVisible: true
    };

    const PropTypes = React.PropTypes;
    const PropertySelector = React.createClass({
        propTypes: {
            filter: PropTypes.shape({
                conceptId: PropTypes.string,
                relationshipId: PropTypes.string,
                addable: PropTypes.bool,
                searchable: PropTypes.bool
            })
        },
        getDefaultProps() {
            return { creatable: true }
        },
        render() {
            const {
                properties,
                propertiesByConcept,
                propertiesByRelationship,
                filter,
                privileges, creatable, ...rest } = this.props;
            const formProps = { ...(filter || {}) };
            const dependentPropertyIris = [];
            let options = properties.filter((p, i, list) => {
                if (p.header) {
                    return true;
                }

                let test = true;
                if (filter && filter.conceptId) {
                    const conceptProps = propertiesByConcept[filter.conceptId];
                    const conceptProp = conceptProps && conceptProps[p.title];
                    test = test && Boolean(conceptProp);
                }
                if (filter && filter.relationshipId) {
                    const relationshipProps = propertiesByRelationship[filter.relationshipId];
                    const relationshipProp = relationshipProps && relationshipProps[p.title];
                    test = test && Boolean(relationshipProp);
                }
                if (filter && filter.rollupCompound && p.dependentPropertyIris) {
                    dependentPropertyIris.push(...p.dependentPropertyIris);
                }
                FilterProps.forEach(fp => {
                    if (filter && fp in filter) {
                        // otherwise any value is valid
                        if (filter[fp] !== undefined && filter[fp] !== null) {
                            test = test && p[fp] === filter[fp];
                        }
                    } else if (fp in FilterPropDefaults) {
                        test = test && p[fp] === FilterPropDefaults[fp];
                    }
                })
                return test;
            });

            if (filter && filter.rollupCompound) {
                const uniqueIris = _.object(dependentPropertyIris.map(iri => [iri, true]))
                options = options.filter(o => !uniqueIris[o.title]);
            }

            removeEmptyHeaders(options)

            return (
                <BaseSelect
                    createForm={'components/ontology/PropertyForm'}
                    formProps={formProps}
                    options={options}
                    creatable={creatable && Boolean(privileges.ONTOLOGY_ADD)}
                    {...rest} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                privileges: userSelectors.getPrivileges(state),
                properties: ontologySelectors.getVisiblePropertiesWithHeaders(state),
                propertiesByConcept: ontologySelectors.getPropertiesByConcept(state),
                propertiesByRelationship: ontologySelectors.getPropertiesByRelationship(state),
                iriKeys: ontologySelectors.getPropertyKeyIris(state),
                ...props
            };
        },

        (dispatch, props) => ({
            onCreate: ({ displayName, type, domain }, options) => {
                let property = { displayName, domain };
                if (ontologyDisplayTypes[type]) {
                    property = {
                        ...property,
                        ...ontologyDisplayTypes[type]
                    };
                } else {
                    property = {
                        ...property,
                        dataType: type
                    };
                }
                dispatch(ontologyActions.addProperty(property, options));
            }
        })
    )(PropertySelector);

    function removeEmptyHeaders(options) {
        const removeHeaderIndices = [];
        let lastHeaderIndex = -1;
        options.forEach((o, i, list) => {
            if (o.header) {
                if (lastHeaderIndex === (i - 1)) {
                    removeHeaderIndices.push(lastHeaderIndex)
                }
                if (i === (list.length - 1)) {
                    removeHeaderIndices.push(i)
                }
                lastHeaderIndex = i;
            }
        })
        removeHeaderIndices.reverse().forEach(i => {
            options.splice(i, 1);
        })
    }
});

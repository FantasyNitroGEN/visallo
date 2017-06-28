define([
    'create-react-class',
    'prop-types',
    'react-redux',
    './BaseSelect',
    'data/web-worker/store/user/selectors',
    'data/web-worker/store/ontology/selectors',
    'data/web-worker/store/ontology/actions'
], function(
    createReactClass,
    PropTypes,
    redux,
    BaseSelect,
    userSelectors,
    ontologySelectors,
    ontologyActions) {

    const ConceptsSelector = createReactClass({
        propTypes: {
            filter: PropTypes.shape({
                conceptId: PropTypes.string.isRequired,
                showAncestors: PropTypes.bool
            }),
            privileges: PropTypes.object.isRequired,
            concepts: PropTypes.array.isRequired,
            conceptAncestors: PropTypes.object.isRequired,
            placeholder: PropTypes.string
        },
        getDefaultProps() {
            return { creatable: true, placeholder: i18n('concept.field.placeholder') }
        },
        render() {
            const {
                conceptAncestors,
                concepts,
                filter,
                privileges,
                creatable,
                ...rest
            } = this.props;

            var options = concepts;
            if (filter) {
                options = concepts.filter(o => {
                    return o.id === filter.conceptId ||
                        (!filter.showAncestors || conceptAncestors[filter.conceptId].includes(o.id))
                })
            }
            return (
                <BaseSelect
                    createForm={'components/ontology/ConceptForm'}
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
                concepts: ontologySelectors.getVisibleConcepts(state),
                conceptAncestors: ontologySelectors.getConceptAncestors(state),
                iriKeys: ontologySelectors.getConceptKeyIris(state),
                ...props
            };
        },

        (dispatch, props) => ({
            onCreate: (concept, options) => {
                dispatch(ontologyActions.addConcept(concept, options));
            }
        })
    )(ConceptsSelector);
});

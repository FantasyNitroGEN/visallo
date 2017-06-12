define([
    'react',
    'react-redux',
    './BaseSelect',
    'data/web-worker/store/ontology/selectors',
    'data/web-worker/store/ontology/actions'
], function(
    React,
    redux,
    BaseSelect,
    ontologySelectors,
    ontologyActions) {

    // TODO: Check for ontology editor priv
    const PropTypes = React.PropTypes;
    const ConceptsSelector = React.createClass({
        propTypes: {
            filter: PropTypes.shape({
                conceptId: PropTypes.string.isRequired,
                showAncestors: PropTypes.bool
            })
        },
        render() {
            const { concepts, filter, conceptAncestors } = this.props;
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
                    {...this.props} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
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

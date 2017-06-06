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
        },
        render() {
            return (
                <BaseSelect createForm={'components/ontology/ConceptForm'} {...this.props} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                options: ontologySelectors.getVisibleConcepts(state),
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

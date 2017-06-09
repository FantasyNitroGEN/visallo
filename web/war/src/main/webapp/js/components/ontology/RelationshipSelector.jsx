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
    const RelationshipSelector = React.createClass({
        propTypes: {
        },
        render() {
            return (
                <BaseSelect createForm={'components/ontology/RelationshipForm'} {...this.props} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                options: ontologySelectors.getVisibleRelationships(state),
                iriKeys: ontologySelectors.getRelationshipKeyIris(state),
                ...props
            };
        },

        (dispatch, props) => ({
            onCreate: (relationship, options) => {
                dispatch(ontologyActions.addRelationship(relationship, options));
            }
        })
    )(RelationshipSelector);
});


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
    const filterList = (conceptDescendents, relationships, relationshipKey, filter) => relationships.filter(r => {
        return _.any(r[relationshipKey], iri => {
            return (
                filter === iri ||
                (conceptDescendents[iri] && conceptDescendents[iri].includes(filter))
            );
        });
    });
    const PropTypes = React.PropTypes;
    const RelationshipSelector = React.createClass({
        propTypes: {
        },
        render() {
            const { conceptDescendents, relationships, sourceConcept, targetConcept, concept, ...rest } = this.props;
            const formProps = { sourceConcept, targetConcept };
            if (concept && (sourceConcept || targetConcept)) {
                throw new Error('only one of concept or source/target can be sent');
            }
            // TODO: handle concept prop
            var options = relationships;
            if (sourceConcept) {
                options = filterList(conceptDescendents, options, 'domainConceptIris', sourceConcept);
            }
            if (targetConcept) {
                options = filterList(conceptDescendents, options, 'rangeConceptIris', targetConcept);
            }
            return (
                <BaseSelect 
                    createForm={'components/ontology/RelationshipForm'}
                    formProps={formProps}
                    options={options}
                    {...rest} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                conceptDescendents: ontologySelectors.getConceptDescendents(state),
                relationships: ontologySelectors.getVisibleRelationships(state),
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


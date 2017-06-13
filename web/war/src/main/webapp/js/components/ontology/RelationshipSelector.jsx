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

    const filterList = (conceptDescendents, relationships, relationshipKeys, filter) => relationships.filter(r => {
        const domainRanges = _.flatten(relationshipKeys.map(k => r[k]));
        return _.any(domainRanges, iri => {
            return (
                filter === iri ||
                (conceptDescendents[iri] && conceptDescendents[iri].includes(filter))
            );
        });
    });
    const PropTypes = React.PropTypes;
    const RelationshipSelector = React.createClass({
        propTypes: {
            conceptDescendents: PropTypes.object.isRequired,
            relationships: PropTypes.array.isRequired,
            sourceConcept: PropTypes.string,
            targetConcept: PropTypes.string,
            concept: PropTypes.string
        },
        getDefaultProps() {
            return { creatable: true }
        },
        render() {
            const {
                concept,
                conceptDescendents,
                privileges,
                relationships,
                sourceConcept,
                targetConcept,
                creatable,
                ...rest
            } = this.props;
            const formProps = { sourceConcept, targetConcept };

            if (concept && (sourceConcept || targetConcept)) {
                throw new Error('only one of concept or source/target can be sent');
            }
            var options = relationships;
            if (concept) {
                options = filterList(conceptDescendents, options, ['domainConceptIris', 'rangeConceptIris'], concept);
            } else {
                if (sourceConcept) {
                    options = filterList(conceptDescendents, options, ['domainConceptIris'], sourceConcept);
                }
                if (targetConcept) {
                    options = filterList(conceptDescendents, options, ['rangeConceptIris'], targetConcept);
                }
            }

            return (
                <BaseSelect
                    createForm={'components/ontology/RelationshipForm'}
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


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

    const PropTypes = React.PropTypes;
    const PropertySelector = React.createClass({
        propTypes: {
        },
        getDefaultProps() {
            return { creatable: true }
        },
        render() {
            const { privileges, creatable, concept, ...rest } = this.props;
            const formProps = { concept };
            return (
                <BaseSelect
                    createForm={'components/ontology/PropertyForm'}
                    formProps={formProps}
                    creatable={creatable && Boolean(privileges.ONTOLOGY_ADD)}
                    {...rest} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                privileges: userSelectors.getPrivileges(state),
                options: ontologySelectors.getVisiblePropertiesWithHeaders(state),
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
});

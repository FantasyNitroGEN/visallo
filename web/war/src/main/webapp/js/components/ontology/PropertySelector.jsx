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
        render() {
            return (
                <BaseSelect createForm={'components/ontology/PropertyForm'} {...this.props} />
            );
        }
    });

    return redux.connect(
        (state, props) => {
            return {
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

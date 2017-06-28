define([
    'create-react-class',
    'react-redux',
    './worker/actions',
    './S3Config'
], function(createReactClass, redux, s3Actions, S3Config) {
    'use strict';

    const triggerPopoverLayout = _.debounce((el) => {
        $(el).trigger('positionDialog');
    }, 250)
    const PopoverHelper = createReactClass({
        componentDidUpdate() {
            triggerPopoverLayout(this.refs.div);
        },
        componentDidMount() {
            triggerPopoverLayout(this.refs.div);
        },
        render() {
            return <div ref="div"><S3Config {...this.props} /></div>
        }
    })
    const AmazonS3ConfigurationContainer = redux.connect(

        (state, props) => {
            const authenticationId = state.configuration.properties['org.visallo.ingest.cloud.s3.authentication'];
            return { ...props, ...state['ingest-cloud-s3'], authenticationId }
        },

        (dispatch, props) => ({
            onConnect(providerClass, credentials) { dispatch(s3Actions.connect(providerClass, credentials)) },
            onOpenDirectory(name) { dispatch(s3Actions.openDirectory(name))},
            onRefreshDirectories() { dispatch(s3Actions.refreshDirectories())},
            onSelectItem(name) { dispatch(s3Actions.selectItem(name))}
        })

    )(PopoverHelper);

    return AmazonS3ConfigurationContainer;
});

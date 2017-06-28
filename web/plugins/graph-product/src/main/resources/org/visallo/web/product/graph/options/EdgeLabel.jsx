define([
    'create-react-class'
], function(createReactClass) {
    'use strict';

    const preferenceName = 'edgeLabels';
    const EdgeLabel = createReactClass({
        onChange(event) {
            const checked = event.target.checked;
            visalloData.currentUser.uiPreferences[preferenceName] = '' + checked;
            $(event.target).trigger('reapplyGraphStylesheet');
            this.props.visalloApi.v1.dataRequest('user', 'preference', preferenceName, checked);
        },

        render() {
            const preferenceValue = visalloData.currentUser.uiPreferences[preferenceName];
            const showEdges = preferenceValue !== 'false';

            return (
                <label>{i18n('controls.options.edgeLabels.toggle')}
                    <input onChange={this.onChange} type="checkbox" defaultChecked={showEdges} />
                </label>
            )
        }
    });

    return EdgeLabel;
});

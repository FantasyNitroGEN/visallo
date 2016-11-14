define([
    'react'
], function(React) {
    'use strict';

    const preferenceName = 'edgeLabels';
    const EdgeLabel = React.createClass({
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

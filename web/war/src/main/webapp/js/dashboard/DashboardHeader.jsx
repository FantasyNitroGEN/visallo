define([
    'react'
], function(React) {
    'use strict';

    const PropTypes = React.PropTypes;
    const DashboardHeader = React.createClass({
        propTypes: {
            onToggleEdit: PropTypes.func.isRequired,
            editing: PropTypes.bool.isRequired
        },
        render: function() {
            var buttonKey = this.props.editing ?
                    'dashboard.title.editing.done' :
                    'dashboard.title.edit',
                keyboardShortcut = this.props.editing ?
                    <span className="keyboard">{i18n('dashboard.title.editing.done.key')}</span> :
                    '';
            return (
                <h1 className="header">
                    {this.props.title}
                    <button onClick={this.props.onToggleEdit} className="edit-dashboard btn btn-link">{i18n(buttonKey)}{keyboardShortcut}</button>
                </h1>
            )
            //<button title="(i18n 'dashboard.title.refresh' }}" class="refresh">{{ i18n 'dashboard.title.refresh' }}</button></h1>
        }
    });

    return DashboardHeader;
});

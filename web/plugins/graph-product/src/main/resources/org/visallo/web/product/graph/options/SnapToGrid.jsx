define([
    'react',
    'react-redux',
    '../worker/actions'
], function(React, redux, graphActions) {
    'use strict';

    const SnapToGrid = React.createClass({
        onChange(event) {
            const checked = event.target.checked;
            this.props.onSnapToGrid(checked);
        },

        render() {
            const { snapToGrid } = this.props;

            return (
                <label>{i18n('controls.options.snapToGrid.toggle')}
                    <input onChange={this.onChange} type="checkbox" defaultChecked={snapToGrid} />
                </label>
            )
        }
    });

    return redux.connect(
        (state, props) => {
            const snapToGrid = state.user.current.uiPreferences.snapToGrid === 'true'
            return { snapToGrid }
        },

        (dispatch, props) => ({
            onSnapToGrid: (snap) => dispatch(graphActions.snapToGrid(snap))
        })
    )(SnapToGrid);
});


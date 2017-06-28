define([
    'create-react-class',
    'prop-types',
    'react-redux',
    '../worker/actions'
], function(createReactClass, PropTypes, redux, graphActions) {
    'use strict';

    const SnapToGrid = createReactClass({
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


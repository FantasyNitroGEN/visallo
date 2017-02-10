define([
    'react-redux',
    'data/web-worker/store/user/actions',
    './FindPathPopover'
], function(redux, userActions, FindPathPopover) {
    'use strict';

    const PREFERENCE_NAME = 'org.visallo.findPath';

    return redux.connect(
        (state, props) => {
            const uiPreferences = state.user.current.uiPreferences || {};
            let findPathPrefs = uiPreferences[PREFERENCE_NAME] || {};
            return {
                ...props,
                userPreferences: findPathPrefs,
                configuration: state.configuration.properties
            };
        },
        (dispatch, props) => {
            return {
                setUserPreferences: (value) => dispatch(
                    userActions.setUserPreference({
                      name: PREFERENCE_NAME,
                      value: JSON.stringify(value)
                    })
                )
            }
        }
    )(FindPathPopover);
});

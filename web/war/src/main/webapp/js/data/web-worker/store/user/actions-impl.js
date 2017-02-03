define(['../actions', 'data/web-worker/util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const api = {
        putUser: ({ user }) => ({
            type: 'USER_PUT',
            payload: { user }
        }),

        putUserPreferences: ({ preferences }) => ({
            type: 'USER_PUT_PREFS',
            payload: { preferences }
        }),

        putUserPreference: ({ name, value }) => ({
            type: 'USER_PUT_PREF',
            payload: { name, value }
        }),

        setUserPreference: ({ name, value }) => (dispatch, getState) => {
            dispatch(api.putUserPreference({ name, value }));
            return ajax('POST', '/user/ui-preferences', { name, value });
        }

    }

    return api;
})
